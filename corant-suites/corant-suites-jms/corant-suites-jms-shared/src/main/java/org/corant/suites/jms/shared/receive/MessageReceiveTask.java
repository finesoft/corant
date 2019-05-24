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
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.lang.reflect.InvocationTargetException;
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
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import org.corant.Corant;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.jms.shared.AbstractJMSExtension;
import org.corant.suites.jms.shared.Transactions;

/**
 * corant-suites-jms-shared
 *
 * @author bingo 上午11:33:15
 *
 */
public class MessageReceiveTask implements Runnable {

  final Logger logger = Logger.getLogger(MessageReceiveTask.class.getName());
  final MessageReceiverMetaData metaData;
  final Object connectionFactory;
  final MessageHandler messageHandler;
  final boolean xa;
  volatile boolean inProgress;
  volatile Connection connection;
  volatile Session session;
  volatile MessageConsumer messageConsumer;

  /**
   * @param metaData
   */
  protected MessageReceiveTask(MessageReceiverMetaData metaData) {
    super();
    this.metaData = metaData;
    xa = AbstractJMSExtension.retrieveConfig(metaData.getConnectionFactoryId()).isXa();
    connectionFactory =
        AbstractJMSExtension.retriveConnectionFactory(metaData.getConnectionFactoryId());
    messageHandler = new MessageHandler(metaData.getMethod());
    logFin("Create message receive task for %s", metaData);
  }

  public boolean isInProgress() {
    return inProgress;
  }

  @Override
  public void run() {
    logFin("Start message receive task.");
    try {
      if (initialize()) {
        int rt = metaData.getNumberOfReceivePerExecution();
        while (--rt >= 0) {
          logFin("Begin message consuming.");
          preConsume();
          Message message = consume();
          postConsume(message);
          logFin("End message consuming.\n");
        }
      }
    } catch (Throwable e) {
      onException(e);
    } finally {
      release(false);
    }
    logFin("Stopped message receive task.\n\n");
  }

  Message consume() throws JMSException {
    try {
      Message message = null;
      if (metaData.getReceiveTimeout() <= 0) {
        message = messageConsumer.receiveNoWait();
      } else {
        message = messageConsumer.receive(metaData.getReceiveTimeout());
      }
      logFin("5. Received message from queue, [%s]", metaData);
      if (message != null) {
        messageHandler.onMessage(message);
        logFin("5. Invoked message handler and processed message, [%s]", metaData);
      }
      return message;
    } catch (JMSException e) {
      logErr("5-x. Receive and process message occurred error, [%s]", metaData);
      throw e;
    }
  }

  boolean initialize() throws JMSException {
    inProgress = true;
    // initialize connection
    if (connection == null) {
      try {
        if (xa) {
          connection = ((XAConnectionFactory) connectionFactory).createXAConnection();
          logFin("1. Created message receive task xaconnection, [%s]", metaData);
        } else {
          connection = ((ConnectionFactory) connectionFactory).createConnection();
          logFin("1. Created message receive task connection, [%s]", metaData);
        }
        if (instance().select(MessageReceiveConnectionInitializer.class).isResolvable()) {
          instance().select(MessageReceiveConnectionInitializer.class).get().initialize(connection,
              metaData);
        }
      } catch (JMSException je) {
        logErr("1-x. Initialize message receive task connection occurred error, [%s]", metaData);
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
      } catch (JMSException je) {
        if (connection != null) {
          try {
            connection.close();
            connection = null;
            logFin(
                "2-1. Close message receive task connection when initialize session occurred error, [%s]",
                metaData);
          } catch (JMSException e) {
            logErr(
                "2-x. Close message receive task connection occurred error when initialize session occurred error, [%s]",
                metaData);
            throw je;
          }
        }
        logErr("2-x. Initialize message receive task session occurred error, [%s]", metaData);
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
          logErr(
              "3-x. Close message receive task session and connection occurred error when initialize consumer occurred error, [%s]",
              metaData);
          throw je;
        }
        logErr("3-x.Initialize message receive task consumer occurred error, [%s]", metaData);
        throw je;
      }
    }
    // start connection
    connection.start();
    logFin("4. Message receive task connection started, [%s]", metaData);
    return true;
  }

  void onException(Throwable e) {
    try {
      if (xa && Transactions.isInTransaction()) {
        Transactions.transactionManager().rollback();
        logErr("8-x. Rollback message receive task JTA transaction when occurred error, [%s]",
            metaData);
      } else if (session != null) {
        if (metaData.getAcknowledge() == Session.SESSION_TRANSACTED) {
          session.rollback();
          logErr("8-x. Rollback message receive task session when occurred error, [%s]", metaData);
        } else if (metaData.getAcknowledge() == Session.CLIENT_ACKNOWLEDGE) {
          session.recover();
          logErr("8-x. Recover message receive task session when occurred error, [%s]", metaData);
        }
      }
    } catch (Exception te) {
      logger.log(Level.SEVERE, te, () -> "Rollback message receive occurred error");
      throw new CorantRuntimeException(e);
    }
  }

  void postConsume(Message message)
      throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
      SecurityException, IllegalStateException, SystemException, JMSException {
    try {
      if (xa) {
        Transactions.transactionManager().commit();
        logFin("7-1. Commit message receive task JTA transaction, [%s]", metaData);
      } else if (metaData.getAcknowledge() == Session.SESSION_TRANSACTED) {
        session.commit();
        logFin("7-2. Commit message receive task session, [%s]", metaData);
      } else if (metaData.getAcknowledge() == Session.CLIENT_ACKNOWLEDGE && message != null) {
        message.acknowledge();
        logFin("7-3. Acknowledge message message receive task session, [%s]", metaData);
      }
    } catch (RollbackException | HeuristicMixedException | HeuristicRollbackException
        | SecurityException | IllegalStateException | SystemException te) {
      logErr("7-x. Commit message receive task JTA transaction occurred error, [%s]", metaData);
      throw te;
    } catch (JMSException je) {
      logErr("7-x. Commit/Acknowledge message receive task session or message occurred error, [%s]",
          metaData);
      throw je;
    }
  }

  void preConsume() throws NotSupportedException, SystemException, JMSException {
    try {
      if (xa) {
        Transactions.transactionManager().begin();
        Transactions.registerXAResource(((XASession) session).getXAResource());
        logFin("4-1. Message receive task JTA transaction began, [%s]", metaData);
      }
    } catch (NotSupportedException | SystemException te) {
      logErr("4-x. Initialize message receive task JTA environment occurred error, [%s]", metaData);
      throw te;
    } catch (JMSException je) {
      logErr(
          "4-x. Enlist message receive task session xa resource to JTA environment occurred error, [%s]",
          metaData);
      throw je;
    }
  }

  void release(boolean stop) {
    try {
      if (messageConsumer != null) {
        messageConsumer.close();
        messageConsumer = null;
      }
      if (!stop) {
        // if (metaData.getCacheLevel() <= 2 && messageConsumer != null) {
        // messageConsumer.close();
        // messageConsumer = null;
        // }
        if (metaData.getCacheLevel() <= 1 && session != null) {
          if (xa && Transactions.isInTransaction()) {
            Transactions.deregisterXAResource(((XASession) session).getXAResource());
          }
          session.close();
          session = null;
          if (connection != null) {
            connection.stop();
          }
        }
        if (metaData.getCacheLevel() <= 0 && connection != null) {
          connection.close();
          connection = null;
        }
      } else {
        // if (messageConsumer != null) {
        // messageConsumer.close();
        // messageConsumer = null;
        // }
        if (session != null) {
          if (xa && Transactions.isInTransaction()) {
            Transactions.deregisterXAResource(((XASession) session).getXAResource());
          }
          session.close();
          session = null;
        }
        if (connection != null) {
          connection.stop();
          connection.close();
          connection = null;
        }
      }
    } catch (JMSException e) {
      // TODO
      throw new CorantRuntimeException(e);
    } finally {
      inProgress = false;
    }
  }

  private void logErr(String msgOrFmt, Object... params) {
    if (params.length > 0) {
      logger.severe(() -> String.format(msgOrFmt, params));
    } else {
      logger.severe(() -> msgOrFmt);
    }
  }

  private void logFin(String msgOrFmt, Object... params) {
    if (params.length > 0) {
      logger.fine(() -> String.format(msgOrFmt, params));
    } else {
      logger.fine(() -> msgOrFmt);
    }
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
        throw new CorantRuntimeException(e);
      }
    }
  }
}
