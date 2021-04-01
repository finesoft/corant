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

import static org.corant.context.Instances.find;
import static org.corant.context.Instances.resolve;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import org.corant.modules.datasource.shared.DataSourceService;
import org.corant.modules.jpa.shared.JPAExtension;
import org.corant.modules.jpa.shared.PersistenceService.PersistenceUnitLiteral;
import org.corant.modules.jpa.shared.metadata.PersistenceUnitInfoMetaData;
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

  @Override
  public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName,
      @SuppressWarnings("rawtypes") Map properties) {
    EntityManagerFactory emf = super.createEntityManagerFactory(persistenceUnitName, properties);
    if (emf == null) {
      EntityManagerFactoryBuilder builder = resolveBuilder(persistenceUnitName, properties);
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

  protected EntityManagerFactoryBuilder resolveBuilder(String persistenceUnitName,
      @SuppressWarnings("rawtypes") Map map) {
    PersistenceUnitInfoMetaData pui = resolve(JPAExtension.class)
        .getPersistenceUnitInfoMetaData(PersistenceUnitLiteral.of(persistenceUnitName));
    if (pui != null) {
      final PersistenceUnitInfoMetaData thePui =
          pui.with(pui.getProperties(), pui.getPersistenceUnitTransactionType());
      find(DataSourceService.class).ifPresent(ds -> thePui.configDataSource(ds::getManaged));
      return getEntityManagerFactoryBuilder(new PersistenceUnitInfoDescriptor(thePui), map,
          (ClassLoader) null);
    }
    return null;
  }
}
