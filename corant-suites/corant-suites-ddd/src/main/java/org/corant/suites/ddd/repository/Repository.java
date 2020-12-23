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
package org.corant.suites.ddd.repository;

import java.io.Serializable;
import java.util.List;

/**
 * corant-suites-ddd
 *
 * @author bingo 下午9:39:03
 *
 */
public interface Repository<Q> {

  /**
   * Retrieve object from repository by id and object class
   *
   * @param <T>
   * @param cls
   * @param id
   * @return get
   */
  <T> T get(Class<T> cls, Serializable id);

  /**
   * Merge the state of the given object into repository.
   *
   * @param <T>
   * @param obj
   * @return merge
   */
  <T> T merge(T obj);

  /**
   * Save the state of the given object into repository
   *
   * @param <T>
   * @param obj
   * @return persist
   */
  <T> boolean persist(T obj);

  /**
   * Remove the object from repository
   *
   * @param <T>
   * @param obj
   * @return true means successfully
   */
  <T> boolean remove(T obj);

  /**
   * Retrieve objects from repository by query object
   *
   * @param <T>
   * @param q
   * @return object list
   */
  <T> List<T> select(Q q);

}
