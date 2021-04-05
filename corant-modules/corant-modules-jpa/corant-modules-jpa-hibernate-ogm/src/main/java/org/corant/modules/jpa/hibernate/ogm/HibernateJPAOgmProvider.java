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
package org.corant.modules.jpa.hibernate.ogm;

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
import org.corant.modules.jpa.hibernate.orm.JTAPlatform;
import org.corant.modules.jpa.shared.JPAProvider;
import org.corant.modules.jpa.shared.metadata.PersistenceUnitInfoMetaData;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.ogm.datastore.mongodb.dialect.impl.AssociationStorageStrategy;
import org.hibernate.ogm.jpa.HibernateOgmPersistence;

/**
 * corant-modules-jpa-hibernate-ogm
 *
 * @author bingo 上午11:44:00
 *
 */
@ApplicationScoped
@Named("org.hibernate.ogm.jpa.HibernateOgmPersistence")
public class HibernateJPAOgmProvider implements JPAProvider {

  protected static final Map<String, Object> DEFAULT_MONGODB_PROPERTIES;
  static {
    Map<String, Object> tmp = new HashMap<>();
    tmp.put("hibernate.ogm.datastore.create_database", true);
    tmp.put("hibernate.ogm.datastore.document.association_storage",
        AssociationStorageStrategy.IN_ENTITY);
    tmp.put("hibernate.ogm.datastore.grid_dialect", HibernateMongoDBDialect.class);
    tmp.put("hibernate.ogm.mongodb.driver.maxConnectionIdleTime", 6000);
    tmp.put("hibernate.ogm.mongodb.driver.socketTimeout", 300000);
    tmp.put("hibernate.ogm.mongodb.driver.connectTimeout", 300000);
    tmp.put("hibernate.ogm.mongodb.driver.autoConnectRetry", true);
    DEFAULT_MONGODB_PROPERTIES = Collections.unmodifiableMap(tmp);
  }

  @Inject
  protected BeanManager beanManager;

  protected final Map<String, Object> defaultProperties = new HashMap<>();

  @Override
  public EntityManagerFactory buildEntityManagerFactory(PersistenceUnitInfoMetaData metaData,
      Map<String, Object> additionalProperties) {
    Object provider = metaData.getProperties().get("hibernate.ogm.datastore.provider");
    if (provider != null && "mongodb".equalsIgnoreCase(provider.toString())) {
      DEFAULT_MONGODB_PROPERTIES.forEach((k, v) -> {
        metaData.getProperties().putIfAbsent(k, v);
      });
    }
    Map<String, Object> properties = new HashMap<>(defaultProperties);
    if (additionalProperties != null) {
      properties.putAll(additionalProperties);
    }
    properties.put(org.hibernate.jpa.AvailableSettings.ENTITY_MANAGER_FACTORY_NAME,
        defaultString(metaData.getPersistenceUnitName()));
    return new HibernateOgmPersistence().createContainerEntityManagerFactory(metaData, properties);
  }

  @PostConstruct
  protected void onPostConstruct() {
    defaultProperties.put(AvailableSettings.JTA_PLATFORM, JTAPlatform.INSTANCE);
    defaultProperties.put(AvailableSettings.CDI_BEAN_MANAGER, beanManager);
  }
}
