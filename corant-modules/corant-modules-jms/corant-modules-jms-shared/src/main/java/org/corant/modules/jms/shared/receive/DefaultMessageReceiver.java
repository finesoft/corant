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
import static org.corant.context.Beans.select;
import static org.corant.shared.util.Strings.isNotBlank;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.jms.Connection;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import jakarta.jms.XAConnection;
import jakarta.jms.XASession;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import org.corant.modules.jms.receive.ManagedMessageReceiver;
import org.corant.modules.jms.receive.ManagedMessageReceivingHandler;
import org.corant.modules.jta.shared.TransactionService;
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
 * NOTE: This is not thread safe.
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
public class DefaultMessageReceiver implements ManagedMessageReceiver {

  protected static final Logger logger = Logger.getLogger(DefaultMessageReceiver.class.getName());

  // configuration
  protected final MessageReceivingMetaData meta;
  protected final int receiveThreshold;
  protected final long receiveTimeout;

  // workhorse
  protected final ManagedMessageReceivingHandler messageHandler;
  protected final MessageReceivingMediator mediator;
  protected volatile Connection connection;
  protected volatile Session session;
  protected volatile MessageConsumer messageConsumer;

  protected DefaultMessageReceiver(MessageReceivingMetaData metaData,
      ManagedMessageReceivingHandler messageHandler, MessageReceivingMediator mediator) {
    meta = metaData;
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

    // FIXME If the task is closed while waiting for the connection to be established, an
    // exception may be thrown

    // initialize connection
    Connection connection = resolve(MessageReceivingConnections.class).startConnection(meta);
    if (connection != this.connection) {
      // connection was changed, we must release current session & consumer
      if (session != null) {
        release(true);
      }
      this.connection = connection;
    }

    // initialize session
    if (session == null) {
      if (meta.isXa()) {
        session = ((XAConnection) this.connection).createXASession();
      } else {
        session = this.connection.createSession(meta.getAcknowledge());
      }
      select(MessageReceivingTaskConfigurator.class).stream().sorted(Sortable::compare)
          .forEach(c -> c.configSession(session, meta));
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
        } catch (JMSException e) {
          je.addSuppressed(e);
        }
        throw je;
      }
    }
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
    } finally {
      // Noop!
    }
  }

  protected void closeMessageConsumerIfNecessary(boolean forceClose) {
    if (messageConsumer != null && (forceClose || meta.getCacheLevel() <= 2)) {
      logger.log(Level.INFO, () -> String.format("Close current consumer [%s].", meta));
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
      logger.log(Level.INFO, () -> String.format("Close current session of [%s].", meta));
      try {
        session.close();
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
      logger.log(Level.FINE, () -> String.format("Complete message handling, [%s]", meta));
    }
    return message;
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
      logger.log(Level.SEVERE, ex,
          () -> String.format("Message receiving task occurred error!, %s.", meta));
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
      logger.log(Level.SEVERE, e,
          () -> String.format("Message receiving occurred error!, %s.", meta));
    }
  }

  /**
   * Related work after consume, commit transaction or session if necessary
   *
   * @param message the message that received and has been handled
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
