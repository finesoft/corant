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
package org.corant.modules.datasource.hikari;

import static org.corant.shared.util.Assertions.shouldBeFalse;
import static org.corant.shared.util.Classes.defaultClassLoader;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Maps.getOptMapObject;
import static org.corant.shared.util.Maps.toProperties;
import static org.corant.shared.util.Strings.isNotBlank;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import javax.naming.NamingException;
import javax.sql.DataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.corant.modules.datasource.shared.AbstractDataSourceExtension;
import org.corant.modules.datasource.shared.DataSourceConfig;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Experimental;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.util.Conversions;
import org.corant.shared.util.Services;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * corant-modules-datasource-hikari
 *
 * @author bingo 下午7:56:58
 */
@Experimental
public class HikariCPDataSourceExtension extends AbstractDataSourceExtension {

  void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery event) {
    if (event != null) {
      getConfigManager().getAllWithQualifiers().forEach((dsc, dsn) -> {
        if (dsc.isEnable()) {
          event.<DataSource>addBean().addQualifiers(dsn).addTransitiveTypeClosure(DataSource.class)
              .addTransitiveTypeClosure(Closeable.class).beanClass(ExtendedDataSource.class)
              .scope(ApplicationScoped.class).produceWith(beans -> {
                try {
                  return produce(beans, dsc);
                } catch (NamingException | SQLException e) {
                  throw new CorantRuntimeException(e);
                }
              }).disposeWith((dataSource, beans) -> {
                try {
                  ((Closeable) dataSource).close();
                } catch (IOException e) {
                  throw new CorantRuntimeException(e);
                }
              });
          if (isNotBlank(dsc.getName()) && dsc.isBindToJndi()) {
            registerJndi(dsc.getName(), dsn);
          }
        }
      });
    }
  }

  DataSource produce(Instance<Object> instance, DataSourceConfig cfg)
      throws NamingException, SQLException {
    if (!cfg.isEnableCustomTransactionIntegration()) {
      shouldBeFalse(cfg.isJta() || cfg.isXa());
    }
    HikariConfig hcfg = new HikariConfig();
    getOptMapObject(cfg.getCtrlProperties(), "allow-pool-suspension", Conversions::toBoolean)
        .ifPresent(hcfg::setAllowPoolSuspension);
    hcfg.setAutoCommit(cfg.isAutoCommit());
    getOptMapObject(cfg.getCtrlProperties(), "catalog", String.class).ifPresent(hcfg::setCatalog);
    hcfg.setConnectionInitSql(cfg.getInitialSql());
    getOptMapObject(cfg.getCtrlProperties(), "test-query", String.class)
        .ifPresent(hcfg::setConnectionTestQuery);
    hcfg.setConnectionTimeout(cfg.getAcquisitionTimeout().toMillis());
    getOptMapObject(cfg.getCtrlProperties(), "data-source-class-name", String.class)
        .ifPresent(hcfg::setDataSourceClassName);
    getOptMapObject(cfg.getCtrlProperties(), "data-source-jndi", String.class)
        .ifPresent(hcfg::setDataSourceJNDI);
    if (isNotEmpty(cfg.getJdbcProperties())) {
      hcfg.setDataSourceProperties(toProperties(cfg.getJdbcProperties()));
    }

    if (cfg.getDriver() != null) {
      hcfg.setDriverClassName(cfg.getDriver().getName());
    }

    getOptMapObject(cfg.getCtrlProperties(), "exception-override-class-name", String.class)
        .ifPresent(hcfg::setExceptionOverrideClassName);

    // cfgs.setHealthCheckProperties(); // Use Configurator SPI
    // cfgs.setHealthCheckRegistry(); //Use Configurator SPI

    hcfg.setIdleTimeout(cfg.getReapTimeout().toMillis());

    getOptMapObject(cfg.getCtrlProperties(), "initialization-fail-timeout", Conversions::toLong)
        .ifPresent(hcfg::setInitializationFailTimeout);

    getOptMapObject(cfg.getCtrlProperties(), "isolate-internal-queries", Conversions::toBoolean)
        .ifPresent(hcfg::setIsolateInternalQueries);

    hcfg.setJdbcUrl(cfg.getConnectionUrl());

    getOptMapObject(cfg.getCtrlProperties(), "keep-alive-time", Conversions::toLong)
        .ifPresent(hcfg::setKeepaliveTime);

    hcfg.setLeakDetectionThreshold(cfg.getLeakTimeout().toMillis());

    hcfg.setMaximumPoolSize(cfg.getMaxSize());

    hcfg.setMaxLifetime(cfg.getMaxLifetime().toMillis());

    hcfg.setMinimumIdle(cfg.getMinSize());

    if (cfg.getUsername() != null) {
      hcfg.setUsername(cfg.getUsername());
    }
    if (cfg.getPassword() != null) {
      hcfg.setPassword(cfg.getPassword());
    }
    hcfg.setPoolName(cfg.getName());

    getOptMapObject(cfg.getCtrlProperties(), "read-only", Conversions::toBoolean)
        .ifPresent(hcfg::setReadOnly);

    hcfg.setRegisterMbeans(cfg.isEnableMetrics());

    getOptMapObject(cfg.getCtrlProperties(), "schema", String.class).ifPresent(hcfg::setSchema);

    if (cfg.getIsolationLevel() > -1) {
      switch (cfg.getIsolationLevel()) {
        case Connection.TRANSACTION_READ_COMMITTED:
          hcfg.setTransactionIsolation("TRANSACTION_READ_COMMITTED");
          break;
        case Connection.TRANSACTION_READ_UNCOMMITTED:
          hcfg.setTransactionIsolation("TRANSACTION_READ_UNCOMMITTED");
          break;
        case Connection.TRANSACTION_REPEATABLE_READ:
          hcfg.setTransactionIsolation("TRANSACTION_REPEATABLE_READ");
          break;
        case Connection.TRANSACTION_SERIALIZABLE:
          hcfg.setTransactionIsolation("TRANSACTION_SERIALIZABLE");
          break;
        default:
          hcfg.setTransactionIsolation("TRANSACTION_NONE");
          break;
      }
    }

    hcfg.setValidationTimeout(cfg.getValidationTimeout().toMillis());

    Services.selectRequired(HikariCPDataSourceConfigurator.class, defaultClassLoader())
        .sorted(Sortable::reverseCompare).forEach(c -> c.config(cfg, hcfg));

    HikariDataSource hikariDataSource = new HikariDataSource(hcfg);
    if (!Duration.ZERO.equals(cfg.getLoginTimeout())) {
      hikariDataSource.setLoginTimeout((int) cfg.getLoginTimeout().toSeconds());
    }

    if (cfg.isEnableCustomTransactionIntegration()) {
      return new ExtendedDataSource(hikariDataSource,
          instance.select(TransactionManager.class).get(),
          instance.select(TransactionSynchronizationRegistry.class).get());
    } else {
      return hikariDataSource;
    }
  }
}
