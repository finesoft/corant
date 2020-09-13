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

import static org.corant.context.Instances.resolve;
import static org.corant.context.Instances.select;
import static org.corant.shared.util.Strings.isNotBlank;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import javax.jms.XASession;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;
import org.corant.context.proxy.ContextualMethodHandler;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Sortable;
import org.corant.suites.jms.shared.annotation.MessageSerialization.MessageSerializationLiteral;
import org.corant.suites.jms.shared.context.MessageSerializer;
import org.corant.suites.jta.shared.TransactionService;

/**
 * corant-suites-jms-shared
 *
 * <p>
 * JMS objects like connection, session, consumer and producer were designed to be re-used, but non
 * thread safe. In most implementations connection and session are pretty heavyweight to setup and
 * consumer usually requires a network round trip to set up. Producer is often more lightweight,
 * although there is often some overhead in creating it.
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
public abstract class AbstractMessageReceiverTask implements Runnable {

  final Logger logger = Logger.getLogger(this.getClass().getName());

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
  protected volatile boolean lastExecutionSuccessfully = false;// 20200602 change to false

  protected AbstractMessageReceiverTask(MessageReceiverMetaData metaData) {
    super();
    meta = metaData;
    xa = metaData.xa();
    connectionFactory = metaData.connectionFactory();
    messageListener = new MessageHandler(metaData.getMethod());
    receiveThreshold = metaData.getReceiveThreshold();
    receiveTimeout = metaData.getReceiveTimeout();
    loopInterval = metaData.getLoopIntervalMs();
  }

  protected void closeConnectionIfNecessary(boolean forceClose) {
    if ((meta.getCacheLevel() <= 0 || forceClose) && connection != null) {
      try {
        connection.stop();
        connection.close();
      } catch (JMSException e) {
        logger.log(Level.SEVERE, e, () -> String.format("Close connection occurred error!", meta));
      } finally {
        messageConsumer = null;
        session = null;
        connection = null;
      }
    }
  }

  protected void closeMessageConsumerIfNecessary(boolean forceClose) {
    if ((meta.getCacheLevel() <= 2 || forceClose) && messageConsumer != null) {
      try {
        messageConsumer.close();
      } catch (JMSException e) {
        logger.log(Level.SEVERE, e, () -> String.format("Close consumer occurred error!", meta));
      } finally {
        messageConsumer = null;
      }
    }
  }

  protected void closeSessionIfNecessary(boolean forceClose) {
    if ((meta.getCacheLevel() <= 1 || forceClose) && session != null) {
      try {
        session.close();
        if (connection != null) {
          connection.stop();
        }
      } catch (JMSException e) {
        logger.log(Level.SEVERE, e, () -> String.format("Close session occurred error!", meta));
      } finally {
        messageConsumer = null;
        session = null;
      }
    }
  }

  protected Message consume() throws JMSException {
    final Message message;
    if (meta.getReceiveTimeout() <= 0) {
      message = messageConsumer.receiveNoWait();
    } else {
      message = messageConsumer.receive(meta.getReceiveTimeout());
    }
    if (message != null) {
      logger.log(Level.FINE, () -> String.format("Received message start handling, [%s]", meta));
      messageListener.onMessage(message);
      logger.log(Level.FINE, () -> String.format("Finished message handling, [%s]", meta));
    }
    return message;
  }

  protected void execute() {
    logger.log(Level.FINE, () -> String.format("Start receiving messages, %s", meta));
    Throwable throwable = null;
    try {
      if (initialize()) {
        int rt = receiveThreshold;
        while (--rt >= 0) {
          preConsume();
          Message message = consume();
          postConsume(message);
          if (message == null) {
            logger.log(Level.FINE, () -> String.format("No message for now, %s", meta));
            break;
          }
        }
      }
    } catch (Exception e) {
      onException(e);
      throwable = e;
    } finally {
      lastExecutionSuccessfully = throwable == null;
      logger.log(Level.FINE, () -> String.format("Stop receiving messages, %s", meta));
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
      try {
        if (xa) {
          connection = ((XAConnectionFactory) connectionFactory).createXAConnection();
        } else {
          connection = connectionFactory.createConnection();
        }
        select(MessageReceiverTaskConfigurator.class).stream().sorted(Sortable::compare)
            .forEach(c -> c.configConnection(connection, meta));
        meta.exceptionListener().ifPresent(listener -> listener.tryConfig(connection));
      } catch (JMSException je) {
        throw je;
      }
    }
    // initialize session
    if (session == null) {
      try {
        if (xa) {
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
      if (xa) {
        if (TransactionService.currentTransaction() != null) {
          TransactionService.transactionManager().rollback();
          logger.log(Level.SEVERE, () -> String.format("Rollback the transaction, %s", meta));
        }
      } else if (session != null) {
        if (meta.getAcknowledge() == Session.SESSION_TRANSACTED) {
          session.rollback();
          logger.log(Level.SEVERE, () -> String.format("Rollback the session, %s", meta));
        } else if (meta.getAcknowledge() == Session.CLIENT_ACKNOWLEDGE) {
          session.recover();
          logger.log(Level.SEVERE, () -> String.format("Recover the session, %s", meta));
        }
      }
    } catch (Exception te) {
      e.addSuppressed(te);
    } finally {
      logger.log(Level.SEVERE, e, () -> String.format("Execution occurred error!, %s", meta));
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
      if (xa) {
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
      if (xa) {
        TransactionService.transactionManager().begin();
        XAResource xar = ((XASession) session).getXAResource();
        TransactionService.enlistXAResourceToCurrentTransaction(xar);
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

    MessageHandler(ContextualMethodHandler method) {
      super();
      this.method = method;
      messageClass = method.getMethod().getParameters()[0].getType();
    }

    @Override
    public void onMessage(Message message) {
      try {
        method.invoke(resolvePayload(message));
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
          | JMSException e) {
        throw new CorantRuntimeException(e);
      }
    }

    Object resolvePayload(Message message) throws JMSException {
      if (messageClass == Message.class) {
        return message;
      } else if (messageClass == TextMessage.class) {
        return TextMessage.class.cast(message);
      } else if (messageClass == BytesMessage.class) {
        return BytesMessage.class.cast(message);
      } else if (messageClass == MapMessage.class) {
        return MapMessage.class.cast(message);
      } else if (messageClass == StreamMessage.class) {
        return StreamMessage.class.cast(message);
      } else if (messageClass == ObjectMessage.class) {
        return ObjectMessage.class.cast(message);
      } else {
        String serialSchema = message.getStringProperty(MessageSerializer.MSG_SERIAL_SCHAME);
        if (isNotBlank(serialSchema)) {
          MessageSerializer serializer =
              resolve(MessageSerializer.class, MessageSerializationLiteral.of(serialSchema));
          return serializer.deserialize(message, messageClass);
        }
      }
      throw new IllegalArgumentException("Can not convert message payload to " + messageClass);
    }

  }

}
