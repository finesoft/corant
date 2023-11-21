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

import static org.corant.shared.util.Assertions.shouldNotNull;
import org.corant.context.Beans;
import org.corant.modules.query.NamedQueryService;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.mapping.Query.QueryType;

/**
 * corant-modules-query-shared
 *
 * @author bingo 下午6:46:14
 *
 */
public interface NamedQueryServiceManager {

  /**
   * Resolves the named query service by query type and qualifier
   *
   * @param queryType the query type
   * @param qualifier the query qualifier
   */
  static NamedQueryService resolveQueryService(QueryType queryType, String qualifier) {
    for (NamedQueryServiceManager nqsm : Beans.select(NamedQueryServiceManager.class)) {
      if (nqsm.getType() == queryType) {
        NamedQueryService nqs = nqsm.get(qualifier);
        if (nqs != null) {
          return nqs;
        }
      }
    }
    throw new QueryRuntimeException(
        "Can't find any query service with query type: %s, qualifier: %s", queryType, qualifier);
  }

  /**
   * Resolves the named query service by query name
   *
   * @param queryName the query name
   * @since 2023-11-20
   */
  static NamedQueryService resolveQueryService(String queryName) {
    Query query = shouldNotNull(Beans.resolve(QueryMappingService.class).getQuery(queryName),
        () -> new QueryRuntimeException("Can't find any named '%s' query", queryName));
    return resolveQueryService(query.getType(), query.getQualifier());
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
