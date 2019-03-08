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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.ClassUtils.tryAsClass;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.sql.SQLException;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import org.corant.kernel.util.Cdis;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.datasource.shared.AbstractDataSourceExtension;
import org.corant.suites.datasource.shared.DataSourceConfig;
import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import io.agroal.api.transaction.TransactionIntegration;
import io.agroal.narayana.NarayanaTransactionIntegration;

/**
 * corant-suites-datasource.agroal
 *
 * @author bingo 下午7:32:17
 *
 */
public class AgroalCPDataSourceExtension extends AbstractDataSourceExtension {

  /**
   * When go into this method, the data source configuration have been collected,
   * {@link #onBeforeBeanDiscovery(javax.enterprise.inject.spi.BeforeBeanDiscovery)}. The jndi
   * naming context has been initialized,
   * {@linkplain org.corant.suites.jndi.InitialContextExtension.beforeBeanDiscovery}
   *
   * @param event afterBeanDiscovery
   */
  void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery event) {
    if (event != null) {
      getDataSourceConfigs().forEach((dsn, dsc) -> {
        event.<DataSource>addBean().addQualifier(Cdis.resolveNamed(dsn))
            .addQualifier(Default.Literal.INSTANCE).addTransitiveTypeClosure(AgroalDataSource.class)
            .beanClass(AgroalDataSource.class).scope(ApplicationScoped.class).produceWith(beans -> {
              try {
                return produce(beans, dsc);
              } catch (NamingException | SQLException e) {
                throw new CorantRuntimeException(e);
              }
            }).disposeWith((dataSource, beans) -> dataSource.close());
      });
    }
  }

  AgroalDataSource produce(Instance<Object> instance, DataSourceConfig cfg)
      throws SQLException, NamingException {
    TransactionManager tm = instance.select(TransactionManager.class).isResolvable()
        ? instance.select(TransactionManager.class).get()
        : null;
    TransactionSynchronizationRegistry tsr =
        instance.select(TransactionSynchronizationRegistry.class).isResolvable()
            ? instance.select(TransactionSynchronizationRegistry.class).get()
            : null;
    if (cfg.isXa()) {
      shouldBeTrue(XADataSource.class.isAssignableFrom(cfg.getDriver()));
    }
    AgroalDataSourceConfigurationSupplier cfgs = new AgroalDataSourceConfigurationSupplier();
    cfgs.metricsEnabled(cfg.isEnableMetrics());
    cfgs.connectionPoolConfiguration().connectionFactoryConfiguration()
        .jdbcUrl(cfg.getConnectionUrl());
    cfgs.connectionPoolConfiguration().connectionFactoryConfiguration()
        .connectionProviderClass(cfg.getDriver());
    if ((cfg.isJta() || cfg.isXa())
        && tryAsClass("org.corant.suites.jta.narayana.NarayanaTransactionServices") != null) {
      TransactionIntegration txIntegration =
          new NarayanaTransactionIntegration(tm, tsr, null, cfg.isConnectable());
      cfgs.connectionPoolConfiguration().transactionIntegration(txIntegration);
    }
    if (cfg.getUsername() != null) {
      cfgs.connectionPoolConfiguration().connectionFactoryConfiguration()
          .principal(new NamePrincipal(cfg.getUsername()));
    }
    if (cfg.getPassword() != null) {
      cfgs.connectionPoolConfiguration().connectionFactoryConfiguration()
          .credential(new SimplePassword(cfg.getPassword()));
    }
    // Configure pool
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
    AgroalDataSource datasource = AgroalDataSource.from(cfgs);
    registerDataSource(cfg.getName(), datasource);
    if (instance.select(InitialContext.class).isResolvable() && isNotBlank(cfg.getName())) {
      InitialContext jndi = instance.select(InitialContext.class).get();
      registerJndi(jndi, cfg.getName(), datasource);
    }
    return datasource;
  }

}
