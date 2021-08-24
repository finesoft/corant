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
package org.corant.modules.query.spi;

import java.util.function.UnaryOperator;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.mapping.FetchQuery;
import org.corant.modules.query.mapping.Query;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-query-api
 *
 * @author bingo 上午10:51:35
 *
 */
public interface QueryParameterReviser extends UnaryOperator<QueryParameter>, Sortable {

  /**
   * Check the query or fetch query object and return whether the reviser can perform query
   * parameter revision. It can be checked based various attributes of the {@link Query} object or
   * the {@link FetchQuery} object, for example: check whether the name of the query
   * {@link Query#getName()} is satisfy some condition to decide whether the reviser can perform
   * query parameter revision.
   *
   * <p>
   * Default we just check whether the given object is instance of {@link Query}, it means that is
   * not support {@link FetchQuery} parameter revision.
   *
   * @param query the query object may be Query or FetchQuery
   * @return whether the reviser can perform query parameter revision
   */
  default boolean supports(Object query) {
    return query instanceof Query;
  }

}
