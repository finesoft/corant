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
package org.corant.suites.query.sql;

import org.corant.suites.query.shared.NamedQuerier;
import org.corant.suites.query.shared.dynamic.DynamicQuerier;

/**
 * corant-suites-query
 *
 * @author bingo 下午3:13:37
 *
 */
public interface SqlNamedQueryResolver<K, P> {

  SqlQuerier resolve(K key, P param);

  interface SqlQuerier extends DynamicQuerier<Object[], String>, NamedQuerier {
  }

}
