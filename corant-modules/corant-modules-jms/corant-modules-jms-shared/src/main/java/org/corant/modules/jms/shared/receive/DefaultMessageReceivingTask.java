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
package org.corant.modules.jms.shared.receive;

import static org.corant.context.Beans.resolve;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.max;
import static org.corant.shared.util.Threads.tryThreadSleep;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import org.corant.context.CDIs;
import org.corant.modules.jms.JMSNames;
import org.corant.modules.jms.marshaller.MessageMarshaller;
import org.corant.modules.jms.receive.ManagedMessageReceiveReplier;
import org.corant.modules.jms.receive.ManagedMessageReceiver;
import org.corant.modules.jms.receive.ManagedMessageReceivingHandler;
import org.corant.modules.jms.receive.ManagedMessageReceivingTask;
import org.corant.shared.retry.BackoffStrategy;

/**
 * corant-modules-jms-shared
 *
 * <p>
 * Default message receiving task, supports retry and circuit break, and supports various retry
 * interval compensation algorithms.
 *
 * Unfinish: use connection or session pool, commit ordering
 *
 * @see <a href = "https://developer.jboss.org/wiki/ShouldICacheJMSConnectionsAndJMSSessions">
 *      Should I cache JMS connections and JMS sessions</a>
 *
 * @see <a href = "https://www.atomikos.com/Documentation/CommitOrderingWithJms">Atomikos Commit
 *      Ordering</a>
 *
 * @see <a href="https://developer.jboss.org/thread/274469">Narayana Commit Order</a>
 *
 * @author bingo 上午11:33:15
 *
 */
public class DefaultMessageReceivingTask
    implements ManagedMessageReceivingTask, MessageReceivingMediator {

  public static final byte STATE_RUN = 0;
  public static final byte STATE_TRY = 1;
  public static final byte STATE_BRK = 2;

  protected static final Logger logger =
      Logger.getLogger(DefaultMessageReceivingTask.class.getName());

  // configuration
  protected final MessageReceivingMetaData meta;
  protected final long loopIntervalMillis;

  // executor controller
  protected final AtomicBoolean cancellation = new AtomicBoolean();

  // control to reconnect JMS server
  protected final int jmsFailureThreshold;
  protected final AtomicInteger jmsFailureCounter = new AtomicInteger(0);

  // control circuit break
  protected final int failureThreshold;
  protected final BackoffStrategy backoffStrategy;
  protected final int tryThreshold;

  protected volatile byte state = STATE_RUN;
  protected volatile long brokenTimePoint;
  protected volatile long brokenMillis;
  protected volatile boolean inProgress;
  protected volatile boolean lastExecutionSuccessfully = false;

  protected final AtomicInteger failureCounter = new AtomicInteger(0);
  protected final AtomicInteger tryCounter = new AtomicInteger(0);
  protected final AtomicInteger tryFailureCounter = new AtomicInteger(0);

  // the workhorse
  protected final ManagedMessageReceiver messageReceiver;
  protected final ManagedMessageReceivingHandler messageHandler;
  protected final ManagedMessageReceiveReplier messageReplier;

  public DefaultMessageReceivingTask(MessageReceivingMetaData metaData) {
    this(metaData, metaData.getBrokenBackoffStrategy());
  }

  public DefaultMessageReceivingTask(MessageReceivingMetaData metaData,
      BackoffStrategy backoffStrategy) {
    meta = metaData;
    loopIntervalMillis = metaData.getLoopIntervalMs();
    failureThreshold = metaData.getFailureThreshold();
    jmsFailureThreshold = max(failureThreshold / 2, 2);
    this.backoffStrategy = backoffStrategy;
    tryThreshold = metaData.getTryThreshold();
    messageReplier = new DefaultMessageReplier(meta, this);
    messageHandler = new DefaultMessageHandler(meta, this);
    messageReceiver = new DefaultMessageReceiver(metaData, messageHandler, this);
    logger.log(Level.FINE, () -> String.format("Create message receive task for %s.", metaData));
  }

  @Override
  public synchronized boolean cancel() {
    int tryTimes = 0;
    long waitMs = max(meta.getReceiveTimeout(), 500L);
    while (true) {
      final int t = tryTimes++;
      if (!isInProgress() || t > 128) {
        logger.log(Level.INFO,
            () -> String.format("Cancel message receiving task try times %s, %s.", t, meta));
        return cancellation.compareAndSet(false, true);
      } else {
        logger.log(Level.INFO,
            () -> String.format("Waiting for message receiving task finished, %s.", meta));
        tryThreadSleep(waitMs);
      }
    }
  }

  @Override
  public boolean checkCancelled() {
    if (cancellation.get()) {
      resetMonitors();
      messageReceiver.release(true);
      logger.log(Level.INFO,
          () -> String.format("The message receiving task was cancelled, %s.", meta));
      return true;
    }
    return false;
  }

  @Override
  public MessageMarshaller getMessageMarshaller(String schema) {
    return resolve(MessageMarshaller.class,
        NamedLiteral.of(defaultObject(schema, JMSNames.MSG_MARSHAL_SCHEMA_STD_JAVA)));
  }

  public boolean isInProgress() {
    return inProgress;
  }

  @Override
  public void onPostMessageHandled(Message message, Session session, Object result)
      throws JMSException {
    messageReplier.reply(session, message, result);
  }

  @Override
  public void onReceivingException(Exception e) {
    if (e instanceof JMSException) {
      jmsFailureCounter.incrementAndGet();
    }
    if (lastExecutionSuccessfully) {
      failureCounter.set(1);
    } else {
      failureCounter.incrementAndGet();
    }
  }

  @Override
  public synchronized void run() {
    if (checkCancelled()) {
      return;
    }
    if (preRun()) {
      inProgress = true;
      lastExecutionSuccessfully = messageReceiver.receive();
      inProgress = false;
      postRun();
    } else if (!cancellation.get()) {
      tryThreadSleep(loopIntervalMillis);
    }
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
          backoffStrategy.reset();
          if (tryCounter.incrementAndGet() >= tryThreshold) {
            stateRun();
          }
        }
      }
      messageReceiver.release(jmsFailureCounter.compareAndSet(jmsFailureThreshold, 0));// FIXME
    } catch (Exception e) {
      logger.log(Level.SEVERE, e,
          () -> String.format("The execution status occurred error, %s.", meta));
    }
  }

  protected boolean preRun() {
    if (!CDIs.isEnabled()) {
      logger.log(Level.SEVERE,
          () -> String.format("The execution can't run because the CDI not enabled, %s.", meta));
      return false;
    }
    if (state == STATE_BRK) {
      long countdownMs = brokenMillis - (System.currentTimeMillis() - brokenTimePoint);
      if (countdownMs > 0) {
        if (countdownMs < loopIntervalMillis * 3) {
          logger.log(Level.INFO, () -> String
              .format("The execution was broken countdown %s ms, [%s]!", countdownMs, meta));
        }
        return false;
      } else {
        stateTry();
        return true;
      }
    }
    brokenMillis = 0L;
    return true;
  }

  protected void resetMonitors() {
    lastExecutionSuccessfully = true;
    jmsFailureCounter.set(0);
    failureCounter.set(0);
    brokenTimePoint = 0;
    tryCounter.set(0);
  }

  protected void stateBrk() {
    // TODO To be improved, when entering BROKEN mode, should exit from the executor.
    resetMonitors();
    brokenTimePoint = System.currentTimeMillis();
    brokenMillis = backoffStrategy.computeBackoffMillis(tryFailureCounter.get());
    state = STATE_BRK;
    logger.log(Level.WARNING, () -> String
        .format("The execution enters breaking mode wait for [%s] ms, [%s]!", brokenMillis, meta));
    messageReceiver.release(true);
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
