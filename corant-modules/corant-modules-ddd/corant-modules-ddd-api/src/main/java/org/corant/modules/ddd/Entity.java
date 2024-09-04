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
package org.corant.modules.ddd;

import java.io.Serializable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 *
 * corant-modules-ddd-api
 *
 * <p>
 * An object is fundamentally defined not by its attributes, but by a thread of continuity and
 * identity
 *
 * @author bingo 下午12:01:50
 */
public interface Entity extends Serializable {

  Serializable getId();

  interface EntityIdentifier extends Value {

    Serializable getId();

    Serializable getType();
  }

  /**
   * corant-modules-ddd-api
   *
   * @author bingo 14:58:45
   */
  interface EntityManagerProvider {

    /**
     * Returns an entity manager by the given entity class
     * FIXME nonstandard
     * @param entityClass the entity class
     */
    default EntityManager getEntityManager(Class<?> entityClass) {
      return getEntityManager(getPersistenceContext(entityClass));
    }

    /**
     * Returns an entity manager by the given persistence context
     *
     * @param persistenceContext the persistence context
     */
    EntityManager getEntityManager(PersistenceContext persistenceContext);

    /**
     * Returns the persistence context for the given entity class
     * FIXME nonstandard
     * @param entityClass the entity class
     */
    PersistenceContext getPersistenceContext(Class<?> entityClass);
  }
}
