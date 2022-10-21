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
package org.corant.modules.query.shared.dynamic;

import java.time.Instant;
import org.corant.modules.query.FetchQueryHandler;
import org.corant.modules.query.QueryHandler;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.Query;

/**
 * corant-modules-query-shared
 *
 * @author bingo 上午9:54:00
 *
 */
public abstract class AbstractDynamicQuerierBuilder<P, S, Q extends DynamicQuerier<P, S>>
    implements DynamicQuerierBuilder<P, S, Q> {

  protected final long cachedTimestamp;
  protected final Query query;
  protected final QueryHandler queryHandler;
  protected final FetchQueryHandler fetchQueryHandler;

  protected AbstractDynamicQuerierBuilder(Query query, QueryHandler queryHandler,
      FetchQueryHandler fetchQueryHandler) {
    if (query == null || queryHandler == null) {
      throw new QueryRuntimeException(
          "Can not initialize dynamic querier builder from null query param!");
    }
    this.cachedTimestamp = Instant.now().toEpochMilli();
    this.query = query;
    this.queryHandler = queryHandler;
    this.fetchQueryHandler = fetchQueryHandler;
  }

  @Override
  public Long getCachedTimestamp() {
    return cachedTimestamp;
  }

  @Override
  public FetchQueryHandler getFetchQueryHandler() {
    return fetchQueryHandler;
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public QueryHandler getQueryHandler() {
    return queryHandler;
  }

  protected QueryParameter resolveParameter(Object param) {
    return queryHandler.resolveParameter(query, param);
  }

}
