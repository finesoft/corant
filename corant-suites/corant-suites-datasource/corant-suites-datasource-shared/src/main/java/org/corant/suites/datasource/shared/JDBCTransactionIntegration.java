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

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.ObjectUtils.asStrings;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.corant.config.PropertyInjector;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.cdi.CDIs;
import org.corant.suites.jta.shared.TransactionIntegration;

/**
 * corant-suites-datasource-shared
 *
 * @author bingo 下午2:23:54
 *
 */
public class JDBCTransactionIntegration implements TransactionIntegration {

  static Map<String, XADataSource> dataSources = new ConcurrentHashMap<>();

  static XAConnection getXAConnection(XADataSource xads, DataSourceConfig cfg) throws SQLException {
    try {
      return xads.getXAConnection();
    } catch (SQLException nfe) {
      LOGGER.log(Level.WARNING, nfe,
          () -> String.format("Connect to xa data source %s occured exception, use another way!",
              cfg.getName()));
      return xads.getXAConnection(cfg.getUsername(), cfg.getPassword());
    }
  }

  @Override
  public XAResource[] getRecoveryXAResources() {
    LOGGER.fine(() -> "Searching JDBC XAResources for JTA recovery processes.");
    if (!CDIs.isEnabled()) {
      LOGGER.warning(
          () -> "Current CDI container can't access, so can't find any XAResource for JTA recovery processes.");
      return new XAResource[0];
    }
    List<XAResource> res = new ArrayList<>();
    Instance<AbstractDataSourceExtension> extensions =
        CDI.current().select(AbstractDataSourceExtension.class);
    if (!extensions.isUnsatisfied()) {
      extensions.forEach(et -> et.getConfigManager().getAllWithNames().values().forEach(cfg -> {
        if (cfg.isJta() && cfg.isXa()) {
          if (!XADataSource.class.isAssignableFrom(cfg.getDriver())) {
            LOGGER.warning(() -> String.format(
                "The data source [%s] is XA, but driver class is not a XA data source, recovery connections are only available for XADataSource.",
                cfg.getName()));
          } else {
            try {
              XADataSource xads = getXADataSource(cfg);
              // Each time the connection is reestablished and released it after use
              res.add(new JDBCRecoveryXAResource(xads, cfg));
              // res.add(getXAResource(xads, cfg));
              LOGGER.fine(() -> String.format(
                  "Found JDBC XA data source[%s] XAResource for JTA recovery processes.",
                  cfg.getName()));
            } catch (SecurityException e) {
              throw new CorantRuntimeException(e, "Unable to instantiate javax.sql.XADataSource");
            }
          }
        }
      }));
    }
    if (isEmpty(res)) {
      LOGGER.fine(() -> "JDBC XAResources for JTA recovery processes not found.");
    }
    return res.toArray(new XAResource[res.size()]);
  }

  XADataSource getXADataSource(DataSourceConfig cfg) {
    return dataSources.computeIfAbsent(cfg.getName(), k -> {
      XADataSource xads;
      try {
        xads =
            cfg.getDriver().asSubclass(XADataSource.class).getDeclaredConstructor().newInstance();
        PropertyInjector pi = new PropertyInjector(xads);
        if (isNotEmpty(cfg.getJdbcProperties())) {
          pi.inject(cfg.getJdbcProperties());
        }
        if (isNotBlank(cfg.getUsername()) && isNotBlank(cfg.getPassword())) {
          pi.inject("user", cfg.getUsername());
          pi.inject("password", cfg.getPassword());
        }
        if (isNotBlank(cfg.getConnectionUrl())) {
          pi.inject("url", cfg.getConnectionUrl());
        }
      } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
          | InvocationTargetException | NoSuchMethodException | SecurityException e) {
        throw new CorantRuntimeException(e);
      }
      return xads;
    });
  }

  XAResource getXAResource(XADataSource xads, DataSourceConfig cfg) throws SQLException {
    return getXAConnection(xads, cfg).getXAResource();
  }

  public static class JDBCRecoveryXAResource implements XAResource {
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
        LOGGER.fine(() -> String.format(
            "Committed the transaction [%s] (onePhase:%s) that run in JTA recovery processes!",
            xid.toString(), onePhase));
      }
    }

    @Override
    public void end(Xid xid, int flags) throws XAException {
      try {
        getXAResource().end(xid, flags);
      } finally {
        disconnect();
        LOGGER.fine(() -> String.format(
            "Ended the work performed on behalf of a transaction branch [%s] flags [%s] that run in JTA recovery processes!",
            xid.toString(), flags));
      }
    }

    @Override
    public void forget(Xid xid) throws XAException {
      try {
        getXAResource().forget(xid);
      } finally {
        disconnect();
        LOGGER.fine(() -> String.format(
            "Forgot about a heuristicallycompleted transaction branch [%s] that run in JTA recovery processes!",
            xid.toString()));
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
      Xid[] xids = new Xid[0];
      try {
        xids = getXAResource().recover(flag);
      } finally {
        if (flag == XAResource.TMENDRSCAN) {
          disconnect();
          final Xid[] useXids = xids;
          if (useXids != null && useXids.length > 0) {
            LOGGER.fine(() -> String.format(
                "Obtained prepared JDBC XA %s transaction branches: [%s] for JTA recovery processes.",
                config.getName(), String.join(", ", asStrings((Object[]) useXids))));
          } else {
            LOGGER.fine(() -> String.format(
                "Prepared JDBC XA %s transaction branches for JTA recovery processes not found.",
                config.getName()));
          }
        }
      }
      return xids;
    }

    @Override
    public void rollback(Xid xid) throws XAException {
      try {
        getXAResource().rollback(xid);
      } finally {
        disconnect();
        LOGGER.fine(() -> String.format(
            "Rolled back work done on behalfof a transaction branch [%s] that run in JTA recovery processes!",
            xid.toString()));
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
        LOGGER.fine(() -> String.format(
            "Started work on behalf of a transaction branch [%s] flags [%s] that run in JTA recovery processes!",
            xid.toString(), flags));
      }
    }

    void disconnect() {
      if (!isConnected()) {
        return;
      }
      try {
        connection.get().close();
      } catch (SQLException e) {
        LOGGER.log(Level.WARNING, e, () -> String
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
          LOGGER.log(Level.SEVERE, e,
              () -> String.format("Can not connect to xa data source %s", config.getName()));
          throw new XAException(XAException.XAER_RMFAIL);
        }
      }
      try {
        return connection.get().getXAResource();
      } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, e, () -> String
            .format("Can not get xa resource from xa data source %s", config.getName()));
        throw new XAException(XAException.XAER_RMFAIL);
      }
    }

    boolean isConnected() {
      return connection.get() != null;
    }
  }
}
