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
package org.corant.suites.jms.shared;

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.ObjectUtils.asStrings;
import static org.corant.suites.cdi.Instances.findNamed;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.jms.JMSException;
import javax.jms.JMSSecurityException;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import javax.jms.XASession;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.corant.suites.cdi.CDIs;
import org.corant.suites.jta.shared.TransactionIntegration;

/**
 * corant-suites-jms-shared
 *
 * @author bingo 下午5:12:28
 *
 */
public class JMSTransactionIntegration implements TransactionIntegration {

  static Map<AbstractJMSConfig, JMSRecoveryXAResource> recoveryXAResources =
      new ConcurrentHashMap<>();

  @Override
  public XAResource[] getRecoveryXAResources() {
    LOGGER.fine(() -> "Searching JMS XAResources for JTA recovery processes.");
    if (!CDIs.isEnabled()) {
      LOGGER.warning(
          () -> "Current CDI container can't access, so can't find any XAResource for JTA recovery processes.");
      return new XAResource[0];
    }
    List<XAResource> resources = new ArrayList<>();
    Instance<AbstractJMSExtension> extensions = CDI.current().select(AbstractJMSExtension.class);
    if (!extensions.isUnsatisfied()) {
      extensions.forEach(et -> et.getConfigManager().getAllWithNames().forEach((k, v) -> {
        if (v.isXa() && v.isEnable()) {
          resolveRecoveryXAResource(v).ifPresent(resources::add);
        }
      }));
    }
    if (isEmpty(resources)) {
      LOGGER.fine(() -> "JMS XAResources for JTA recovery processes not found.");
    }
    return isNotEmpty(resources) ? resources.toArray(new XAResource[resources.size()])
        : new XAResource[0];
  }

  Optional<XAResource> resolveRecoveryXAResource(AbstractJMSConfig config) {
    XAResource res = null;
    try {
      res = recoveryXAResources.computeIfAbsent(config, JMSRecoveryXAResource::new)
          .connectIfNecessary();
    } catch (XAException e) {
      LOGGER.log(Level.WARNING, e,
          () -> String.format("Can not resolve JMSRecoveryXAResource from connection factory [%s]",
              config.getName()));
    }
    return Optional.ofNullable(res);
  }

  public static class JMSRecoveryXAResource implements XAResource {
    final AbstractJMSConfig config;
    final XAConnectionFactory factory;
    final AtomicReference<XAConnection> connection = new AtomicReference<>();
    final AtomicReference<XASession> session = new AtomicReference<>();

    /**
     * @param factory
     */
    protected JMSRecoveryXAResource(AbstractJMSConfig config) {
      super();
      this.config = config;
      factory = findNamed(XAConnectionFactory.class, config.getConnectionFactoryId()).get();
      LOGGER.fine(() -> String.format("Found JMS [%s] XAResource for JTA recovery processes.",
          config.getConnectionFactoryId()));
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
      LOGGER.fine(() -> String.format(
          "Commit the JMS XA [%s] transaction [%s] (onePhase:[%s]) that run in JTA recovery processes!",
          config.getConnectionFactoryId(), xid.toString(), onePhase));

      if (isConnected()) {
        session.get().getXAResource().commit(xid, onePhase);
        return;
      }
      connectIfNecessary();
      try {
        session.get().getXAResource().commit(xid, onePhase);
      } finally {
        disconnect();
      }
    }

    @Override
    public void end(Xid xid, int flags) throws XAException {
      LOGGER.fine(() -> String.format(
          "End the work performed on behalf of the JMS XA [%s] transaction branch [%s] flags [%s] that run in JTA recovery processes!",
          config.getConnectionFactoryId(), xid.toString(), flags));
      if (isConnected()) {
        session.get().getXAResource().end(xid, flags);
        return;
      }
      connectIfNecessary();
      try {
        session.get().getXAResource().end(xid, flags);
      } finally {
        disconnect();
      }
    }

    @Override
    public void forget(Xid xid) throws XAException {
      LOGGER.fine(() -> String.format(
          "Forget about the JMS XA [%s] heuristicallycompleted transaction branch [%s] that run in JTA recovery processes!",
          config.getConnectionFactoryId(), xid.toString()));

      if (isConnected()) {
        session.get().getXAResource().forget(xid);
        return;
      }
      connectIfNecessary();
      try {
        session.get().getXAResource().forget(xid);
      } finally {
        disconnect();
      }
    }

    @Override
    public int getTransactionTimeout() throws XAException {
      if (isConnected()) {
        return session.get().getXAResource().getTransactionTimeout();
      }
      connectIfNecessary();
      try {
        return session.get().getXAResource().getTransactionTimeout();
      } finally {
        disconnect();
      }
    }

    @Override
    public boolean isSameRM(XAResource xares) throws XAException {
      if (isConnected()) {
        return session.get().getXAResource().isSameRM(xares);
      }
      connectIfNecessary();
      try {
        return session.get().getXAResource().isSameRM(xares);
      } finally {
        disconnect();
      }
    }

    @Override
    public int prepare(Xid xid) throws XAException {
      if (isConnected()) {
        return session.get().getXAResource().prepare(xid);
      }
      connectIfNecessary();
      try {
        return session.get().getXAResource().prepare(xid);
      } finally {
        disconnect();
      }
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
      Xid[] xids = new Xid[0];
      try {
        if (isConnected()) {
          xids = session.get().getXAResource().recover(flag);
        } else {
          connectIfNecessary();
          xids = session.get().getXAResource().recover(flag);
          disconnect();
        }
      } finally {
        final Xid[] useXids = xids;
        if (useXids != null && useXids.length > 0) {
          LOGGER.fine(() -> String.format(
              "Found prepared JMS XA [%s] transaction branches: [%s] for JTA recovery processes.",
              config.getConnectionFactoryId(), String.join(", ", asStrings((Object[]) useXids))));
        } else {
          LOGGER.fine(() -> String.format(
              "Prepared JMS XA [%s] transaction branches for JTA recovery processes not found.",
              config.getConnectionFactoryId()));
        }
        if (flag == XAResource.TMENDRSCAN) {
          disconnect();
        }
      }
      return xids;
    }

    @Override
    public void rollback(Xid xid) throws XAException {
      LOGGER.fine(() -> String.format(
          "Roll back work done on behalfof the JMS XA [%s] transaction branch [%s] that run in JTA recovery processes!",
          config.getConnectionFactoryId(), xid.toString()));
      if (isConnected()) {
        session.get().getXAResource().rollback(xid);
        return;
      }
      connectIfNecessary();
      try {
        session.get().getXAResource().rollback(xid);
      } finally {
        disconnect();
      }
    }

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
      if (isConnected()) {
        return session.get().getXAResource().setTransactionTimeout(seconds);
      }
      connectIfNecessary();
      try {
        return session.get().getXAResource().setTransactionTimeout(seconds);
      } finally {
        disconnect();
      }
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
      LOGGER.fine(() -> String.format(
          "Start work on behalf of a JMS XA [%s] transaction branch [%s] flags [%s] that run in JTA recovery processes!",
          config.getConnectionFactoryId(), xid.toString(), flags));

      if (isConnected()) {
        session.get().getXAResource().start(xid, flags);
        return;
      }
      connectIfNecessary();
      try {
        session.get().getXAResource().start(xid, flags);
      } finally {
        disconnect();
      }
    }

    JMSRecoveryXAResource connectIfNecessary() throws XAException {
      if (!isConnected()) {
        LOGGER.fine(() -> String.format("Connect to JMS XA server [%s] for JTA recovery processes.",
            config.getConnectionFactoryId()));
        try {
          try {
            connection.set(factory.createXAConnection());
          } catch (JMSSecurityException e) {
            LOGGER.log(Level.WARNING, e,
                () -> String.format(
                    "Connect to JMS XA server [%s] occured security exception, use another way!",
                    config.connectionFactoryId));
            connection.set(factory.createXAConnection(config.getUsername(), config.getPassword()));
          }
          session.set(connection.get().createXASession());
        } catch (JMSException e) {
          LOGGER.log(Level.SEVERE, e, () -> String
              .format("Can't not connect to JMS XA server [%s]!", config.connectionFactoryId));
          if (connection.get() != null) {
            try {
              connection.get().close();
            } catch (JMSException ex) {
              LOGGER.log(Level.WARNING, e,
                  () -> String.format("Release JMS XA [%s] connection occured exception!",
                      config.connectionFactoryId));
            }
          }
          throw new XAException(XAException.XAER_RMFAIL);
        }
      }
      return this;
    }

    void disconnect() {
      if (!isConnected()) {
        return;
      }
      LOGGER.fine(() -> String.format("Close JMS XA [%s] connection after JTA recovery processes.",
          config.getName()));
      try {
        connection.get().close();
      } catch (JMSException e) {
        // Noop!
      } finally {
        connection.set(null);
        session.set(null);
      }
    }

    boolean isConnected() {
      return connection.get() != null && session.get() != null;
    }
  }
}
