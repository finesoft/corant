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
package org.corant.suites.datasource.shared;

import static org.corant.shared.util.Empties.isNotEmpty;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.corant.config.PropertyInjector;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.jta.shared.TransactionConfig;
import org.corant.suites.jta.shared.TransactionIntegration;

/**
 * corant-suites-datasource-shared
 *
 * @author bingo 下午2:23:54
 *
 */
public class JDBCTransactionIntegration implements TransactionIntegration {

  private static final Logger logger = Logger.getLogger(JDBCTransactionIntegration.class.getName());

  static Map<String, XADataSource> dataSources = new ConcurrentHashMap<>();

  static XAConnection getXAConnection(XADataSource xads, DataSourceConfig cfg) throws SQLException {
    try {
      return xads.getXAConnection();
    } catch (SQLException nfe) {
      logger.log(Level.WARNING, nfe,
          () -> String.format("Connect to xa data source %s occured exception, use another way!",
              cfg.getName()));
      return xads.getXAConnection(cfg.getUsername(), cfg.getPassword());
    }
  }

  @Override
  public XAResource[] getRecoveryXAResources() {
    logger.info(() -> "Resolving JDBC XAResources for JTA recovery processes.");
    TransactionConfig txCfg = getConfig();
    List<XAResource> res = new ArrayList<>();
    Instance<AbstractDataSourceExtension> extensions =
        CDI.current().select(AbstractDataSourceExtension.class);
    if (!extensions.isUnsatisfied()) {
      extensions.forEach(et -> {
        et.getConfigManager().getAllWithNames().values().forEach(cfg -> {
          if (cfg.isJta() && cfg.isXa()) {
            if (!XADataSource.class.isAssignableFrom(cfg.getDriver())) {
              logger.warning(() -> String.format(
                  "The data source [%s] is XA, but driver class is not a XA data source, recovery connections are only available for XADataSource.",
                  cfg.getName()));
            } else {
              try {
                XADataSource xads = getXADataSource(cfg);
                if (txCfg.isAutoRecovery()) {
                  res.add(new JDBCRecoveryXAResource(xads, cfg));
                } else {
                  res.add(getXAResource(xads, cfg));
                }
                logger.info(() -> String.format(
                    "Added JDBC XA data source[%s] XAResource to JTA recovery processes.",
                    cfg.getName()));
              } catch (SecurityException | SQLException e) {
                throw new CorantRuntimeException(e, "Unable to instantiate javax.sql.XADataSource");
              }
            }
          }
        });
      });
    }

    return res.toArray(new XAResource[res.size()]);
  }

  XADataSource getXADataSource(DataSourceConfig cfg) {
    return dataSources.computeIfAbsent(cfg.getName(), k -> {
      XADataSource xads;
      try {
        xads =
            cfg.getDriver().asSubclass(XADataSource.class).getDeclaredConstructor().newInstance();
      } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
          | InvocationTargetException | NoSuchMethodException | SecurityException e) {
        throw new CorantRuntimeException(e);
      }
      if (isNotEmpty(cfg.getJdbcProperties())) {
        new PropertyInjector(xads).inject(cfg.getJdbcProperties());
      }
      return xads;
    });
  }

  XAResource getXAResource(XADataSource xads, DataSourceConfig cfg) throws SQLException {
    return getXAConnection(xads, cfg).getXAResource();
  }

  public static class JDBCRecoveryXAResource implements XAResource {
    static final Logger logger = Logger.getLogger(JDBCRecoveryXAResource.class.getName());
    final DataSourceConfig config;
    final XADataSource dataSource;
    final AtomicReference<XAConnection> connection = new AtomicReference<>();

    /**
     * @param dataSource
     * @param config
     */
    protected JDBCRecoveryXAResource(XADataSource dataSource, DataSourceConfig config) {
      super();
      this.dataSource = dataSource;
      this.config = config;
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
      try {
        getXAResource().commit(xid, onePhase);
      } finally {
        disconnect();
      }
    }

    @Override
    public void end(Xid xid, int flags) throws XAException {
      try {
        getXAResource().end(xid, flags);
      } finally {
        disconnect();
      }
    }

    @Override
    public void forget(Xid xid) throws XAException {
      try {
        getXAResource().forget(xid);
      } finally {
        disconnect();
      }
    }

    @Override
    public int getTransactionTimeout() throws XAException {
      try {
        return getXAResource().getTransactionTimeout();
      } finally {
        disconnect();
      }
    }

    @Override
    public boolean isSameRM(XAResource xares) throws XAException {
      try {
        return getXAResource().isSameRM(xares);
      } finally {
        disconnect();
      }
    }

    @Override
    public int prepare(Xid xid) throws XAException {
      try {
        return getXAResource().prepare(xid);
      } finally {
        disconnect();
      }
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
      try {
        return getXAResource().recover(flag);
      } finally {
        if (flag == XAResource.TMENDRSCAN) {
          disconnect();
        }
      }
    }

    @Override
    public void rollback(Xid xid) throws XAException {
      try {
        getXAResource().rollback(xid);
      } finally {
        disconnect();
      }
    }

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
      try {
        return getXAResource().setTransactionTimeout(seconds);
      } finally {
        disconnect();
      }
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
      try {
        getXAResource().start(xid, flags);
      } finally {
        disconnect();
      }
    }

    void disconnect() {
      if (!isConnected()) {
        return;
      }
      try {
        connection.get().close();
      } catch (SQLException e) {
        logger.log(Level.WARNING, e, () -> String
            .format("Can not release connection to xa data source %s", config.getName()));
      } finally {
        connection.set(null);
      }
    }

    XAResource getXAResource() throws XAException {
      if (!isConnected()) {
        try {
          connection.set(getXAConnection(dataSource, config));
        } catch (SQLException e) {
          logger.log(Level.SEVERE, e,
              () -> String.format("Can not connect to xa data source %s", config.getName()));
          throw new XAException(XAException.XAER_RMFAIL);
        }
      }
      try {
        return connection.get().getXAResource();
      } catch (SQLException e) {
        logger.log(Level.SEVERE, e, () -> String
            .format("Can not get xa resource from xa data source %s", config.getName()));
        throw new XAException(XAException.XAER_RMFAIL);
      }
    }

    boolean isConnected() {
      return connection.get() != null;
    }
  }
}
