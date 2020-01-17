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

  @Override
  public XAResource[] getRecoveryXAResources() {
    LOGGER.fine(() -> "Searching JMS XAResources for JTA recovery processes.");
    if (!CDIs.isEnabled()) {
      LOGGER.warning(
          () -> "Current CDI container can't access, so can't find any XAResource for JTA recovery processes.");
      return new XAResource[0];
    }
    Instance<AbstractJMSExtension> extensions = CDI.current().select(AbstractJMSExtension.class);
    List<XAResource> resources = new ArrayList<>();
    if (!extensions.isUnsatisfied()) {
      extensions.forEach(et -> et.getConfigManager().getAllWithNames().forEach((k, v) -> {
        if (v.isXa() && v.isEnable()) {
          // Each time the connection is reestablished and released it after use
          resources.add(new JMSRecoveryXAResource(v));
          // findNamed(XAConnectionFactory.class, k)
          // .ifPresent(xacf -> resources.add(xacf.createXAContext().getXAResource()));
          LOGGER
              .fine(() -> String.format("Found JMS[%s] XAResource for JTA recovery processes.", k));
        }
      }));
    }
    if (isEmpty(resources)) {
      LOGGER.fine(() -> "JMS XAResources for JTA recovery processes not found.");
    }
    return isNotEmpty(resources) ? resources.toArray(new XAResource[resources.size()])
        : new XAResource[0];
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
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
      connectIfNecessary();
      try {
        session.get().getXAResource().commit(xid, onePhase);
      } finally {
        disconnect();
        LOGGER.fine(() -> String.format(
            "Committed the JMS XA %s transaction [%s] (onePhase:%s) that run in JTA recovery processes!",
            config.getConnectionFactoryId(), xid.toString(), onePhase));
      }
    }

    @Override
    public void end(Xid xid, int flags) throws XAException {
      connectIfNecessary();
      try {
        session.get().getXAResource().end(xid, flags);
      } finally {
        disconnect();
        LOGGER.fine(() -> String.format(
            "Ended the work performed on behalf of the JMS XA %s transaction branch [%s] flags [%s] that run in JTA recovery processes!",
            config.getConnectionFactoryId(), xid.toString(), flags));
      }
    }

    @Override
    public void forget(Xid xid) throws XAException {
      connectIfNecessary();
      try {
        session.get().getXAResource().forget(xid);
      } finally {
        disconnect();
        LOGGER.fine(() -> String.format(
            "Forgot about the JMS XA %s heuristicallycompleted transaction branch [%s] that run in JTA recovery processes!",
            config.getConnectionFactoryId(), xid.toString()));
      }
    }

    @Override
    public int getTransactionTimeout() throws XAException {
      connectIfNecessary();
      try {
        return session.get().getXAResource().getTransactionTimeout();
      } finally {
        disconnect();
      }
    }

    @Override
    public boolean isSameRM(XAResource xares) throws XAException {
      connectIfNecessary();
      try {
        return session.get().getXAResource().isSameRM(xares);
      } finally {
        disconnect();
      }
    }

    @Override
    public int prepare(Xid xid) throws XAException {
      connectIfNecessary();
      try {
        return session.get().getXAResource().prepare(xid);
      } finally {
        disconnect();
      }
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
      connectIfNecessary();
      Xid[] xids = new Xid[0];
      try {
        xids = session.get().getXAResource().recover(flag);
      } finally {
        if (flag == XAResource.TMENDRSCAN) {
          disconnect();
          final Xid[] useXids = xids;
          if (useXids != null && useXids.length > 0) {
            LOGGER.fine(() -> String.format(
                "Obtained prepared JMS XA %s transaction branches: [%s] for JTA recovery processes.",
                config.getConnectionFactoryId(), String.join(", ", asStrings((Object[]) useXids))));
          } else {
            LOGGER.fine(() -> String.format(
                "Prepared JMS XA %s transaction branches for JTA recovery processes not found.",
                config.getConnectionFactoryId()));
          }
        }
      }
      return xids;
    }

    @Override
    public void rollback(Xid xid) throws XAException {
      connectIfNecessary();
      try {
        session.get().getXAResource().rollback(xid);
      } finally {
        disconnect();
        LOGGER.fine(() -> String.format(
            "Rolled back work done on behalfof the JMS XA %s transaction branch [%s] that run in JTA recovery processes!",
            config.getConnectionFactoryId(), xid.toString()));
      }
    }

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
      connectIfNecessary();
      try {
        return session.get().getXAResource().setTransactionTimeout(seconds);
      } finally {
        disconnect();
      }
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
      connectIfNecessary();
      try {
        session.get().getXAResource().start(xid, flags);
      } finally {
        disconnect();
        LOGGER.fine(() -> String.format(
            "Started work on behalf of a JMS XA %s transaction branch [%s] flags [%s] that run in JTA recovery processes!",
            config.getConnectionFactoryId(), xid.toString(), flags));
      }
    }

    void connectIfNecessary() throws XAException {
      if (!isConnected()) {
        try {
          try {
            connection.set(factory.createXAConnection());
          } catch (JMSSecurityException e) {
            LOGGER.log(Level.WARNING, e,
                () -> String.format(
                    "Connect to jms server %s occured security exception, use another way!",
                    config.connectionFactoryId));
            connection.set(factory.createXAConnection(config.getUsername(), config.getPassword()));
          }
          session.set(connection.get().createXASession());
        } catch (JMSException e) {
          LOGGER.log(Level.SEVERE, e, () -> String.format("Can't not connect to jms server %s!",
              config.connectionFactoryId));
          if (connection.get() != null) {
            try {
              connection.get().close();
            } catch (JMSException ex) {
              LOGGER.log(Level.WARNING, e,
                  () -> String.format("Release jms %s connection occured exception!",
                      config.connectionFactoryId));
            }
          }
          throw new XAException(XAException.XAER_RMFAIL);
        }
      }
    }

    void disconnect() {
      if (!isConnected()) {
        return;
      }
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
