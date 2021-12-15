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
package org.corant.modules.datasource.shared;

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Objects.asStrings;
import static org.corant.shared.util.Strings.isNotBlank;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.corant.context.CDIs;
import org.corant.modules.jta.shared.TransactionIntegration;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-datasource-shared
 *
 * @author bingo 下午2:23:54
 *
 */
public class JDBCTransactionIntegration implements TransactionIntegration {

  public static final XAResource[] EMPTY_XA_RESOURCES = {};
  static Map<DataSourceConfig, JDBCRecoveryXAResource> recoveryXAResources =
      new ConcurrentHashMap<>();// static

  static XAConnection getXAConnection(XADataSource xads, DataSourceConfig cfg) throws SQLException {
    try {
      return xads.getXAConnection();
    } catch (SQLException nfe) {
      LOGGER.log(Level.WARNING, nfe,
          () -> String.format("Connect to xa data source [%s] occured exception, use another way!",
              cfg.getName()));
      return xads.getXAConnection(cfg.getUsername(), cfg.getPassword());
    }
  }

  @Override
  public void destroy() {
    recoveryXAResources.clear();
  }

  @Override
  public XAResource[] getRecoveryXAResources() {
    LOGGER.fine(() -> "Searching JDBC XAResources for JTA recovery processes.");
    if (!CDIs.isEnabled()) {
      LOGGER.warning(
          () -> "Current CDI container can't access, so can't find any XAResource for JTA recovery processes.");
      return EMPTY_XA_RESOURCES;
    }
    List<XAResource> resources = new ArrayList<>();
    Instance<AbstractDataSourceExtension> extensions =
        CDI.current().select(AbstractDataSourceExtension.class);
    if (!extensions.isUnsatisfied()) {
      extensions.forEach(et -> et.getConfigManager().getAllWithNames().values().forEach(cfg -> {
        if (cfg.isJta() && cfg.isXa() && cfg.isValid()) {
          if (!XADataSource.class.isAssignableFrom(cfg.getDriver())) {
            LOGGER.warning(() -> String.format(
                "The data source [%s] is XA, but driver class is not a XA data source, recovery connections are only available for XADataSource.",
                cfg.getName()));
          } else {
            resolveRecoveryXAResource(cfg).ifPresent(resources::add);
          }
        }
      }));
    }
    if (isEmpty(resources)) {
      LOGGER.fine(() -> "JDBC XAResources for JTA recovery processes not found.");
    }
    return resources.toArray(new XAResource[resources.size()]);
  }

  XADataSource getXADataSource(DataSourceConfig cfg) {
    XADataSource xads;
    try {
      xads = cfg.getDriver().asSubclass(XADataSource.class).getDeclaredConstructor().newInstance();
      PropertyInjector pi = new PropertyInjector(true, xads);
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
  }

  XAResource getXAResource(XADataSource xads, DataSourceConfig cfg) throws SQLException {
    return getXAConnection(xads, cfg).getXAResource();
  }

  Optional<XAResource> resolveRecoveryXAResource(DataSourceConfig config) {
    XAResource res = null;
    try {
      res = recoveryXAResources.computeIfAbsent(config, k -> {
        try {
          XADataSource xads = getXADataSource(k);
          return new JDBCRecoveryXAResource(xads, k);
        } catch (SecurityException e) {
          throw new CorantRuntimeException(e, "Unable to instantiate javax.sql.XADataSource");
        }
      }).connectIfNecessary();
    } catch (XAException e) {
      LOGGER.log(Level.WARNING, e,
          () -> String.format(
              "Can not resolve JDBCRecoveryXAResource from JDBC XA data source [%s].",
              config.getName()));
    }
    return Optional.ofNullable(res);
  }

  /**
   * corant-modules-datasource-shared
   *
   * FIXME: Make XAResource {@link Serializable}, may use data source config ({@link #config}) to
   * rebuild the {@link #dataSource}, and the {@link #connection} and the {@link #xaresource}}. some
   * issues see ARJUNA016037
   *
   *
   * @author bingo 下午3:17:50
   *
   */
  public static class JDBCRecoveryXAResource implements XAResource {
    final DataSourceConfig config;
    final XADataSource dataSource;
    final AtomicReference<XAConnection> connection = new AtomicReference<>();
    final AtomicReference<XAResource> xaresource = new AtomicReference<>();

    /**
     * @param dataSource
     * @param config
     */
    protected JDBCRecoveryXAResource(XADataSource dataSource, DataSourceConfig config) {
      this.dataSource = dataSource;
      this.config = config;
      LOGGER.fine(() -> String.format(
          "Found JDBC XA data source[%s] XAResource for JTA recovery processes.",
          config.getName()));
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
      LOGGER.fine(() -> String.format(
          "Commit the JDBC XA [%s] transaction [%s] (onePhase:[%s]) that run in JTA recovery processes!",
          config.getName(), xid.toString(), onePhase));

      if (isConnected()) {
        xaresource.get().commit(xid, onePhase);
        return;
      }
      try {
        connectIfNecessary();
        xaresource.get().commit(xid, onePhase);
      } finally {
        disconnect();
      }
    }

    @Override
    public void end(Xid xid, int flags) throws XAException {
      LOGGER.fine(() -> String.format(
          "Ended the work performed on behalf of the JDBC XA [%s] transaction branch [%s] flags [%s] that run in JTA recovery processes!",
          config.getName(), xid.toString(), flags));
      if (isConnected()) {
        xaresource.get().end(xid, flags);
        return;
      }
      try {
        connectIfNecessary();
        xaresource.get().end(xid, flags);
      } finally {
        disconnect();
      }
    }

    @Override
    public void forget(Xid xid) throws XAException {
      LOGGER.fine(() -> String.format(
          "Forget about the the JDBC XA [%s] heuristicallycompleted transaction branch [%s] that run in JTA recovery processes!",
          config.getName(), xid.toString()));
      if (isConnected()) {
        xaresource.get().forget(xid);
        return;
      }
      try {
        connectIfNecessary();
        xaresource.get().forget(xid);
      } finally {
        disconnect();
      }
    }

    @Override
    public int getTransactionTimeout() throws XAException {
      if (isConnected()) {
        return xaresource.get().getTransactionTimeout();
      }
      try {
        connectIfNecessary();
        return xaresource.get().getTransactionTimeout();
      } finally {
        disconnect();
      }
    }

    @Override
    public boolean isSameRM(XAResource xares) throws XAException {
      if (isConnected()) {
        return xaresource.get().isSameRM(xares);
      }
      try {
        connectIfNecessary();
        return xaresource.get().isSameRM(xares);
      } finally {
        disconnect();
      }
    }

    @Override
    public int prepare(Xid xid) throws XAException {
      if (isConnected()) {
        return xaresource.get().prepare(xid);
      }
      try {
        connectIfNecessary();
        return xaresource.get().prepare(xid);
      } finally {
        disconnect();
      }
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
      Xid[] xids = {};
      try {
        if (isConnected()) {
          xids = xaresource.get().recover(flag);
        } else {
          connectIfNecessary();
          xids = xaresource.get().recover(flag);
          disconnect();
        }
      } finally {
        final Xid[] useXids = xids;
        if (useXids != null && useXids.length > 0) {
          LOGGER.fine(() -> String.format(
              "Found prepared JDBC XA [%s] transaction branches: [%s] for JTA recovery processes.",
              config.getName(), String.join(", ", asStrings((Object[]) useXids))));
        } else {
          LOGGER.fine(() -> String.format(
              "Prepared JDBC XA [%s] transaction branches for JTA recovery processes not found.",
              config.getName()));
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
          "Roll back work done on behalfof the the JDBC XA [%s] transaction branch [%s] that run in JTA recovery processes!",
          config.getName(), xid.toString()));
      if (isConnected()) {
        xaresource.get().rollback(xid);
        return;
      }
      try {
        connectIfNecessary();
        xaresource.get().rollback(xid);
      } finally {
        disconnect();
      }
    }

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
      if (isConnected()) {
        return xaresource.get().setTransactionTimeout(seconds);
      }
      try {
        connectIfNecessary();
        return xaresource.get().setTransactionTimeout(seconds);
      } finally {
        disconnect();
      }
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
      LOGGER.fine(() -> String.format(
          "Start work on behalf of a transaction branch [%s] flags [%s] that run in JTA recovery processes!",
          xid.toString(), flags));
      if (isConnected()) {
        xaresource.get().start(xid, flags);
        return;
      }
      try {
        connectIfNecessary();
        xaresource.get().start(xid, flags);
      } finally {
        disconnect();
      }
    }

    JDBCRecoveryXAResource connectIfNecessary() throws XAException {
      if (!isConnected()) {
        LOGGER.fine(() -> String.format(
            "Connect to JDBC XA data source [%s] for JTA recovery processes.", config.getName()));
        try {
          if (connection.get() == null) {
            connection.set(getXAConnection(dataSource, config));
          }
        } catch (SQLException e) {
          LOGGER.log(Level.SEVERE, e, () -> String
              .format("Can not connect to JDBC XA data source [%s].", config.getName()));
          throw new XAException(XAException.XAER_RMFAIL);
        }
        try {
          xaresource.set(connection.get().getXAResource());
        } catch (SQLException e) {
          LOGGER.log(Level.SEVERE, e, () -> String
              .format("Can not get xa resource from JDBC XA data source [%s].", config.getName()));
          throw new XAException(XAException.XAER_RMFAIL);
        }
      }
      return this;
    }

    void disconnect() {
      if (!isConnected()) {
        return;
      }
      LOGGER.fine(() -> String.format(
          "Close JDBC XA data source [%s] connection after JTA recovery processes.",
          config.getName()));
      try {
        connection.get().close();
      } catch (SQLException e) {
        LOGGER.log(Level.WARNING, e, () -> String
            .format("Can not release connection to JDBC XA data source [%s].", config.getName()));
      } finally {
        connection.set(null);
        xaresource.set(null);
      }
    }

    boolean isConnected() {
      return connection.get() != null && xaresource.get() != null;
    }
  }

}
