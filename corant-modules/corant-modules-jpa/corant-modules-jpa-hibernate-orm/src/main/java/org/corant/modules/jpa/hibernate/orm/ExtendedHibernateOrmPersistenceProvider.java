/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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

import static org.corant.context.Beans.find;
import static org.corant.context.Beans.resolve;
import static org.corant.shared.util.Strings.defaultString;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.inject.spi.BeanManager;
import javax.persistence.EntityManagerFactory;
import org.corant.modules.datasource.shared.DataSourceService;
import org.corant.modules.jpa.shared.JPAExtension;
import org.corant.modules.jpa.shared.PersistenceService.PersistenceUnitLiteral;
import org.corant.modules.jpa.shared.metadata.PersistenceUnitInfoMetaData;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;

/**
 * corant-modules-jpa-hibernate-orm
 *
 * Used to manually obtain the EntityManagerFactory in the SE environment when the Persistence Unit
 * is configured using the .properties file.
 *
 * @author bingo 下午6:37:17
 *
 */
public class ExtendedHibernateOrmPersistenceProvider extends HibernatePersistenceProvider {

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName,
      Map properties) {
    EntityManagerFactory emf = super.createEntityManagerFactory(persistenceUnitName, properties);
    if (emf == null) {
      Map thePros = properties == null ? new HashMap<>() : new HashMap<>(properties);
      HibernateJPAOrmProvider.DEFAULT_PROPERTIES.forEach((k, v) -> thePros.putIfAbsent(k, v));
      thePros.put(AvailableSettings.JTA_PLATFORM, JTAPlatform.INSTANCE);
      thePros.put(AvailableSettings.CDI_BEAN_MANAGER, resolve(BeanManager.class));
      EntityManagerFactoryBuilder builder = resolveBuilder(persistenceUnitName, thePros);
      if (builder != null) {
        return builder.build();
      }
    }
    return emf;
  }

  @Override
  public boolean generateSchema(String persistenceUnitName, @SuppressWarnings("rawtypes") Map map) {
    boolean gen = super.generateSchema(persistenceUnitName, map);
    if (!gen) {
      EntityManagerFactoryBuilder builder = resolveBuilder(persistenceUnitName, map);
      if (builder != null) {
        builder.generateSchema();
        gen = true;
      }
    }
    return gen;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  protected EntityManagerFactoryBuilder resolveBuilder(String persistenceUnitName, Map map) {
    PersistenceUnitInfoMetaData pui = resolve(JPAExtension.class)
        .getPersistenceUnitInfoMetaData(PersistenceUnitLiteral.of(persistenceUnitName));
    if (pui != null && pui.getPersistenceProviderClassName()
        .equals(HibernatePersistenceProvider.class.getName())) {
      final PersistenceUnitInfoMetaData thePui =
          pui.with(pui.getProperties(), pui.getPersistenceUnitTransactionType());
      find(DataSourceService.class).ifPresent(ds -> thePui.configDataSource(ds::tryResolve));
      Map thePros = new HashMap<>(map);
      thePros.put(org.hibernate.jpa.AvailableSettings.ENTITY_MANAGER_FACTORY_NAME,
          defaultString(pui.getPersistenceUnitName()));
      return getEntityManagerFactoryBuilder(new PersistenceUnitInfoDescriptor(thePui), thePros,
          (ClassLoader) null);
    }
    return null;
  }
}
