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
package org.corant.suites.datasource.hikari;

import static org.corant.shared.util.Assertions.shouldBeFalse;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.lang.annotation.Annotation;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.corant.kernel.util.Cdis;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.datasource.shared.AbstractDataSourceExtension;
import org.corant.suites.datasource.shared.DataSourceConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * corant-suites-datasource
 *
 * @author bingo 下午7:56:58
 *
 */
public class HikariCPDataSourceExtension extends AbstractDataSourceExtension {

  void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery event) {
    if (event != null) {
      getDataSourceConfigs().forEach((dsn, dsc) -> {
        final Annotation[] qualifiers = Cdis.resolveNameds(dsn);
        event.<DataSource>addBean().addQualifiers(qualifiers)
            .addTransitiveTypeClosure(HikariDataSource.class).beanClass(HikariDataSource.class)
            .scope(ApplicationScoped.class).produceWith(beans -> {
              try {
                return produce(beans, dsc);
              } catch (NamingException e) {
                throw new CorantRuntimeException(e);
              }
            }).disposeWith((dataSource, beans) -> dataSource.close());
        if (isNotBlank(dsn)) {
          registerJndi(dsn, qualifiers);
        }
      });
    }
  }

  HikariDataSource produce(Instance<Object> instance, DataSourceConfig cfg) throws NamingException {
    shouldBeFalse(cfg.isJta() || cfg.isXa());
    HikariConfig cfgs = new HikariConfig();
    cfgs.setJdbcUrl(cfg.getConnectionUrl());
    cfgs.setDriverClassName(cfg.getDriver().getName());
    if (cfg.getUsername() != null) {
      cfgs.setUsername(cfg.getUsername());
    }
    if (cfg.getPassword() != null) {
      cfgs.setPassword(cfg.getPassword());
    }
    cfgs.setMinimumIdle(cfg.getMinSize());
    cfgs.setMaximumPoolSize(cfg.getMaxSize());
    cfgs.setPoolName(cfg.getName());
    cfgs.setValidationTimeout(cfg.getValidationTimeout().toMillis());
    HikariDataSource datasource = new HikariDataSource(cfgs);
    return datasource;
  }
}
