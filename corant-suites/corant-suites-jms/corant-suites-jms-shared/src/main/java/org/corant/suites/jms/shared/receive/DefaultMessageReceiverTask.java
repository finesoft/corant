/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.suites.jms.shared.receive;

import static org.corant.shared.util.Objects.max;
import static org.corant.shared.util.Threads.tryThreadSleep;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import javax.jms.JMSException;
import org.corant.context.CDIs;
import org.corant.shared.util.Retry.RetryInterval;

/**
 * corant-suites-jms-shared
 *
 * <p>
 * Default message receiving task, supports retry and circuit break, and supports various retry
 * interval compensation algorithms.
 *
 * Unfinish: use connection or session pool
 *
 * <p>
 * {@link <a href = "https://developer.jboss.org/wiki/ShouldICacheJMSConnectionsAndJMSSessions">
 * Should I cache JMS connections and JMS sessions</a>}
 *
 * @author bingo 上午11:33:15
 *
 */
public class DefaultMessageReceiverTask extends AbstractMessageReceiverTask {

  public static final byte STATE_RUN = 0;
  public static final byte STATE_TRY = 1;
  public static final byte STATE_BRK = 2;

  // controll to reconnect jms server
  protected final int jmsFailureThreshold;
  protected final AtomicInteger jmsFailureCounter = new AtomicInteger(0);

  // controll circuit break
  protected final int failureThreshold;
  protected final RetryInterval breakedInterval;
  protected final int tryThreshold;

  protected volatile byte state = STATE_RUN;
  protected volatile long breakedTimePoint;
  protected volatile long breakedMillis;
  protected volatile boolean inProgress;

  protected final AtomicInteger failureCounter = new AtomicInteger(0);
  protected final AtomicInteger tryCounter = new AtomicInteger(0);
  protected final AtomicInteger tryFailureCounter = new AtomicInteger(0);

  public DefaultMessageReceiverTask(MessageReceiverMetaData metaData) {
    this(metaData, metaData.getBreakedInterval());
  }

  public DefaultMessageReceiverTask(MessageReceiverMetaData metaData,
      RetryInterval breakedInterval) {
    super(metaData);
    failureThreshold = metaData.getFailureThreshold();
    jmsFailureThreshold = max(failureThreshold / 2, 2);
    this.breakedInterval = breakedInterval;
    tryThreshold = metaData.getTryThreshold();
    logger.log(Level.FINE, () -> String.format("Create message receive task for %s", metaData));
  }

  public boolean checkCancelled() {
    if (cancellation.get()) {
      resetMonitors();
      release(true);
      logger.log(Level.INFO, () -> String.format("Cancelled message receiving task, %s", meta));
      return true;
    }
    return false;
  }

  public boolean isInProgress() {
    return inProgress;
  }

  @Override
  public void run() {
    if (checkCancelled()) {
      return;
    }
    if (preRun()) {
      execute();
      postRun();
    } else {
      tryThreadSleep(loopIntervalMillis);
      return;
    }
  }

  @Override
  protected boolean initialize() throws JMSException {
    inProgress = true;
    return super.initialize();
  }

  @Override
  protected void onException(Exception e) {
    if (e instanceof JMSException) {
      jmsFailureCounter.incrementAndGet();
    }
    if (lastExecutionSuccessfully) {
      failureCounter.set(1);
    } else {
      failureCounter.incrementAndGet();
    }
    super.onException(e);
  }

  protected void postRun() {
    try {
      if (state == STATE_RUN) {
        if (failureCounter.intValue() >= failureThreshold) {
          stateBrk();
          return;
        }
      } else if (state == STATE_TRY) {
        if (failureCounter.intValue() > 0) {
          tryFailureCounter.incrementAndGet();
          stateBrk();
          return;
        } else {
          tryFailureCounter.set(0);
          breakedInterval.reset();
          if (tryCounter.incrementAndGet() >= tryThreshold) {
            stateRun();
          }
        }
      }
      release(jmsFailureCounter.compareAndSet(jmsFailureThreshold, 0));// FIXME
    } catch (Exception e) {
      logger.log(Level.SEVERE, e,
          () -> String.format("The execution status occurred error, %s", meta));
    }
  }

  protected boolean preRun() {
    if (!CDIs.isEnabled()) {
      logger.log(Level.SEVERE,
          () -> String.format("The executeion can't run because the CDI not enabled, %s", meta));
      return false;
    }
    if (state == STATE_BRK) {
      long countdownMs = breakedMillis - (System.currentTimeMillis() - breakedTimePoint);
      if (countdownMs > 0) {
        if (countdownMs < loopIntervalMillis * 3) {
          logger.log(Level.INFO, () -> String
              .format("The execution was breaked countdown %s ms, [%s]!", countdownMs, meta));
        }
        return false;
      } else {
        stateTry();
        return true;
      }
    }
    breakedMillis = 0L;
    return true;
  }

  @Override
  protected void release(boolean stop) {
    try {
      super.release(stop);
    } finally {
      inProgress = false;
    }
  }

  protected void resetMonitors() {
    lastExecutionSuccessfully = true;
    jmsFailureCounter.set(0);
    failureCounter.set(0);
    breakedTimePoint = 0;
    tryCounter.set(0);
  }

  protected void stateBrk() {
    resetMonitors();
    breakedTimePoint = System.currentTimeMillis();
    breakedMillis = breakedInterval.calculateMillis(tryFailureCounter.get());
    state = STATE_BRK;
    logger.log(Level.WARNING, () -> String
        .format("The execution enters breaking mode wait for [%s] ms, [%s]!", breakedMillis, meta));
    release(true);
  }

  protected void stateRun() {
    resetMonitors();
    state = STATE_RUN;
    logger.log(Level.INFO, () -> String.format("The execution enters running mode, [%s]!", meta));
  }

  protected void stateTry() {
    resetMonitors();
    state = STATE_TRY;
    logger.log(Level.INFO, () -> String.format("The execution enters trying mode, [%s]!", meta));
  }

}
