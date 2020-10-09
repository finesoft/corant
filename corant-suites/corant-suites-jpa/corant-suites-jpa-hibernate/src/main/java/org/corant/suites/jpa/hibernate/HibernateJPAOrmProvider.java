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
package org.corant.suites.jpa.hibernate;

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;
import org.corant.suites.datasource.shared.DataSourceService;
import org.corant.suites.jpa.shared.JPAProvider;
import org.corant.suites.jpa.shared.metadata.PersistenceUnitInfoMetaData;
import org.corant.suites.jta.shared.TransactionService;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.tool.schema.Action;

/**
 * corant-suites-jpa-hibernate
 *
 * @author bingo 上午11:44:00
 *
 */
@ApplicationScoped
@Named("org.hibernate.jpa.HibernatePersistenceProvider")
public class HibernateJPAOrmProvider implements JPAProvider {

  @Inject
  protected TransactionService transactionService;

  @Inject
  protected BeanManager beanManager;

  protected final Map<String, Object> PROPERTIES = new HashMap<>();

  protected final Map<String, Object> DEFAULT_PROPERTIES = new HashMap<>();
  {
    DEFAULT_PROPERTIES.put("hibernate.show_sql", false);
    DEFAULT_PROPERTIES.put("hibernate.format_sql", false);
    DEFAULT_PROPERTIES.put("hibernate.use_sql_comments", false);
    DEFAULT_PROPERTIES.put("hibernate.connection.autocommit", false);
    DEFAULT_PROPERTIES.put("hibernate.archive.autodetection", "class, hbm");
    DEFAULT_PROPERTIES.put("javax.persistence.query.timeout", 100000);
    DEFAULT_PROPERTIES.put("javax.persistence.schema-generation.database.action", Action.NONE);
  }

  @Inject
  protected DataSourceService dataSourceService;

  @Override
  public EntityManagerFactory buildEntityManagerFactory(PersistenceUnitInfoMetaData metaData,
      Map<String, Object> additionalProperties) {
    DEFAULT_PROPERTIES.forEach((k, v) -> {
      metaData.getProperties().putIfAbsent(k, v);
    });
    shouldNotNull(metaData).configDataSource(dataSourceService::getManaged);
    Map<String, Object> properties = new HashMap<>(PROPERTIES);
    if (additionalProperties != null) {
      properties.putAll(additionalProperties);
    }
    return new HibernatePersistenceProvider().createContainerEntityManagerFactory(metaData,
        properties);
  }

  @PostConstruct
  protected void onPostConstruct() {
    PROPERTIES.put(AvailableSettings.JTA_PLATFORM, new JTAPlatform(transactionService));
    PROPERTIES.put(AvailableSettings.CDI_BEAN_MANAGER, beanManager);
  }

}
