/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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

import static java.lang.String.format;
import static org.corant.context.Beans.findNamed;
import static org.corant.context.Beans.select;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSException;
import jakarta.jms.XAConnectionFactory;
import org.corant.modules.jms.receive.ManagedMessageReceivingExceptionListener;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 上午11:48:50
 */
@ApplicationScoped
public class MessageReceivingConnections {

  protected static final Logger logger =
      Logger.getLogger(MessageReceivingConnections.class.getName());
  protected static final Map<String, Connection> conns = new ConcurrentHashMap<>();
  protected static final Map<String, Connection> xaconns = new ConcurrentHashMap<>();

  public synchronized static void stopConnection(MessageReceivingMetaData meta,
      boolean removeAndClose) throws JMSException {
    final String connectionFactoryId = meta.getConnectionFactoryId();
    final boolean xa = meta.isXa();
    Connection conn;
    if (removeAndClose) {
      if (xa) {
        if ((conn = xaconns.remove(connectionFactoryId)) != null) {
          logger.info(() -> format(
              "Stop and close and remove xa connection, the connection factory id [%s].",
              connectionFactoryId));
          conn.stop();
          conn.close();
        }
      } else if ((conn = conns.remove(connectionFactoryId)) != null) {
        logger.info(
            () -> format("Stop and close and remove connection, the connection factory id [%s].",
                connectionFactoryId));
        conn.stop();
        conn.close();
      }
    } else if (xa) {
      if ((conn = xaconns.get(connectionFactoryId)) != null) {
        logger.info(() -> format("Stop xa connection, the connection factory id [%s].",
            connectionFactoryId));
        conn.stop();
      }
    } else if ((conn = conns.get(connectionFactoryId)) != null) {
      logger.info(
          () -> format("Stop connection, the connection factory id [%s].", connectionFactoryId));
      conn.stop();
    }
  }

  public synchronized void shutdown() {
    conns.forEach((k, v) -> {
      try {
        logger.info(() -> format("Dismantle connection, the connection factory id [%s].", k));
        v.stop();
        v.close();
      } catch (Exception e) {
        logger.log(Level.WARNING, e,
            () -> format("Dismantle connection occurred error, the connection factory id [%s].",
                k));
      }
    });
    conns.clear();
    xaconns.forEach((k, v) -> {
      try {
        logger.info(() -> format("Dismantle xa connection, the connection factory id [%s].", k));
        v.stop();
        v.close();
      } catch (Exception e) {
        logger.log(Level.WARNING, e,
            () -> format("Dismantle xa connection occurred error, the connection factory id [%s].",
                k));
      }
    });
    xaconns.clear();
  }

  public Connection startConnection(MessageReceivingMetaData meta) throws JMSException {
    final Map<String, Connection> useConns = meta.isXa() ? xaconns : conns;
    Connection conn;
    try {
      conn = useConns.computeIfAbsent(meta.getConnectionFactoryId(), cf -> createConnection(meta));
      conn.start();
    } catch (Throwable e) {
      logger
          .warning(() -> format("Start connection occurred error, the connection factory id [%s].",
              meta.getConnectionFactoryId()));
      try {
        // connection can't start, need to evict it for the next step to re-create new connection.
        stopConnection(meta, true);
      } catch (Throwable se) {
        logger.warning(() -> format(
            "Stop and close connection occurred error, the connection factory id [%s].",
            meta.getConnectionFactoryId()));
        e.addSuppressed(se);
      }
      if (e instanceof JMSException) {
        throw e;
      } else {
        JMSException je = new JMSException("Start connection occurred error!");
        je.addSuppressed(e);
        throw je;
      }
    }
    return conn;
  }

  protected Connection configureConnection(MessageReceivingMetaData meta, Connection connection)
      throws JMSException {
    select(MessageReceivingTaskConfigurator.class).stream().sorted(Sortable::reverseCompare)
        .forEach(c -> c.configConnection(connection, meta));
    Optional<ManagedMessageReceivingExceptionListener> listener =
        select(ManagedMessageReceivingExceptionListener.class).stream().min(Sortable::compare);
    if (listener.isEmpty()) {
      connection.setExceptionListener(new MixedConnectionListener(meta, null));
    } else {
      connection.setExceptionListener(new MixedConnectionListener(meta, listener.get()));
    }
    return connection;
  }

  protected Connection createConnection(MessageReceivingMetaData meta) {
    logger.fine(() -> format("Create %s connection, the connection factory id [%s].",
        meta.isXa() ? "xa" : "", meta.getConnectionFactoryId()));
    ConnectionFactory connectionFactory =
        findNamed(ConnectionFactory.class, meta.getConnectionFactoryId()).orElseThrow(
            () -> new CorantRuntimeException("Can not find any JMS connection factory by id [%s].",
                meta.getConnectionFactoryId()));
    try {
      if (meta.isXa()) {
        return configureConnection(meta,
            ((XAConnectionFactory) connectionFactory).createXAConnection());
      } else {
        return configureConnection(meta, connectionFactory.createConnection());
      }
    } catch (JMSException e) {
      throw new CorantRuntimeException(e);
    }
  }

  static class MixedConnectionListener implements ExceptionListener {

    final Logger logger = Logger.getLogger(MixedConnectionListener.class.getName());
    final MessageReceivingMetaData meta;
    final ManagedMessageReceivingExceptionListener userExceptionListener;

    MixedConnectionListener(MessageReceivingMetaData meta,
        ManagedMessageReceivingExceptionListener userExceptionListener) {
      this.meta = meta;
      this.userExceptionListener = userExceptionListener;
    }

    @Override
    public void onException(JMSException e) {
      logger.log(Level.WARNING, e,
          () -> format("The connection occurred error, the connection factory id [%s].",
              meta.getConnectionFactoryId()));
      if (userExceptionListener != null) {
        try {
          userExceptionListener.onException(e);
        } catch (Exception ex) {
          logger.log(Level.WARNING, e,
              () -> format(
                  "User exception listener occurred error, the connection factory id [%s].",
                  meta.getConnectionFactoryId()));
        }
      }
      try {
        stopConnection(meta, true);
      } catch (JMSException ex) {
        logger.warning(() -> format(
            "Stop and close connection occurred error, the connection factory id [%s].",
            meta.getConnectionFactoryId()));
      }
    }

  }
}
