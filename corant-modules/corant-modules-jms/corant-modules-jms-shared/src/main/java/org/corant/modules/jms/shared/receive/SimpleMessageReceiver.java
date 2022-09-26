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

import static org.corant.context.Beans.findNamed;
import static org.corant.context.Beans.select;
import static org.corant.shared.util.Strings.isNotBlank;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
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
import org.corant.modules.jms.receive.ManagedMessageReceiver;
import org.corant.modules.jms.receive.ManagedMessageReceivingExceptionListener;
import org.corant.modules.jms.receive.ManagedMessageReceivingHandler;
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
public class SimpleMessageReceiver implements ManagedMessageReceiver {

  protected static final Logger logger = Logger.getLogger(SimpleMessageReceiver.class.getName());

  // config
  protected final MessageReceivingMetaData meta;
  protected final int receiveThreshold;
  protected final long receiveTimeout;

  // worker object
  protected final ConnectionFactory connectionFactory;
  protected final ManagedMessageReceivingHandler messageHandler;
  protected final MessageReceivingMediator mediator;
  protected volatile Connection connection;
  protected volatile Session session;
  protected volatile MessageConsumer messageConsumer;

  protected SimpleMessageReceiver(MessageReceivingMetaData metaData,
      ManagedMessageReceivingHandler messageHandler, MessageReceivingMediator mediator) {
    meta = metaData;
    connectionFactory = createConnectionFactory(metaData.getConnectionFactoryId());
    this.messageHandler = messageHandler;
    this.mediator = mediator;
    receiveThreshold = metaData.getReceiveThreshold();
    receiveTimeout = metaData.getReceiveTimeout();

  }

  @Override
  public boolean initialize() throws JMSException {
    // initialize connection
    if (mediator.checkCancelled()) {
      return false;
    }
    if (connection == null) {
      // FIXME If the task is closed while waiting for the connection to be established, an
      // exception may be thrown
      if (meta.isXa()) {
        connection = ((XAConnectionFactory) connectionFactory).createXAConnection();
      } else {
        connection = connectionFactory.createConnection();
      }
      select(MessageReceivingTaskConfigurator.class).stream().sorted(Sortable::reverseCompare)
          .forEach(c -> c.configConnection(connection, meta));
      select(ManagedMessageReceivingExceptionListener.class).stream().min(Sortable::compare)
          .ifPresent(t -> {
            try {
              connection.setExceptionListener(t);
            } catch (JMSException e) {
              throw new CorantRuntimeException(e);
            }
          });
    }
    // initialize session
    if (session == null) {
      try {
        if (meta.isXa()) {
          session = ((XAConnection) connection).createXASession();
        } else {
          session = connection.createSession(meta.getAcknowledge());
        }
        select(MessageReceivingTaskConfigurator.class).stream().sorted(Sortable::compare)
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
        String selector =
            meta.getSpecifiedSelectors().getOrDefault(meta.getDestination(), meta.getSelector());
        if (isNotBlank(selector)) {
          messageConsumer = session.createConsumer(destination, selector);
        } else {
          messageConsumer = session.createConsumer(destination);
        }
        select(MessageReceivingTaskConfigurator.class).stream().sorted(Sortable::compare)
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

  @Override
  public synchronized boolean receive() {
    logger.log(Level.FINE, () -> String.format(">>> Begin receiving messages, %s.", meta));
    Throwable throwable = null;
    try {
      if (initialize()) {
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
      logger.log(Level.FINE, () -> String.format("<<< End receiving messages, %s.%n", meta));
    }
    return throwable == null;
  }

  @Override
  public void release(boolean stop) {
    try {
      closeMessageConsumerIfNecessary(stop);
      closeSessionIfNecessary(stop);
      closeConnectionIfNecessary(stop);
    } finally {
      // Noop!
    }
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
      Object result = messageHandler.onMessage(message, session);
      mediator.onPostMessageHandled(message, session, result);
      logger.log(Level.FINE, () -> String.format("Finished message handling, [%s]", meta));
    }
    return message;
  }

  protected ConnectionFactory createConnectionFactory(String connectionFactoryId) {
    return findNamed(ConnectionFactory.class, connectionFactoryId).orElseThrow(
        () -> new CorantRuntimeException("Can not find any JMS connection factory for %s.",
            connectionFactoryId));
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

  /**
   * Related work on consume occurred error, rollback transaction or rollback/recover session if
   * necessary
   *
   * @param e onException
   */
  protected void onException(Exception e) {
    try {
      mediator.onReceivingException(e);
    } catch (Exception ex) {
      logger.log(Level.SEVERE, ex, () -> String.format("Execution occurred error!, %s.", meta));
    }
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
   * @param message the message that received and has been handled
   * @throws JMSException postConsume
   */
  protected void postConsume(Message message) throws JMSException {
    if (message == null && mediator.checkCancelled()) {
      return;
    }
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
        XAResource xaresource = ((XASession) session).getXAResource();
        tm.getTransaction().enlistResource(xaresource);
      }
    } catch (Exception te) {
      throw generateJMSException(te);
    }
  }

}
