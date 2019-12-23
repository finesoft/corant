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
package org.corant.suites.datasource.dbcp;

import static org.corant.shared.util.StringUtils.isNotBlank;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import org.apache.commons.dbcp2.managed.BasicManagedDataSource;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.datasource.shared.AbstractDataSourceExtension;
import org.corant.suites.datasource.shared.DataSourceConfig;
import com.arjuna.ats.jta.common.jtaPropertyManager;

/**
 * corant-suites-datasource-dbcp
 *
 * Unfinish yet
 *
 * @author bingo 下午7:32:17
 *
 */
public class DBCPDataSourceExtension extends AbstractDataSourceExtension {

  /**
   *
   * @param event onAfterBeanDiscovery
   */
  void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery event) {
    jtaPropertyManager.getJTAEnvironmentBean().setLastResourceOptimisationInterfaceClassName(
        "org.apache.commons.dbcp2.managed.LocalXAConnectionFactory$LocalXAResource");
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

  BasicManagedDataSource produce(Instance<Object> instance, DataSourceConfig cfg)
      throws SQLException, NamingException {
    BasicManagedDataSource ds = new BasicManagedDataSource();
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
    return ds;
  }

}
