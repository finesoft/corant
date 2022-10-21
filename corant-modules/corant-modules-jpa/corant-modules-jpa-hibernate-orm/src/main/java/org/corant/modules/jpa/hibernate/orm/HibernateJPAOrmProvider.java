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
package org.corant.modules.jpa.hibernate.orm;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Strings.defaultString;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;
import org.corant.modules.datasource.shared.DataSourceService;
import org.corant.modules.jpa.shared.JPAProvider;
import org.corant.modules.jpa.shared.metadata.PersistenceUnitInfoMetaData;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;

/**
 * corant-modules-jpa-hibernate-orm
 *
 * @author bingo 上午11:44:00
 *
 */
@ApplicationScoped
@Named("org.hibernate.jpa.HibernatePersistenceProvider")
public class HibernateJPAOrmProvider implements JPAProvider {

  protected static final Map<String, Object> DEFAULT_PROPERTIES;
  static {
    Map<String, Object> tmp = new HashMap<>();
    tmp.put("hibernate.show_sql", false);
    tmp.put("hibernate.format_sql", false);
    tmp.put("hibernate.use_sql_comments", false);
    tmp.put("hibernate.connection.autocommit", false);
    tmp.put("hibernate.archive.autodetection", "class, hbm");
    tmp.put("javax.persistence.query.timeout", 100000);
    // tmp.put("javax.persistence.schema-generation.database.action", Action.NONE);
    DEFAULT_PROPERTIES = Collections.unmodifiableMap(tmp);
  }

  @Inject
  protected BeanManager beanManager;

  protected final Map<String, Object> defaultProperties = new HashMap<>();

  @Inject
  protected DataSourceService dataSourceService;

  @Override
  public EntityManagerFactory buildEntityManagerFactory(PersistenceUnitInfoMetaData metaData,
      Map<String, Object> additionalProperties) {
    DEFAULT_PROPERTIES.forEach((k, v) -> {
      metaData.getProperties().putIfAbsent(k, v);
    });
    shouldNotNull(metaData).configDataSource(dataSourceService::tryResolve);
    Map<String, Object> properties = new HashMap<>(defaultProperties);
    if (additionalProperties != null) {
      properties.putAll(additionalProperties);
    }
    properties.put(org.hibernate.jpa.AvailableSettings.ENTITY_MANAGER_FACTORY_NAME,
        defaultString(metaData.getPersistenceUnitName()));
    return new HibernatePersistenceProvider().createContainerEntityManagerFactory(metaData,
        properties);
  }

  @PostConstruct
  protected void onPostConstruct() {
    defaultProperties.put(AvailableSettings.JTA_PLATFORM, JTAPlatform.INSTANCE);
    defaultProperties.put(AvailableSettings.CDI_BEAN_MANAGER, beanManager);
  }

}
