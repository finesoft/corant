/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.suites.jpa.shared;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.corant.suites.jpa.shared.metadata.PersistenceContextMetaData;
import org.corant.suites.jpa.shared.metadata.PersistenceUnitMetaData;

/**
 * corant-suites-jpa-shared
 *
 * @author bingo 上午11:08:47
 *
 */
@ApplicationScoped
public abstract class AbstractJpaProvider {

  protected static final Map<PersistenceUnitMetaData, EntityManagerFactory> EMFS =
      new ConcurrentHashMap<>();

  protected static final ThreadLocal<Map<PersistenceContextMetaData, EntityManager>> EMS =
      ThreadLocal.withInitial(HashMap::new);

  protected Logger logger = Logger.getLogger(getClass().getName());

  /**
   * Be careful when PersistenceContextType is EXTENDED
   *
   * @param metaData
   * @return buildEntityManager
   */
  public EntityManager getEntityManager(PersistenceContextMetaData metaData) {
    return buildEntityManager(metaData);
  }

  /**
   * A persistence unit is associated with an entity manager factory, like
   * singleton and that is thread safe.
   *
   * @param metaData
   * @return getEntityManagerFactory
   */
  public EntityManagerFactory getEntityManagerFactory(PersistenceUnitMetaData metaData) {
    return EMFS.computeIfAbsent(metaData, this::buildEntityManagerFactory);
  }


  protected EntityManager buildEntityManager(PersistenceContextMetaData metaData) {
    return getEntityManagerFactory(metaData.getUnit())
        .createEntityManager(metaData.getSynchronization(), metaData.getProperties());
  }

  protected abstract EntityManagerFactory buildEntityManagerFactory(
      PersistenceUnitMetaData metaData);
}
