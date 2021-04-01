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
package org.corant.modules.datasource.dbcp;

import static org.corant.shared.util.Classes.defaultClassLoader;
import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.isNotBlank;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import org.apache.commons.dbcp2.managed.BasicManagedDataSource;
import org.corant.modules.datasource.shared.AbstractDataSourceExtension;
import org.corant.modules.datasource.shared.DataSourceConfig;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Sortable;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.common.recoveryPropertyManager;
import com.arjuna.ats.arjuna.coordinator.TransactionReaper;
import com.arjuna.ats.arjuna.coordinator.TxControl;
import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.internal.arjuna.recovery.AtomicActionRecoveryModule;
import com.arjuna.ats.internal.arjuna.recovery.ExpiredTransactionStatusManagerScanner;
import com.arjuna.ats.internal.jta.recovery.arjunacore.JTANodeNameXAResourceOrphanFilter;
import com.arjuna.ats.internal.jta.recovery.arjunacore.JTATransactionLogXAResourceOrphanFilter;
import com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule;
import com.arjuna.ats.jdbc.TransactionalDriver;
import com.arjuna.ats.jta.common.jtaPropertyManager;

/**
 * corant-modules-datasource-dbcp
 *
 * Unfinish yet
 *
 * @author bingo 下午7:32:17
 *
 */
public class DBCPDataSourceExtension extends AbstractDataSourceExtension {

  private static final String DEFAULT_NODE_IDENTIFIER = "1";

  private static final List<String> DEFAULT_RECOVERY_MODULES =
      Arrays.asList(AtomicActionRecoveryModule.class.getName(), XARecoveryModule.class.getName()); // static?

  private static final List<String> DEFAULT_ORPHAN_FILTERS =
      Arrays.asList(JTATransactionLogXAResourceOrphanFilter.class.getName(),
          JTANodeNameXAResourceOrphanFilter.class.getName());

  private static final List<String> DEFAULT_EXPIRY_SCANNERS =
      Arrays.asList(ExpiredTransactionStatusManagerScanner.class.getName());

  void initNarayana() {
    initNodeIdentifier();
    initRecoveryModules();
    initOrphanFilters();
    initExpiryScanners();
    RecoveryManager.manager();
    TxControl.enable();
    TransactionReaper.instantiate();
  }

  /**
   *
   * @param event onAfterBeanDiscovery
   */
  void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery event) {
    initNarayana();
    if (event != null) {
      getConfigManager().getAllWithQualifiers().forEach((dsc, dsn) -> {
        event.<DataSource>addBean().addQualifiers(dsn)
            .addTransitiveTypeClosure(BasicManagedDataSource.class)
            .beanClass(BasicManagedDataSource.class).scope(ApplicationScoped.class)
            .produceWith(beans -> {
              try {
                return produce(beans, dsc);
              } catch (NamingException | SQLException e) {
                throw new CorantRuntimeException(e);
              }
            }).disposeWith((dataSource, beans) -> {
              try {
                dataSource.close();
              } catch (SQLException e) {
                throw new CorantRuntimeException(e);
              }
            });
        if (isNotBlank(dsc.getName()) && dsc.isBindToJndi()) {
          registerJndi(dsc.getName(), dsn);
        }
      });
    }
  }

  @PreDestroy
  void onPreDestroy() {
    logger.fine("Disabling Narayana");
    TransactionReaper.terminate(false);
    TxControl.disable(true);
    RecoveryManager.manager().terminate();
    Collections.list(DriverManager.getDrivers()).stream()
        .filter(d -> d instanceof TransactionalDriver).forEach(d -> {
          try {
            DriverManager.deregisterDriver(d);
          } catch (SQLException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
          }
        });
  }

  BasicManagedDataSource produce(Instance<Object> instance, DataSourceConfig cfg)
      throws SQLException, NamingException {
    BasicManagedDataSource ds = new BasicManagedDataSource();
    // ds.setAbandonedUsageTracking(usageTracking); // use DBCPDataSourceConfigurator
    // ds.setAbandonedLogWriter(logWriter); // use DBCPDataSourceConfigurator
    // ds.setAccessToUnderlyingConnectionAllowed(allow); // use DBCPDataSourceConfigurator
    ds.setAutoCommitOnReturn(cfg.isAutoCommit());
    // ds.setCacheState(cacheState);// use DBCPDataSourceConfigurator
    // ds.setConnectionFactoryClassName(cfn); // use DBCPDataSourceConfigurator
    if (isNotBlank(cfg.getInitialSql())) {
      ds.setConnectionInitSqls(listOf(cfg.getInitialSql()));
    }
    // ds.setConnectionProperties(connectionProperties);

    ds.setTransactionManager(instance.select(TransactionManager.class).get());
    ds.setDriverClassName(cfg.getDriver().getName());
    ds.setUrl(cfg.getConnectionUrl());
    ds.setUsername(cfg.getUsername());
    ds.setPassword(cfg.getPassword());
    ds.setInitialSize(cfg.getInitialSize());
    ds.setMaxTotal(cfg.getMaxSize());
    ds.setMinIdle(cfg.getMinSize());
    ds.setMaxConnLifetimeMillis(cfg.getMaxLifetime().get(ChronoUnit.MILLIS));
    ds.setMaxWaitMillis(cfg.getAcquisitionTimeout().get(ChronoUnit.MILLIS));
    ds.setMinEvictableIdleTimeMillis(cfg.getIdleValidationTimeout().get(ChronoUnit.MILLIS));

    streamOf(ServiceLoader.load(DBCPDataSourceConfigurator.class, defaultClassLoader()))
        .sorted(Sortable::compare).forEach(c -> c.config(cfg, ds));
    return ds;
  }

  /**
   * If expiry scanners were not set by property manager, then set defaults
   * {@link #DEFAULT_EXPIRY_SCANNERS}.
   */
  private void initExpiryScanners() {
    if (!recoveryPropertyManager.getRecoveryEnvironmentBean().getExpiryScannerClassNames()
        .isEmpty()) {
      return;
    }

    logger.fine(
        "Expiry scanners were not enabled. Enabling default scanners: " + DEFAULT_EXPIRY_SCANNERS);
    recoveryPropertyManager.getRecoveryEnvironmentBean()
        .setExpiryScannerClassNames(DEFAULT_EXPIRY_SCANNERS);
  }

  /**
   * If node identifier wasn't set by property manager, then set default
   * {@link #DEFAULT_NODE_IDENTIFIER}.
   */
  private void initNodeIdentifier() {
    if (arjPropertyManager.getCoreEnvironmentBean().getNodeIdentifier() == null) {
      logger.warning("Node identifier was not set. Setting it to the default value: "
          + DEFAULT_NODE_IDENTIFIER);
      try {
        arjPropertyManager.getCoreEnvironmentBean().setNodeIdentifier(DEFAULT_NODE_IDENTIFIER);
      } catch (CoreEnvironmentBeanException e) {
        logger.log(Level.WARNING, e.getMessage(), e);
      }
    }
    jtaPropertyManager.getJTAEnvironmentBean().setLastResourceOptimisationInterfaceClassName(
        "org.apache.commons.dbcp2.managed.LocalXAConnectionFactory$LocalXAResource");
    jtaPropertyManager.getJTAEnvironmentBean().setXaRecoveryNodes(
        Collections.singletonList(arjPropertyManager.getCoreEnvironmentBean().getNodeIdentifier()));
  }

  /**
   * If orphan filters were not set by property manager, then set defaults
   * {@link #DEFAULT_ORPHAN_FILTERS}.
   */
  private void initOrphanFilters() {
    if (!jtaPropertyManager.getJTAEnvironmentBean().getXaResourceOrphanFilterClassNames()
        .isEmpty()) {
      return;
    }

    logger.fine(
        "Orphan filters were not enabled. Enabling default filters: " + DEFAULT_ORPHAN_FILTERS);
    jtaPropertyManager.getJTAEnvironmentBean()
        .setXaResourceOrphanFilterClassNames(DEFAULT_ORPHAN_FILTERS);
  }

  /**
   * If recovery modules were not set by property manager, then set defaults
   * {@link #DEFAULT_RECOVERY_MODULES}.
   */
  private void initRecoveryModules() {
    if (!recoveryPropertyManager.getRecoveryEnvironmentBean().getRecoveryModuleClassNames()
        .isEmpty()) {
      return;
    }
    logger.fine(
        "Recovery modules were not enabled. Enabling default modules: " + DEFAULT_RECOVERY_MODULES);
    recoveryPropertyManager.getRecoveryEnvironmentBean()
        .setRecoveryModuleClassNames(DEFAULT_RECOVERY_MODULES);
  }

}
