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
package org.corant.suites.datasource.agroal;

import static org.corant.kernel.util.Instances.resolve;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.ClassUtils.tryAsClass;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.sql.SQLException;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.datasource.shared.AbstractDataSourceExtension;
import org.corant.suites.datasource.shared.DataSourceConfig;
import org.jboss.tm.XAResourceRecoveryRegistry;
import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import io.agroal.narayana.NarayanaTransactionIntegration;

/**
 * corant-suites-datasource.agroal
 *
 * @author bingo 下午7:32:17
 *
 */
public class AgroalCPDataSourceExtension extends AbstractDataSourceExtension {

  /**
   *
   * @param event onAfterBeanDiscovery
   */
  void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery event) {
    if (event != null) {
      getConfigManager().getAllWithQualifiers().forEach((dsc, dsn) -> {
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
      });
    }
  }

  AgroalDataSource produce(Instance<Object> instance, DataSourceConfig cfg)
      throws SQLException, NamingException {

    AgroalDataSourceConfigurationSupplier cfgs = new AgroalDataSourceConfigurationSupplier();

    // transaction
    if (cfg.isXa()) {
      shouldBeTrue(XADataSource.class.isAssignableFrom(cfg.getDriver()));
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

    // auth
    if (cfg.getUsername() != null) {
      cfgs.connectionPoolConfiguration().connectionFactoryConfiguration()
          .principal(new NamePrincipal(cfg.getUsername()));
    }
    if (cfg.getPassword() != null) {
      cfgs.connectionPoolConfiguration().connectionFactoryConfiguration()
          .credential(new SimplePassword(cfg.getPassword()));
    }

    // connection pool
    cfgs.connectionPoolConfiguration().acquisitionTimeout(cfg.getAcquisitionTimeout());
    cfgs.connectionPoolConfiguration().maxSize(cfg.getMaxSize());
    cfgs.connectionPoolConfiguration().minSize(cfg.getMinSize());
    cfgs.connectionPoolConfiguration().initialSize(cfg.getInitialSize());
    cfgs.connectionPoolConfiguration().leakTimeout(cfg.getLeakTimeout());
    cfgs.connectionPoolConfiguration().reapTimeout(cfg.getReapTimeout());
    cfgs.connectionPoolConfiguration().validationTimeout(cfg.getValidationTimeout());
    if (cfg.isValidateConnection()) {
      cfgs.connectionPoolConfiguration().connectionValidator(
          AgroalConnectionPoolConfiguration.ConnectionValidator.defaultValidator());
    }
    return AgroalDataSource.from(cfgs);
  }

  void transactionIntegration(DataSourceConfig cfg, AgroalDataSourceConfigurationSupplier cfgs) {
    if (cfg.isJta() || cfg.isXa()) {
      TransactionManager tm = resolve(TransactionManager.class).orElse(null);
      TransactionSynchronizationRegistry tsr =
          resolve(TransactionSynchronizationRegistry.class).orElse(null);
      XAResourceRecoveryRegistry xar = null;
      if (tryAsClass("org.corant.suites.jta.narayana.NarayanaExtension") != null) {
        xar =
            resolve(XAResourceRecoveryRegistry.class, NamedLiteral.of("narayana-jta")).orElse(null);
      } else {
        xar = resolve(XAResourceRecoveryRegistry.class).orElse(null);
      }
      if (xar != null) {
        cfgs.connectionPoolConfiguration().transactionIntegration(
            new NarayanaTransactionIntegration(tm, tsr, null, cfg.isConnectable(), xar));
      } else {
        cfgs.connectionPoolConfiguration().transactionIntegration(
            new NarayanaTransactionIntegration(tm, tsr, null, cfg.isConnectable()));
      }
    }
  }

}
