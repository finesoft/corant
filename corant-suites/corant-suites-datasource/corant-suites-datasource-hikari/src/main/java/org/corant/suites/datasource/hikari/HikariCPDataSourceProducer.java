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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import org.corant.shared.normal.Names.JndiNames;
import org.corant.suites.datasource.shared.DataSourceConfig;
import org.eclipse.microprofile.config.Config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * corant-suites-datasource
 *
 * @author bingo 下午7:56:58
 *
 */
// @ApplicationScoped
public class HikariCPDataSourceProducer {

  @Inject
  Config config;

  @Inject
  TransactionManager transactionManager;

  @Inject
  TransactionSynchronizationRegistry transactionSynchronizationRegistry;

  @Inject
  InitialContext jndi;

  protected final Map<String, DataSourceConfig> configs = new HashMap<>();

  public void dispose(@Disposes HikariDataSource ds) {
    ds.close();
  }

  HikariDataSource doProduce(DataSourceConfig cfg) {
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
    return new HikariDataSource(cfgs);
  }

  @PostConstruct
  void onPostConstruct() {
    configs.putAll(DataSourceConfig.from(config));
  }

  @Produces
  @ApplicationScoped
  synchronized HikariDataSource produce(InjectionPoint injectionPoint) throws NamingException {
    HikariDataSource dataSource = null;
    final Set<Annotation> qualifiers = injectionPoint.getQualifiers();
    for (final Annotation qualifier : qualifiers) {
      if (qualifier instanceof Named) {
        String dataSourceName = ((Named) qualifier).value();
        if (isNotBlank(dataSourceName)) {
          DataSourceConfig cfg = configs.get(dataSourceName);
          if (cfg != null) {
            dataSource = doProduce(cfg);
            jndi.bind(JndiNames.JNDI_DATS_NME + "/" + dataSourceName, dataSource);
            break;
          }
        }
      }
    }
    return dataSource;
  }
}
