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

import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.suites.cdi.Instances.findNamed;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.corant.suites.jta.shared.TransactionConfig;
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
    TransactionConfig txCfg = getConfig();
    Instance<AbstractJMSExtension> extensions = CDI.current().select(AbstractJMSExtension.class);
    List<XAResource> resources = new ArrayList<>();
    if (!extensions.isUnsatisfied()) {
      extensions.forEach(et -> et.getConfigManager().getAllWithNames().forEach((k, v) -> {
        if (v.isXa() && v.isEnable()) {
          LOGGER.fine(() -> "Resolving JMS XAResources for JTA recovery processes.");
          if (txCfg.isAutoRecovery()) {
            resources.add(new JMSRecoveryXAResource(v));
          } else {
            findNamed(XAConnectionFactory.class, k)
                .ifPresent(xacf -> resources.add(xacf.createXAContext().getXAResource()));
          }
          LOGGER
              .fine(() -> String.format("Added JMS[%s] XAResource to JTA recovery processes.", k));
        }
      }));
    }
    return isNotEmpty(resources) ? resources.toArray(new XAResource[resources.size()])
        : new XAResource[0];
  }

  public static class JMSRecoveryXAResource implements XAResource {
    static final Logger logger = Logger.getLogger(JMSRecoveryXAResource.class.getName());
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
      }
    }

    @Override
    public void end(Xid xid, int flags) throws XAException {
      connectIfNecessary();
      try {
        session.get().getXAResource().end(xid, flags);
      } finally {
        disconnect();
      }
    }

    @Override
    public void forget(Xid xid) throws XAException {
      connectIfNecessary();
      try {
        session.get().getXAResource().forget(xid);
      } finally {
        disconnect();
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
      try {
        return session.get().getXAResource().recover(flag);
      } finally {
        if (flag == XAResource.TMENDRSCAN) {
          disconnect();
        }
      }
    }

    @Override
    public void rollback(Xid xid) throws XAException {
      connectIfNecessary();
      try {
        session.get().getXAResource().rollback(xid);
      } finally {
        disconnect();
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
      }
    }

    void connectIfNecessary() throws XAException {
      if (!isConnected()) {
        try {
          try {
            connection.set(factory.createXAConnection());
          } catch (JMSSecurityException e) {
            logger.log(Level.WARNING, e,
                () -> String.format(
                    "Connect to jms server %s occured security exception, use another way!",
                    config.connectionFactoryId));
            connection.set(factory.createXAConnection(config.getUsername(), config.getPassword()));
          }
          session.set(connection.get().createXASession());
        } catch (JMSException e) {
          logger.log(Level.SEVERE, e, () -> String.format("Can't not connect to jms server %s!",
              config.connectionFactoryId));
          if (connection.get() != null) {
            try {
              connection.get().close();
            } catch (JMSException ex) {
              logger.log(Level.WARNING, e,
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
