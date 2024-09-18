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
package org.corant.modules.ddd;

import java.io.Serializable;
import java.util.List;

/**
 * corant-modules-ddd-api
 *
 * @author bingo 下午6:25:50
 */
public interface TypedRepository<T, Q> {

  /**
   * Retrieve an object from repository by id and object class
   *
   *
   * @param id the entity identifier
   * @return the entity
   */
  T get(Serializable id);

  /**
   * Merge the state of the given object into repository.
   *
   * @param obj the entity instance
   * @return the merged entity
   */
  T merge(T obj);

  /**
   * Save the state of the given object into repository
   *
   * @param obj the entity instance
   * @return the persisted entity
   */
  T persist(T obj);

  /**
   * Remove the given object from the repository
   *
   * @param obj the entity instance
   * @return true means successfully
   */
  boolean remove(T obj);

  /**
   * Retrieve objects from repository by query object
   *
   * @param q the query object
   * @return object list
   */
  List<T> select(Q q);

}
