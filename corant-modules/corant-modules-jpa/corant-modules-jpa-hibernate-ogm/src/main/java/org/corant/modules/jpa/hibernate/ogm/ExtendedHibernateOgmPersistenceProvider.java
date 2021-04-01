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
package org.corant.modules.jpa.hibernate.ogm;

import java.util.Map;
import javax.persistence.EntityManagerFactory;
import org.corant.modules.jpa.hibernate.orm.ExtendedHibernateOrmPersistenceProvider;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.ogm.jpa.HibernateOgmPersistence;

/**
 * corant-modules-jpa-hibernate-orm
 *
 * @author bingo 下午6:37:17
 *
 */
public class ExtendedHibernateOgmPersistenceProvider extends HibernateOgmPersistence {

  final HibernatePersistenceProvider extendedDelegate =
      new ExtendedHibernateOrmPersistenceProvider();

  @Override
  public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName,
      @SuppressWarnings("rawtypes") Map properties) {
    EntityManagerFactory emf = super.createEntityManagerFactory(persistenceUnitName, properties);
    if (emf == null) {
      // TODO
    }
    return emf;
  }

}
