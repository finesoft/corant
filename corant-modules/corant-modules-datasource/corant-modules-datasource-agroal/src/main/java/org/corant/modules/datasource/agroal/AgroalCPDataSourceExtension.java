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
package org.corant.modules.datasource.agroal;

import static org.corant.context.Beans.tryResolve;
import static org.corant.shared.normal.Names.applicationName;
import static org.corant.shared.util.Classes.defaultClassLoader;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.MBeans.registerToMBean;
import static org.corant.shared.util.Strings.defaultString;
import static org.corant.shared.util.Strings.isNotBlank;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import org.corant.modules.datasource.agroal.patch.MyNarayanaTransactionIntegration;
import org.corant.modules.datasource.shared.AbstractDataSourceExtension;
import org.corant.modules.datasource.shared.DataSourceConfig;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.util.MBeans;
import org.corant.shared.util.Services;
import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration.TransactionIsolation;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration.ConnectionValidator;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import io.agroal.narayana.NarayanaTransactionIntegration;

/**
 * corant-modules-datasource.agroal
 *
 * @author bingo 下午7:32:17
 *
 */
public class AgroalCPDataSourceExtension extends AbstractDataSourceExtension {

  protected Set<String> mbeanNames = new CopyOnWriteArraySet<>();

  protected void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery event) {
    if (event != null) {
      getConfigManager().getAllWithQualifiers().forEach((dsc, dsn) -> {
        if (dsc.isEnable()) {
          event.<DataSource>addBean().addQualifiers(dsn)
              .addTransitiveTypeClosure(AgroalDataSource.class).beanClass(AgroalDataSource.class)
              .scope(ApplicationScoped.class).produceWith(beans -> {
                try {
                  return produce(beans, dsc);
                } catch (NamingException | SQLException e) {
                  throw new CorantRuntimeException(e);
                }
              }).disposeWith((dataSource, beans) -> dataSource.close());
          if (isNotBlank(dsc.getName()) && dsc.isBindToJndi()) {
            registerJndi(dsc.getName(), dsn);
          }
        }
      });
    }
  }

  @Override
  protected void onBeforeShutdown(@Observes BeforeShutdown bs) {
    super.onBeforeShutdown(bs);
    try {
      if (isNotEmpty(mbeanNames)) {
        mbeanNames.forEach(MBeans::deregisterFromMBean);
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, e,
          () -> "Deregister agroal data source %s metrices from jmx error!");
    }
  }

  protected AgroalDataSource produce(Instance<Object> instance, DataSourceConfig cfg)
      throws SQLException, NamingException {

    AgroalDataSourceConfigurationSupplier cfgs = new AgroalDataSourceConfigurationSupplier();

    // transaction
    if (cfg.isXa() && !XADataSource.class.isAssignableFrom(cfg.getDriver())) {
      logger.warning(() -> String.format(
          "When using XA, the driver of data source [%s] should be a XADataSource.",
          cfg.getName()));
    }
    if (cfg.isJta() && !XADataSource.class.isAssignableFrom(cfg.getDriver())) {
      logger.info(() -> String.format(
          "Use resource local transaction for the data source [%s] if it used in transactional scenario.",
          cfg.getName()));
    }

    transactionIntegration(cfg, cfgs);

    // metrics
    cfgs.metricsEnabled(cfg.isEnableMetrics());

    // jdbc
    cfgs.connectionPoolConfiguration().connectionFactoryConfiguration()
        .jdbcUrl(cfg.getConnectionUrl());
    cfgs.connectionPoolConfiguration().connectionFactoryConfiguration()
        .connectionProviderClass(cfg.getDriver());
    cfgs.connectionPoolConfiguration().connectionFactoryConfiguration()
        .autoCommit(cfg.isAutoCommit());
    cfgs.connectionPoolConfiguration().connectionFactoryConfiguration()
        .trackJdbcResources(cfg.isEnableTrackJdbcResources());
    cfgs.connectionPoolConfiguration().connectionFactoryConfiguration()
        .jdbcTransactionIsolation(TransactionIsolation.fromLevel(cfg.getIsolationLevel()));

    // auth
    if (cfg.getUsername() != null) {
      cfgs.connectionPoolConfiguration().connectionFactoryConfiguration()
          .principal(new NamePrincipal(cfg.getUsername()));
    }
    if (cfg.getPassword() != null) {
      cfgs.connectionPoolConfiguration().connectionFactoryConfiguration()
          .credential(new SimplePassword(cfg.getPassword()));
    }

    cfgs.connectionPoolConfiguration().connectionFactoryConfiguration()
        .trackJdbcResources(cfg.isEnableTrackJdbcResources());

    if (isNotEmpty(cfg.getJdbcProperties())) {
      cfg.getJdbcProperties().forEach(
          cfgs.connectionPoolConfiguration().connectionFactoryConfiguration()::jdbcProperty);
    }

    if (isNotBlank(cfg.getInitialSql())) {
      cfgs.connectionPoolConfiguration().connectionFactoryConfiguration()
          .initialSql(cfg.getInitialSql());
    }

    if (!cfg.getLoginTimeout().equals(Duration.ZERO)) {
      cfgs.connectionPoolConfiguration().connectionFactoryConfiguration()
          .loginTimeout(cfg.getLoginTimeout());
    }

    // connection pool
    cfgs.connectionPoolConfiguration().acquisitionTimeout(cfg.getAcquisitionTimeout());
    cfgs.connectionPoolConfiguration().maxSize(cfg.getMaxSize());
    cfgs.connectionPoolConfiguration().minSize(cfg.getMinSize());
    cfgs.connectionPoolConfiguration().initialSize(cfg.getInitialSize());
    cfgs.connectionPoolConfiguration().leakTimeout(cfg.getLeakTimeout());
    cfgs.connectionPoolConfiguration().reapTimeout(cfg.getReapTimeout());
    cfgs.connectionPoolConfiguration().validationTimeout(cfg.getValidationTimeout());
    cfgs.connectionPoolConfiguration().idleValidationTimeout(cfg.getIdleValidationTimeout());
    cfgs.connectionPoolConfiguration().maxLifetime(cfg.getMaxLifetime());

    if (!cfg.getValidationTimeout().equals(Duration.ZERO)) {
      Optional<ConnectionValidator> validators =
          Services.findRequired(ConnectionValidator.class, defaultClassLoader());
      if (validators.isEmpty()) {
        cfgs.connectionPoolConfiguration().connectionValidator(
            AgroalConnectionPoolConfiguration.ConnectionValidator.defaultValidator());
      } else {
        cfgs.connectionPoolConfiguration().connectionValidator(validators.get());
      }
    }
    Services.selectRequired(AgroalCPDataSourceConfigurator.class, defaultClassLoader())
        .sorted(Sortable::reverseCompare).forEach(c -> c.config(cfg, cfgs));
    AgroalDataSource agroalDataSource = AgroalDataSource.from(cfgs);
    if (cfg.isEnableMetrics()) {
      registerMetricsMBean(cfg.getName());
    }
    return agroalDataSource;
  }

  protected void registerMetricsMBean(String name) {
    final String useName = defaultString(name, "unnamed");
    logger.info(() -> String.format("Register agroal data source %s metrices to jmx.", useName));
    final String mbeanName = applicationName().concat(":type=agroal,name=").concat(useName);
    registerToMBean(mbeanName, new AgroalCPDataSourceMetrics(name));
    mbeanNames.add(mbeanName);
  }

  protected void transactionIntegration(DataSourceConfig cfg,
      AgroalDataSourceConfigurationSupplier cfgs) {
    if (cfg.isJta() || cfg.isXa()) {
      TransactionManager tm = tryResolve(TransactionManager.class);
      TransactionSynchronizationRegistry tsr = tryResolve(TransactionSynchronizationRegistry.class);
      if (cfg.isEnableCustomTransactionIntegration()) {
        cfgs.connectionPoolConfiguration().transactionIntegration(
            new MyNarayanaTransactionIntegration(tm, tsr, null, false, null));
      } else {
        cfgs.connectionPoolConfiguration()
            .transactionIntegration(new NarayanaTransactionIntegration(tm, tsr, null, false, null));
      }
    }
  }

}
