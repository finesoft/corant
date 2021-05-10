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
package org.corant.modules.jms.shared.receive.retain;

import static org.corant.context.Instances.find;
import static org.corant.context.Instances.findNamed;
import static org.corant.context.Instances.resolve;
import static org.corant.context.Instances.select;
import static org.corant.shared.util.Strings.isNotBlank;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import org.corant.context.proxy.ContextualMethodHandler;
import org.corant.context.security.SecurityContext;
import org.corant.context.security.SecurityContexts;
import org.corant.modules.jms.shared.MessagePropertyNames;
import org.corant.modules.jms.shared.annotation.MessageSerialization.MessageSerializationLiteral;
import org.corant.modules.jms.shared.context.JMSExceptionListener;
import org.corant.modules.jms.shared.context.MessageSerializer;
import org.corant.modules.jms.shared.context.SecurityContextPropagator;
import org.corant.modules.jms.shared.context.SecurityContextPropagator.SimpleSecurityContextPropagator;
import org.corant.modules.jms.shared.receive.retain.MessageReceiverTaskFactory.CancellableTask;
import org.corant.modules.jta.shared.TransactionService;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-jms-shared
 *
 * <p>
 * JMS objects like connection, session, consumer and producer were designed to be re-used, but non
 * thread safe. In most implementations connection and session are pretty heavyweight to setup and
 * consumer usually requires a network round trip to set up. Producer is often more lightweight,
 * although there is often some overhead in creating it.
 *
 * NOTE: This is not threadsafe.
 *
 * Unfinish: use connection or session pool
 *
 * <p>
 * <a href = "https://developer.jboss.org/wiki/ShouldICacheJMSConnectionsAndJMSSessions"> Should I
 * cache JMS connections and JMS sessions</a>
 *
 * @author bingo 上午11:33:15
 *
 */
public abstract class AbstractMessageReceiverTask implements CancellableTask {

  final Logger logger = Logger.getLogger(this.getClass().getName());

  // config
  protected final MessageReceiverMetaData meta;
  protected final int receiveThreshold;
  protected final long receiveTimeout;
  protected final long loopIntervalMillis;

  // worker object
  protected final ConnectionFactory connectionFactory;
  protected final MessageListener messageListener;
  protected volatile Connection connection;
  protected volatile Session session;
  protected volatile MessageConsumer messageConsumer;
  protected volatile boolean lastExecutionSuccessfully = false;

  // executor controller
  protected final AtomicBoolean cancellation = new AtomicBoolean();

  protected AbstractMessageReceiverTask(MessageReceiverMetaData metaData) {
    meta = metaData;
    connectionFactory = createConnectionFactory(metaData.getConnectionFactoryId());
    messageListener = new MessageHandler(metaData.getMethod());
    receiveThreshold = metaData.getReceiveThreshold();
    receiveTimeout = metaData.getReceiveTimeout();
    loopIntervalMillis = metaData.getLoopIntervalMs();
  }

  @Override
  public synchronized boolean cancel() {
    return cancellation.compareAndSet(false, true);
  }

  protected void closeConnectionIfNecessary(boolean forceClose) {
    if (connection != null && (forceClose || meta.getCacheLevel() <= 0)) {
      try {
        connection.stop();
        connection.close();
      } catch (JMSException e) {
        logger.log(Level.SEVERE, e,
            () -> String.format("Close connection occurred error, [%s]", meta));
      } finally {
        messageConsumer = null;
        session = null;
        connection = null;
      }
    }
  }

  protected void closeMessageConsumerIfNecessary(boolean forceClose) {
    if (messageConsumer != null && (forceClose || meta.getCacheLevel() <= 2)) {
      try {
        messageConsumer.close();
      } catch (JMSException e) {
        logger.log(Level.SEVERE, e,
            () -> String.format("Close consumer occurred error, [%s]", meta));
      } finally {
        messageConsumer = null;
      }
    }
  }

  protected void closeSessionIfNecessary(boolean forceClose) {
    if (session != null && (forceClose || meta.getCacheLevel() <= 1)) {
      try {
        session.close();
        if (connection != null) {
          connection.stop();
        }
      } catch (JMSException e) {
        logger.log(Level.SEVERE, e,
            () -> String.format("Close session occurred error, [%s]", meta));
      } finally {
        messageConsumer = null;
        session = null;
      }
    }
  }

  protected Message consume() throws JMSException {
    final Message message;
    if (receiveTimeout <= 0) {
      message = messageConsumer.receiveNoWait();
    } else {
      message = messageConsumer.receive(receiveTimeout);
    }
    if (message != null) {
      logger.log(Level.FINE, () -> String.format("Received message start handling, [%s]", meta));
      messageListener.onMessage(message);
      logger.log(Level.FINE, () -> String.format("Finished message handling, [%s]", meta));
    }
    return message;
  }

  protected ConnectionFactory createConnectionFactory(String connectionFactoryId) {
    return findNamed(ConnectionFactory.class, connectionFactoryId).orElseThrow(
        () -> new CorantRuntimeException("Can not find any JMS connection factory for %s.",
            connectionFactoryId));
  }

  protected synchronized void execute() {
    logger.log(Level.FINE, () -> String.format("Begin receiving messages, %s.", meta));
    Throwable throwable = null;
    try {
      if (!cancellation.get() && initialize()) {
        int rt = receiveThreshold;
        while (--rt >= 0) {
          preConsume();
          Message message = consume();
          postConsume(message);
          if (message == null) {
            logger.log(Level.FINE, () -> String.format("No message for now, %s.", meta));
            break;
          }
        }
      }
    } catch (Exception e) {
      throwable = e;
      onException(e);
    } finally {
      lastExecutionSuccessfully = throwable == null;
      logger.log(Level.FINE, () -> String.format("End receiving messages, %s.", meta));
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
    // initialize connection
    if (connection == null) {
      if (meta.isXa()) {
        connection = ((XAConnectionFactory) connectionFactory).createXAConnection();
      } else {
        connection = connectionFactory.createConnection();
      }
      select(MessageReceiverTaskConfigurator.class).stream().sorted(Sortable::reverseCompare)
          .forEach(c -> c.configConnection(connection, meta));
      select(JMSExceptionListener.class).stream().min(Sortable::compare)
          .ifPresent(listener -> listener.tryConfig(connection));
    }
    // initialize session
    if (session == null) {
      try {
        if (meta.isXa()) {
          session = ((XAConnection) connection).createXASession();
        } else {
          session = connection.createSession(meta.getAcknowledge());
        }
        select(MessageReceiverTaskConfigurator.class).stream().sorted(Sortable::compare)
            .forEach(c -> c.configSession(session, meta));
      } catch (JMSException je) {
        if (connection != null) {
          try {
            connection.close();
            connection = null;
          } catch (JMSException e) {
            je.addSuppressed(e);
          }
        }
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
        select(MessageReceiverTaskConfigurator.class).stream().sorted(Sortable::compare)
            .forEach(c -> c.configMessageConsumer(messageConsumer, meta));
      } catch (JMSException je) {
        try {
          if (session != null) {
            session.close();
            session = null;
          }
          if (connection != null) {
            connection.close();
            connection = null;
          }
        } catch (JMSException e) {
          je.addSuppressed(e);
        }
        throw je;
      }
    }
    connection.start();
    return true;
  }

  /**
   * Related work on consume occurred error, rollback transaction or rollback/recover session if
   * necessary
   *
   * @param e onException
   */
  protected void onException(Exception e) {
    try {
      if (meta.isXa()) {
        if (TransactionService.currentTransaction() != null) {
          TransactionService.transactionManager().rollback();
          logger.log(Level.SEVERE, () -> String.format("Rollback the transaction, %s.", meta));
        }
      } else if (session != null) {
        if (meta.getAcknowledge() == Session.SESSION_TRANSACTED) {
          session.rollback();
          logger.log(Level.SEVERE, () -> String.format("Rollback the session, %s.", meta));
        } else if (meta.getAcknowledge() == Session.CLIENT_ACKNOWLEDGE) {
          session.recover();
          logger.log(Level.SEVERE, () -> String.format("Recover the session, %s.", meta));
        }
      }
    } catch (Exception te) {
      e.addSuppressed(te);
    } finally {
      logger.log(Level.SEVERE, e, () -> String.format("Execution occurred error!, %s.", meta));
    }
  }

  /**
   * Related work after consume, commit transaction or session if necessary
   *
   * @param message
   * @throws JMSException postConsume
   */
  protected void postConsume(Message message) throws JMSException {
    try {
      if (meta.isXa()) {
        if (TransactionService.currentTransaction() != null) {
          TransactionService.transactionManager().commit();
        }
      } else if (meta.getAcknowledge() == Session.SESSION_TRANSACTED && session != null) {
        session.commit();
      } else if (meta.getAcknowledge() == Session.CLIENT_ACKNOWLEDGE && message != null) {
        message.acknowledge();
      }
    } catch (RollbackException | HeuristicMixedException | HeuristicRollbackException
        | SecurityException | IllegalStateException | SystemException te) {
      throw generateJMSException(te);
    } catch (JMSException je) {
      throw je;
    }
  }

  /**
   * Related work before starting to consume, begin transaction if necessary
   *
   * @throws JMSException preConsume
   */
  protected void preConsume() throws JMSException {
    try {
      if (meta.isXa()) {
        final TransactionManager tm = TransactionService.transactionManager();
        if (meta.getTxTimeout() > 0) {
          tm.setTransactionTimeout(meta.getTxTimeout());
        }
        tm.begin();
        XAResource xar = ((XASession) session).getXAResource();
        tm.getTransaction().enlistResource(xar);
      }
    } catch (Exception te) {
      throw generateJMSException(te);
    }
  }

  protected void release(boolean stop) {
    try {
      closeMessageConsumerIfNecessary(stop);
      closeSessionIfNecessary(stop);
      closeConnectionIfNecessary(stop);
    } finally {
    }
  }

  static class MessageHandler implements MessageListener {

    final ContextualMethodHandler method;
    final Class<?> messageClass;
    final Logger logger = Logger.getLogger(MessageHandler.class.getName());

    MessageHandler(ContextualMethodHandler method) {
      this.method = method;
      messageClass = method.getMethod().getParameters()[0].getType();
    }

    @Override
    public void onMessage(Message message) {
      try {
        resolveSecurityContext(message);
        method.invoke(resolvePayload(message));
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
          | JMSException e) {
        throw new CorantRuntimeException(e);
      } finally {
        SecurityContexts.setCurrent(null);
      }
    }

    Object resolvePayload(Message message) throws JMSException {
      String serialSchema = message.getStringProperty(MessagePropertyNames.MSG_SERIAL_SCHAME);
      if (isNotBlank(serialSchema)) {
        if (!Message.class.isAssignableFrom(messageClass)) {
          MessageSerializer serializer =
              resolve(MessageSerializer.class, MessageSerializationLiteral.of(serialSchema));
          return serializer.deserialize(message, messageClass);
        } else {
          logger.warning(() -> String.format(
              "The message has serialization scheme property, but the message consumer still use the native javax.jms.Message as method %s parameter type.",
              method));
        }
      }
      return message;
    }

    void resolveSecurityContext(Message message) {
      try {
        SecurityContext ctx = find(SecurityContextPropagator.class)
            .orElse(SimpleSecurityContextPropagator.INSTANCE).extract(message);
        SecurityContexts.setCurrent(ctx);
      } catch (Exception e) {
        logger.log(Level.SEVERE, e,
            () -> "Resolve security context propagation from message occurred error!");
      }
    }

  }

}
