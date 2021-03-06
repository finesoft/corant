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
package org.corant.modules.query.shared;

import org.corant.context.Beans;
import org.corant.modules.query.NamedQueryService;
import org.corant.modules.query.mapping.Query.QueryType;
import org.corant.shared.ubiquity.Mutable.MutableObject;

/**
 * corant-modules-query-shared
 *
 * @author bingo 下午6:46:14
 *
 */
public interface NamedQueryServiceManager {

  /**
   * Resolve the named query service by query type and qualifier
   *
   * @param queryType
   * @param qualifier
   * @return NamedQueryService
   */
  static NamedQueryService resolveQueryService(QueryType queryType, String qualifier) {
    MutableObject<NamedQueryService> ref = new MutableObject<>();
    Beans.select(NamedQueryServiceManager.class).forEach(nqs -> {
      if (nqs.getType() == queryType) {
        ref.set(nqs.get(qualifier));
      }
    });
    return ref.get();
  }

  /**
   * Obtain a named query service, the parameter can be a MgQuery/SqlQuery/EsQuery/JpqlQuery or a
   * string
   *
   * @param qualifier
   * @return get
   */
  NamedQueryService get(Object qualifier);

  QueryType getType();

}
