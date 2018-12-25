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

import static org.corant.shared.util.CollectionUtils.isEmpty;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SynchronizationType;
import org.jboss.weld.injection.spi.ResourceReference;

/**
 * corant-suites-jpa-shared
 *
 * @author bingo 下午6:46:11
 *
 */
public class ThreadLocalEntityManagerReference implements ResourceReference<EntityManager> {

  protected static ThreadLocal<EntityManager> entityManager = new ThreadLocal<>();
  protected final EntityManagerFactory entityManagerFactory;

  /**
   * @param entityManagerFactory
   */
  public ThreadLocalEntityManagerReference(EntityManagerFactory entityManagerFactory) {
    this(entityManagerFactory, SynchronizationType.SYNCHRONIZED, null);
  }

  /**
   *
   * @param entityManagerFactory
   * @param syncType
   */
  public ThreadLocalEntityManagerReference(EntityManagerFactory entityManagerFactory,
      SynchronizationType syncType) {
    this(entityManagerFactory, syncType, null);
  }

  /**
   * @param entityManagerFactory
   * @param syncType
   * @param properties
   */
  public ThreadLocalEntityManagerReference(EntityManagerFactory entityManagerFactory,
      SynchronizationType syncType, Map<String, Object> properties) {
    super();
    this.entityManagerFactory = entityManagerFactory;
    if (!isEmpty(properties) && syncType != null) {
      entityManager.set(entityManagerFactory.createEntityManager(syncType, properties));
    } else if (!isEmpty(properties)) {
      entityManager.set(entityManagerFactory.createEntityManager(properties));
    } else if (!isEmpty(syncType)) {
      entityManager.set(entityManagerFactory.createEntityManager(syncType));
    } else {
      entityManager.set(entityManagerFactory.createEntityManager());
    }
  }

  @Override
  public EntityManager getInstance() {
    return entityManager.get();
  }

  @Override
  public void release() {
    EntityManager em = entityManager.get();
    if (em != null) {
      synchronized (this) {
        if (em.isOpen()) {
          em.close();
        }
      }
    }
  }
}
