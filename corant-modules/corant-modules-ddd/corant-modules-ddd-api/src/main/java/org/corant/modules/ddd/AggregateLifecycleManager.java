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

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * corant-modules-ddd-api
 *
 * <p>
 * Manages the life cycle of an aggregate, such as persistent state or deletion
 *
 * @author bingo 上午10:33:56
 *
 */
public interface AggregateLifecycleManager {

  EntityManager getEntityManager(Class<?> cls);

  PersistenceContext getPersistenceContext(Class<?> cls);

  void on(AggregateLifecycleManageEvent e);

  enum LifecycleAction {
    PERSIST, RECOVER, REMOVE
  }
}