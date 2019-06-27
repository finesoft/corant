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

import static org.corant.Corant.instance;
import static org.corant.kernel.util.Instances.select;
import static org.corant.shared.util.ObjectUtils.max;
import static org.corant.shared.util.ObjectUtils.tryThreadSleep;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import javax.jms.XASession;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import org.corant.Corant;
import org.corant.config.ComparableConfigurator;
import org.corant.kernel.service.TransactionService;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-suites-jms-shared
 *
 * @author bingo 上午11:33:15
 *
 */
public class MessageReceiverTask implements Runnable {

  public static final byte STATE_RUN = 0;
  public static final byte STATE_TRY = 1;
  public static final byte STATE_BRK = 2;

  static final Logger logger = Logger.getLogger(MessageReceiverTask.class.getName());

  // config
  protected final MessageReceiverMetaData metaData;
  protected final ConnectionFactory connectionFactory;
  protected final MessageListener messageListener;
  protected final boolean xa;
  protected final int receiveThreshold;
  protected final long receiveTimeout;

  // worker object
  protected volatile Connection connection;
  protected volatile Session session;
  protected volatile MessageConsumer messageConsumer;
  protected volatile boolean inProgress;

  // controll to reconnect jms server
  protected final int jmsFailureThreshold;
  protected final AtomicInteger jmsFailureCounter = new AtomicInteger(0);

  // controll circuit break
  protected final int failureThreshold;
  protected final int tryThreshold;
  protected final Duration failureDuration;
  protected final Duration breakedDuration;

  protected volatile byte state = STATE_RUN;
  protected volatile boolean lastExecutionSuccessfully = true;
  protected volatile long breakedTimePoint;
  protected volatile long firstFailureTimePoint;

  protected final AtomicInteger failureCounter = new AtomicInteger(0);
  protected final AtomicInteger continuousFailureCounter = new AtomicInteger(0);
  protected final AtomicInteger tryCounter = new AtomicInteger(0);

  /**
   * @param metaData
   */
  protected MessageReceiverTask(MessageReceiverMetaData metaData) {
    super();
    this.metaData = metaData;
    xa = metaData.xa();
    connectionFactory = metaData.connectionFactory();
    messageListener = new MessageHandler(metaData.getMethod());
    failureThreshold = metaData.getFailureThreshold();
    jmsFailureThreshold = max(failureThreshold / 2, 2);
    failureDuration = metaData.getFailureDuration();
    breakedDuration = metaData.getBreakedDuration();
    tryThreshold = metaData.getTryThreshold();
    receiveThreshold = metaData.getReceiveThreshold();
    receiveTimeout = metaData.getReceiveTimeout();
    logFin("Create message receive task for %s", metaData);
  }

  public boolean isInProgress() {
    return inProgress;
  }

  @Override
  public void run() {
    Exception ex = null;
    if (!preRun()) {
      tryThreadSleep(max(500L, receiveTimeout));
      return;
    }
    try {
      logFin("Start message receive task.");
      if (initialize()) {
        int rt = receiveThreshold;
        while (--rt >= 0) {
          logFin("Begin message consuming.");
          preConsume();
          Message message = consume();
          postConsume(message);
          if (message == null) {
            break;
          }
          logFin("End message consuming.");
        }
      }
    } catch (Exception e) {
      onException(ex = e);
    } finally {
      lastExecutionSuccessfully = ex == null;
      postRun();
      logFin("Stopped message receive task.\n\n");
    }
  }

  protected Message consume() throws JMSException {
    try {
      Message message = null;
      if (metaData.getReceiveTimeout() <= 0) {
        message = messageConsumer.receiveNoWait();
      } else {
        message = messageConsumer.receive(metaData.getReceiveTimeout());
      }
      logFin("5. Received message from queue, [%s]", metaData);
      if (message != null) {
        messageListener.onMessage(message);
        logFin("6. Invoked message handler and processed message, [%s]", metaData);
      }
      return message;
    } catch (JMSException e) {
      logServ("6-x. Receive and process message occurred error, [%s]", metaData);
      throw e;
    }
  }

  protected boolean initialize() throws JMSException {
    inProgress = true;
    // initialize connection
    if (connection == null) {
      try {
        if (xa) {
          connection = ((XAConnectionFactory) connectionFactory).createXAConnection();
          logFin("1. Created message receive task xaconnection, [%s]", metaData);
        } else {
          connection = connectionFactory.createConnection();
          logFin("1. Created message receive task connection, [%s]", metaData);
        }
        select(MessageReceiverTaskConfigurator.class).stream()
            .sorted(ComparableConfigurator::compare)
            .forEach(c -> c.configConnection(connection, metaData));
        metaData.exceptionListener().ifPresent(listener -> listener.tryConfig(connection));
      } catch (JMSException je) {
        logServ("1-x. Initialize message receive task connection occurred error, [%s]", metaData);
        throw je;
      }
    }
    // initialize session
    if (session == null) {
      try {
        if (xa) {
          session = ((XAConnection) connection).createXASession();
          logFin("2. Created message receive task xasession, [%s]", metaData);
        } else {
          session = connection.createSession(metaData.getAcknowledge());
          logFin("2. Created message receive task session, [%s]", metaData);
        }
        instance().select(MessageReceiverTaskConfigurator.class).stream()
            .sorted(ComparableConfigurator::compare)
            .forEach(c -> c.configSession(session, metaData));
      } catch (JMSException je) {
        if (connection != null) {
          try {
            connection.close();
            connection = null;
            logFin(
                "2-1. Close message receive task connection when initialize session occurred error, [%s]",
                metaData);
          } catch (JMSException e) {
            logServ(
                "2-x. Close message receive task connection occurred error when initialize session occurred error, [%s]",
                metaData);
            throw je;
          }
        }
        logServ("2-x. Initialize message receive task session occurred error, [%s]", metaData);
        throw je;
      }
    }
    // initialize message consumer
    if (messageConsumer == null) {
      try {
        Destination destination =
            metaData.isMulticast() ? session.createTopic(metaData.getDestination())
                : session.createQueue(metaData.getDestination());
        if (isNotBlank(metaData.getSelector())) {
          messageConsumer = session.createConsumer(destination, metaData.getSelector());
        } else {
          messageConsumer = session.createConsumer(destination);
        }
        instance().select(MessageReceiverTaskConfigurator.class).stream()
            .sorted(ComparableConfigurator::compare)
            .forEach(c -> c.configMessageConsumer(messageConsumer, metaData));
        logFin("3. Created message receive task consumer, [%s]", metaData);
      } catch (JMSException je) {
        try {
          if (session != null) {
            session.close();
            session = null;
            logFin(
                "3-1. Close message receive task sesion when initialize consumer occurred error, [%s]",
                metaData);
          }
          if (connection != null) {
            connection.close();
            connection = null;
            logFin(
                "3-2. Close message receive task connection when initialize consumer occurred error, [%s]",
                metaData);
          }
        } catch (JMSException e) {
          logServ(
              "3-x. Close message receive task session and connection occurred error when initialize consumer occurred error, [%s]",
              metaData);
          throw je;
        }
        logServ("3-x.Initialize message receive task consumer occurred error, [%s]", metaData);
        throw je;
      }
    }
    // start connection
    connection.start();
    logFin("4. Message receive task connection started, [%s]", metaData);
    return true;
  }

  protected void onException(Exception e) {
    if (e instanceof JMSException) {
      jmsFailureCounter.incrementAndGet();
    }
    if (failureCounter.get() == 0) {
      firstFailureTimePoint = System.currentTimeMillis();
    }
    if (lastExecutionSuccessfully) {
      continuousFailureCounter.set(1);
    } else {
      continuousFailureCounter.incrementAndGet();
    }
    failureCounter.incrementAndGet();

    logEx(e, "Message receiver task occurred error, %s", metaData);
    try {
      if (xa) {
        if (TransactionService.currentTransaction() != null) {
          TransactionService.transactionManager().rollback();
          logServ("8-x. Rollback message receive task JTA transaction when occurred error, [%s]",
              metaData);
        }
      } else if (session != null) {
        if (metaData.getAcknowledge() == Session.SESSION_TRANSACTED) {
          session.rollback();
          logServ("8-x. Rollback message receive task session when occurred error, [%s]", metaData);
        } else if (metaData.getAcknowledge() == Session.CLIENT_ACKNOWLEDGE) {
          session.recover();
          logServ("8-x. Recover message receive task session when occurred error, [%s]", metaData);
        }
      }
    } catch (Exception te) {
      logEx(te, "Rollback message receive occurred error, %s", metaData);
      // throw new CorantRuntimeException(te);
    }
    // throw new CorantRuntimeException(e);
  }

  protected void postConsume(Message message) throws JMSException {
    try {
      if (xa) {
        if (TransactionService.currentTransaction() != null) {
          TransactionService.transactionManager().commit();
        }
        logFin("7-1. Commit message receive task JTA transaction, [%s]", metaData);
      } else if (metaData.getAcknowledge() == Session.SESSION_TRANSACTED && session != null) {
        session.commit();
        logFin("7-2. Commit message receive task session, [%s]", metaData);
      } else if (metaData.getAcknowledge() == Session.CLIENT_ACKNOWLEDGE && message != null) {
        message.acknowledge();
        logFin("7-3. Acknowledge message message receive task session, [%s]", metaData);
      }
    } catch (RollbackException | HeuristicMixedException | HeuristicRollbackException
        | SecurityException | IllegalStateException | SystemException te) {
      logServ("7-x. Commit message receive task JTA transaction occurred error, [%s]", metaData);
      throw generateJMSException(te);
    } catch (JMSException je) {
      logServ(
          "7-x. Commit/Acknowledge message receive task session or message occurred error, [%s]",
          metaData);
      throw je;
    }
  }

  protected void preConsume() throws JMSException {
    try {
      if (xa) {
        TransactionService.transactionManager().begin();
        TransactionService
            .enlistXAResourceToCurrentTransaction(((XASession) session).getXAResource());
        logFin("4-1. Message receive task JTA transaction began, [%s]", metaData);
      }
    } catch (Exception te) {
      logServ(
          "4-x. Enlist message receive task session xa resource to JTA environment occurred error, [%s]",
          metaData);
      throw generateJMSException(te);
    }
  }

  protected void release(boolean stop) {
    try {
      closeMessageConsumerIfNecessary(stop);
      closeSessionIfNecessary(stop);
      closeConnectionIfNecessary(stop);
    } finally {
      inProgress = false;
    }
  }

  void postRun() {
    try {
      if (state == STATE_RUN) {
        if (continuousFailureCounter.intValue() >= failureThreshold
            || failureCounter.intValue() >= failureThreshold
                && System.currentTimeMillis() - breakedTimePoint >= failureDuration.toMillis()) {
          stateBrk();
          return;
        }
      } else if (state == STATE_TRY && tryCounter.incrementAndGet() > tryThreshold) {
        if (failureCounter.intValue() > 0) {
          stateBrk();
          return;
        } else {
          stateRun();
        }
      }
      release(jmsFailureCounter.compareAndSet(jmsFailureThreshold, 0));
    } catch (Exception e) {
      logger.log(Level.SEVERE, e,
          () -> String.format("On post run message receive task occurred error, %s", metaData));
    }
  }

  boolean preRun() {
    if (state == STATE_BRK) {
      long countdownMs =
          breakedDuration.toMillis() - (System.currentTimeMillis() - breakedTimePoint);
      if (countdownMs >= 0) {
        logFin("The message receive task is breaking countdown %s ms, [%s]!", countdownMs,
            metaData);
        return false;
      } else {
        stateTry();
        return true;
      }
    }
    return true;
  }

  private void closeConnectionIfNecessary(boolean forceClose) {
    if ((metaData.getCacheLevel() <= 0 || forceClose) && connection != null) {
      try {
        connection.stop();
        connection.close();
      } catch (JMSException e) {
        logServ("9-4x. Stop and close message receive task occurred error, [%s]", metaData);
      } finally {
        session = null;
        messageConsumer = null;
        connection = null;
        logFin("9-4. Stop and close message receive task connection, [%s]", metaData);
      }
    }
  }

  private void closeMessageConsumerIfNecessary(boolean forceClose) {
    if ((metaData.getCacheLevel() <= 2 || forceClose) && messageConsumer != null) {
      try {
        messageConsumer.close();
      } catch (JMSException e) {
        logEx(e, "9-1x. Close message receive task consumer occurred error, [%s]", metaData);
      } finally {
        messageConsumer = null;
        logFin("9-1. Close message receive task consumer, [%s]", metaData);
      }
    }
  }

  private void closeSessionIfNecessary(boolean forceClose) {
    if ((metaData.getCacheLevel() <= 1 || forceClose) && session != null) {
      try {
        session.close();
        logFin("9-2. Close message receive task session, [%s]", metaData);
        if (connection != null) {
          connection.stop();
          logFin("9-3. Stop message receive task connection, [%s]", metaData);
        }
      } catch (JMSException e) {
        logFin("9-3x. Stop message receive task connection occurred error, [%s]", metaData);
      } finally {
        messageConsumer = null;
        session = null;
      }
    }
  }

  private JMSException generateJMSException(Exception t) {
    if (t instanceof JMSException) {
      return (JMSException) t;
    } else {
      JMSException jmsException = new JMSException(t.getMessage());
      jmsException.setLinkedException(t);
      return jmsException;
    }
  }

  private void logEx(Throwable t, String msgOrFmt, Object... params) {
    if (params.length > 0) {
      logger.log(Level.SEVERE, String.format(msgOrFmt, params), t);
    } else {
      logger.log(Level.SEVERE, String.format(msgOrFmt), t);
    }
  }

  private void logFin(String msgOrFmt, Object... params) {
    if (params.length > 0) {
      logger.fine(() -> String.format(msgOrFmt, params));
    } else {
      logger.fine(() -> msgOrFmt);
    }
  }

  private void logServ(String msgOrFmt, Object... params) {
    if (params.length > 0) {
      logger.severe(() -> String.format(msgOrFmt, params));
    } else {
      logger.severe(() -> msgOrFmt);
    }
  }

  private void resetMonitors() {
    jmsFailureCounter.set(0);
    continuousFailureCounter.set(0);
    failureCounter.set(0);
    firstFailureTimePoint = 0;
    breakedTimePoint = 0;
    tryCounter.set(0);
  }

  private void stateBrk() {
    resetMonitors();
    breakedTimePoint = System.currentTimeMillis();
    state = STATE_BRK;
    logFin("The message receive task start break mode, [%s]!", metaData);
    release(true);
  }

  private void stateRun() {
    resetMonitors();
    state = STATE_RUN;
    logFin("The message receive task start run mode, [%s]!", metaData);
  }

  private void stateTry() {
    resetMonitors();
    state = STATE_TRY;
    logFin("The message receive task start try mode, [%s]!", metaData);
  }

  static class MessageHandler implements MessageListener {

    final Object object;
    final AnnotatedMethod<?> method;

    MessageHandler(AnnotatedMethod<?> method) {
      super();
      this.method = method;
      final BeanManager beanManager = Corant.me().getBeanManager();
      final Bean<?> propertyResolverBean =
          beanManager.resolve(beanManager.getBeans(method.getJavaMember().getDeclaringClass()));
      final CreationalContext<?> creationalContext =
          beanManager.createCreationalContext(propertyResolverBean);
      object = beanManager.getReference(propertyResolverBean,
          method.getJavaMember().getDeclaringClass(), creationalContext);
    }

    @Override
    public void onMessage(Message message) {
      try {
        method.getJavaMember().invoke(object, message);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        logger.log(Level.SEVERE, e,
            () -> String.format("5-x. Invok message receive method %s occurred error.",
                method.getJavaMember()));
        throw new CorantRuntimeException(e);
      }
    }
  }

}
