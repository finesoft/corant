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

import static org.corant.context.Instances.findNamed;
import static org.corant.context.Instances.select;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.XAConnectionFactory;
import org.corant.modules.jms.shared.context.JMSExceptionListener;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 上午11:48:50
 *
 */
@ApplicationScoped
public class MessageReceivingConnections {

  protected static final Logger logger =
      Logger.getLogger(MessageReceivingConnections.class.getName());
  protected static final Map<String, Connection> conns = new ConcurrentHashMap<>();
  protected static final Map<String, Connection> xaconns = new ConcurrentHashMap<>();

  public synchronized void shutdown() {
    conns.forEach((k, v) -> {
      try {
        logger.info(() -> String.format("Dismantle connection, the connection factory id %s.", k));
        v.stop();
        v.close();
      } catch (Exception e) {
        logger.log(Level.WARNING, e, () -> String
            .format("Dismantle connection occurred error, the connection factory id %s", k));
      }
    });
    conns.clear();
    xaconns.forEach((k, v) -> {
      try {
        logger
            .info(() -> String.format("Dismantle xa connection, the connection factory id %s.", k));
        v.stop();
        v.close();
      } catch (Exception e) {
        logger.log(Level.WARNING, e, () -> String
            .format("Dismantle xa connection occurred error, the connection factory id %s.", k));
      }
    });
    xaconns.clear();
  }

  public Connection startConnection(MessageReceivingMetaData meta) throws JMSException {
    final Map<String, Connection> useConns = meta.isXa() ? xaconns : conns;
    Connection conn = null;
    try {
      conn = useConns.computeIfAbsent(meta.getConnectionFactoryId(), cf -> createConnection(meta));
      conn.start();
    } catch (Throwable e) {
      logger.warning(
          () -> String.format("Start connection occurred error, the connection factory id %s.",
              meta.getConnectionFactoryId()));
      try {
        // the connection can't start, need to evict it for next step to re-create new connection.
        stopConnection(meta, true);
      } catch (Throwable se) {
        logger.warning(() -> String.format(
            "Stop and close connection occurred error, the connection factory id %s.",
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

  public synchronized void stopConnection(MessageReceivingMetaData meta, boolean removeAndClose)
      throws JMSException {
    final String connectionFactoryId = meta.getConnectionFactoryId();
    final boolean xa = meta.isXa();
    Connection conn;
    if (removeAndClose) {
      if (xa) {
        if ((conn = xaconns.remove(connectionFactoryId)) != null) {
          logger.info(() -> String.format(
              "Stop and close and remove xa connection, the connection factory id %s.",
              connectionFactoryId));
          conn.stop();
          conn.close();
        }
      } else {
        if ((conn = conns.remove(connectionFactoryId)) != null) {
          logger.info(() -> String.format(
              "Stop and close and remove connection, the connection factory id %s.",
              connectionFactoryId));
          conn.stop();
          conn.close();
        }
      }
    } else {
      if (xa) {
        if ((conn = xaconns.get(connectionFactoryId)) != null) {
          logger.info(() -> String.format("Stop xa connection, the connection factory id %s.",
              connectionFactoryId));
          conn.stop();
        }
      } else {
        if ((conn = conns.get(connectionFactoryId)) != null) {
          logger.info(() -> String.format("Stop connection, the connection factory id %s.",
              connectionFactoryId));
          conn.stop();
        }
      }
    }
  }

  protected Connection configureConnection(MessageReceivingMetaData meta, Connection connection) {
    select(MessageReceivingTaskConfigurator.class).stream().sorted(Sortable::reverseCompare)
        .forEach(c -> c.configConnection(connection, meta));
    select(JMSExceptionListener.class).stream().min(Sortable::compare)
        .ifPresent(listener -> listener.tryConfig(connection));
    return connection;
  }

  protected Connection createConnection(MessageReceivingMetaData meta) {
    logger.fine(() -> String.format("Create %s connection, the connection factory id %s.",
        meta.isXa() ? "xa" : "", meta.getConnectionFactoryId()));
    ConnectionFactory connectionFactory =
        findNamed(ConnectionFactory.class, meta.getConnectionFactoryId()).orElseThrow(
            () -> new CorantRuntimeException("Can not find any JMS connection factory by id %s.",
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
}
