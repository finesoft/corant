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
  protected final MessageReceiverMetaData meta;
  protected final ConnectionFactory connectionFactory;
  protected final MessageListener messageListener;
  protected final boolean xa;
  protected final int receiveThreshold;
  protected final long receiveTimeout;
  protected final long loopInterval;

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
  protected final Duration breakedDuration;
  protected final int tryThreshold;

  protected volatile byte state = STATE_RUN;
  protected volatile boolean lastExecutionSuccessfully = true;
  protected volatile long breakedTimePoint;

  protected final AtomicInteger failureCounter = new AtomicInteger(0);
  protected final AtomicInteger tryCounter = new AtomicInteger(0);

  /**
   * @param meta
   */
  protected MessageReceiverTask(MessageReceiverMetaData metaData) {
    super();
    meta = metaData;
    xa = metaData.xa();
    connectionFactory = metaData.connectionFactory();
    messageListener = new MessageHandler(metaData.getMethod());
    failureThreshold = metaData.getFailureThreshold();
    jmsFailureThreshold = max(failureThreshold / 2, 2);
    breakedDuration = metaData.getBreakedDuration();
    tryThreshold = metaData.getTryThreshold();
    receiveThreshold = metaData.getReceiveThreshold();
    receiveTimeout = metaData.getReceiveTimeout();
    loopInterval = metaData.getLoopIntervalMs();
    log(Level.FINE, null, "Create message receive task for %s", metaData);
  }

  protected static void log(Level l, Throwable t, String msgOrFmt, Object... params) {
    if (params.length > 0) {
      logger.log(l, t, () -> String.format(msgOrFmt, params));
    } else {
      logger.log(l, t, () -> msgOrFmt);
    }
  }

  public boolean isInProgress() {
    return inProgress;
  }

  @Override
  public void run() {
    Exception ex = null;
    if (!preRun()) {
      tryThreadSleep(loopInterval);
      return;
    }
    try {
      log(Level.FINE, null, "Start message receive task.");
      if (initialize()) {
        int rt = receiveThreshold;
        while (--rt >= 0) {
          log(Level.FINE, null, "Start receiving messages.");
          preConsume();
          Message message = consume();
          postConsume(message);
          if (message == null) {
            break;
          }
          log(Level.FINE, null, "Stop receiving messages.");
        }
      }
    } catch (Exception e) {
      onException(ex = e);
    } finally {
      lastExecutionSuccessfully = ex == null;
      postRun();
      log(Level.FINE, null, "Stopped message receive task.");
    }
  }

  protected void closeConnectionIfNecessary(boolean forceClose) {
    if ((meta.getCacheLevel() <= 0 || forceClose) && connection != null) {
      try {
        connection.stop();
        connection.close();
      } catch (JMSException e) {
        log(Level.SEVERE, null, "9-4x. Stop and close message receive task occurred error, [%s]",
            meta);
      } finally {
        messageConsumer = null;
        session = null;
        connection = null;
        log(Level.FINE, null, "9-4. Stop and close message receive task connection, [%s]", meta);
      }
    }
  }

  protected void closeMessageConsumerIfNecessary(boolean forceClose) {
    if ((meta.getCacheLevel() <= 2 || forceClose) && messageConsumer != null) {
      try {
        messageConsumer.close();
      } catch (JMSException e) {
        log(Level.SEVERE, e, "9-1x. Close message receive task consumer occurred error, [%s]",
            meta);
      } finally {
        messageConsumer = null;
        log(Level.FINE, null, "9-1. Close message receive task consumer, [%s]", meta);
      }
    }
  }

  protected void closeSessionIfNecessary(boolean forceClose) {
    if ((meta.getCacheLevel() <= 1 || forceClose) && session != null) {
      try {
        session.close();
        log(Level.FINE, null, "9-2. Close message receive task session, [%s]", meta);
        if (connection != null) {
          connection.stop();
          log(Level.FINE, null, "9-3. Stop message receive task connection, [%s]", meta);
        }
      } catch (JMSException e) {
        log(Level.FINE, null, "9-3x. Stop message receive task connection occurred error, [%s]",
            meta);
      } finally {
        messageConsumer = null;
        session = null;
      }
    }
  }

  protected Message consume() throws JMSException {
    try {
      Message message = null;
      if (meta.getReceiveTimeout() <= 0) {
        message = messageConsumer.receiveNoWait();
      } else {
        message = messageConsumer.receive(meta.getReceiveTimeout());
      }
      if (message != null) {
        log(Level.FINE, null, "5. Received message from queue and start handling, [%s]", meta);
        messageListener.onMessage(message);
        log(Level.FINE, null, "6. Finish message handle, [%s]", meta);
      }
      return message;
    } catch (JMSException e) {
      log(Level.SEVERE, null, "6-x. Receive and process message occurred error, [%s]", meta);
      throw e;
    }
  }

  protected JMSException generateJMSException(Exception t) {
    if (t instanceof JMSException) {
      return (JMSException) t;
    } else {
      JMSException jmsException = new JMSException(t.getMessage());
      jmsException.setLinkedException(t);
      return jmsException;
    }
  }

  protected boolean initialize() throws JMSException {
    inProgress = true;
    // initialize connection
    if (connection == null) {
      try {
        if (xa) {
          connection = ((XAConnectionFactory) connectionFactory).createXAConnection();
          log(Level.FINE, null, "1. Created message receive task xaconnection, [%s]", meta);
        } else {
          connection = connectionFactory.createConnection();
          log(Level.FINE, null, "1. Created message receive task connection, [%s]", meta);
        }
        select(MessageReceiverTaskConfigurator.class).stream()
            .sorted(ComparableConfigurator::compare)
            .forEach(c -> c.configConnection(connection, meta));
        meta.exceptionListener().ifPresent(listener -> listener.tryConfig(connection));
      } catch (JMSException je) {
        log(Level.SEVERE, null,
            "1-x. Initialize message receive task connection occurred error, [%s]", meta);
        throw je;
      }
    }
    // initialize session
    if (session == null) {
      try {
        if (xa) {
          session = ((XAConnection) connection).createXASession();
          log(Level.FINE, null, "2. Created message receive task xasession, [%s]", meta);
        } else {
          session = connection.createSession(meta.getAcknowledge());
          log(Level.FINE, null, "2. Created message receive task session, [%s]", meta);
        }
        instance().select(MessageReceiverTaskConfigurator.class).stream()
            .sorted(ComparableConfigurator::compare).forEach(c -> c.configSession(session, meta));
      } catch (JMSException je) {
        if (connection != null) {
          try {
            connection.close();
            connection = null;
            log(Level.FINE, null,
                "2-1. Close message receive task connection when initialize session occurred error, [%s]",
                meta);
          } catch (JMSException e) {
            log(Level.SEVERE, null,
                "2-x. Close message receive task connection occurred error when initialize session occurred error, [%s]",
                meta);
            throw je;
          }
        }
        log(Level.SEVERE, null, "2-x. Initialize message receive task session occurred error, [%s]",
            meta);
        throw je;
      }
    }
    // initialize message consumer
    if (messageConsumer == null) {
      try {
        Destination destination = meta.isMulticast() ? session.createTopic(meta.getDestination())
            : session.createQueue(meta.getDestination());
        if (isNotBlank(meta.getSelector())) {
          messageConsumer = session.createConsumer(destination, meta.getSelector());
        } else {
          messageConsumer = session.createConsumer(destination);
        }
        instance().select(MessageReceiverTaskConfigurator.class).stream()
            .sorted(ComparableConfigurator::compare)
            .forEach(c -> c.configMessageConsumer(messageConsumer, meta));
        log(Level.FINE, null, "3. Created message receive task consumer, [%s]", meta);
      } catch (JMSException je) {
        try {
          if (session != null) {
            session.close();
            session = null;
            log(Level.FINE, null,
                "3-1. Close message receive task sesion when initialize consumer occurred error, [%s]",
                meta);
          }
          if (connection != null) {
            connection.close();
            connection = null;
            log(Level.FINE, null,
                "3-2. Close message receive task connection when initialize consumer occurred error, [%s]",
                meta);
          }
        } catch (JMSException e) {
          log(Level.SEVERE, null,
              "3-x. Close message receive task session and connection occurred error when initialize consumer occurred error, [%s]",
              meta);
          throw je;
        }
        log(Level.SEVERE, null, "3-x.Initialize message receive task consumer occurred error, [%s]",
            meta);
        throw je;
      }
    }
    // start connection
    connection.start();
    log(Level.FINE, null, "4. Message receive task was initialized, [%s]", meta);
    return true;
  }

  protected void onException(Exception e) {
    if (e instanceof JMSException) {
      jmsFailureCounter.incrementAndGet();
    }
    if (lastExecutionSuccessfully) {
      failureCounter.set(1);
    } else {
      failureCounter.incrementAndGet();
    }

    log(Level.SEVERE, e, "Message receiver task occurred error, %s", meta);
    try {
      if (xa) {
        if (TransactionService.currentTransaction() != null) {
          TransactionService.transactionManager().rollback();
          log(Level.SEVERE, null,
              "8-x. Rollback message receive task JTA transaction when occurred error, [%s]", meta);
        }
      } else if (session != null) {
        if (meta.getAcknowledge() == Session.SESSION_TRANSACTED) {
          session.rollback();
          log(Level.SEVERE, null,
              "8-x. Rollback message receive task session when occurred error, [%s]", meta);
        } else if (meta.getAcknowledge() == Session.CLIENT_ACKNOWLEDGE) {
          session.recover();
          log(Level.SEVERE, null,
              "8-x. Recover message receive task session when occurred error, [%s]", meta);
        }
      }
    } catch (Exception te) {
      log(Level.SEVERE, te, "Rollback message receive occurred error, %s", meta);
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
        log(Level.FINE, null, "7-1. Commit message receive task JTA transaction, [%s]", meta);
      } else if (meta.getAcknowledge() == Session.SESSION_TRANSACTED && session != null) {
        session.commit();
        log(Level.FINE, null, "7-2. Commit message receive task session, [%s]", meta);
      } else if (meta.getAcknowledge() == Session.CLIENT_ACKNOWLEDGE && message != null) {
        message.acknowledge();
        log(Level.FINE, null, "7-3. Acknowledge message message receive task session, [%s]", meta);
      }
    } catch (RollbackException | HeuristicMixedException | HeuristicRollbackException
        | SecurityException | IllegalStateException | SystemException te) {
      log(Level.SEVERE, null,
          "7-x. Commit message receive task JTA transaction occurred error, [%s]", meta);
      throw generateJMSException(te);
    } catch (JMSException je) {
      log(Level.SEVERE, null,
          "7-x. Commit/Acknowledge message receive task session or message occurred error, [%s]",
          meta);
      throw je;
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
          stateBrk();
          return;
        } else {
          if (tryCounter.incrementAndGet() >= tryThreshold) {
            stateRun();
          }
        }
      }
      release(jmsFailureCounter.compareAndSet(jmsFailureThreshold, 0));
    } catch (Exception e) {
      logger.log(Level.SEVERE, e,
          () -> String.format("On post run message receive task occurred error, %s", meta));
    }
  }

  protected void preConsume() throws JMSException {
    try {
      if (xa) {
        TransactionService.transactionManager().begin();
        TransactionService
            .enlistXAResourceToCurrentTransaction(((XASession) session).getXAResource());
        log(Level.FINE, null, "4-1. Message receive task JTA transaction began, [%s]", meta);
      }
    } catch (Exception te) {
      log(Level.SEVERE, null,
          "4-x. Enlist message receive task session xa resource to JTA environment occurred error, [%s]",
          meta);
      throw generateJMSException(te);
    }
  }

  protected boolean preRun() {
    if (state == STATE_BRK) {
      long countdownMs =
          breakedDuration.toMillis() - (System.currentTimeMillis() - breakedTimePoint);
      if (countdownMs > 0) {
        if (countdownMs < loopInterval * 3) {
          log(Level.INFO, null, "The message receive task was breaked countdown %s ms, [%s]!",
              countdownMs, meta);
        }
        return false;
      } else {
        stateTry();
        return true;
      }
    }
    return true;
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
    state = STATE_BRK;
    log(Level.WARNING, null, "The message receive task start break mode, [%s]!", meta);
    release(true);
  }

  protected void stateRun() {
    resetMonitors();
    state = STATE_RUN;
    log(Level.INFO, null, "The message receive task start run mode, [%s]!", meta);
  }

  protected void stateTry() {
    resetMonitors();
    state = STATE_TRY;
    log(Level.INFO, null, "The message receive task start try mode, [%s]!", meta);
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
        log(Level.SEVERE, e, "5-x. Invok message receive method %s occurred error.",
            method.getJavaMember());
        throw new CorantRuntimeException(e);
      }
    }
  }

}
