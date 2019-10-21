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
package org.corant.suites.query.shared.dynamic;

import java.time.Instant;
import org.corant.suites.query.shared.FetchQueryResolver;
import org.corant.suites.query.shared.QueryParameter;
import org.corant.suites.query.shared.QueryParameterResolver;
import org.corant.suites.query.shared.QueryResultResolver;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.shared.mapping.Query;

/**
 * corant-suites-query
 *
 * @author bingo 上午9:54:00
 *
 */
public abstract class AbstractDynamicQuerierBuilder<P, S, Q extends DynamicQuerier<P, S>>
    implements DynamicQuerierBuilder<P, S, Q> {

  protected final long cachedTimestemp;
  protected final Query query;
  protected final QueryParameterResolver parameterResolver;
  protected final QueryResultResolver resultResolver;
  protected final FetchQueryResolver fetchQueryResolver;

  protected AbstractDynamicQuerierBuilder(Query query, QueryParameterResolver parameterResolver,
      QueryResultResolver resultResolver, FetchQueryResolver fetchQueryResolver) {
    if (query == null || parameterResolver == null) {
      throw new QueryRuntimeException(
          "Can not initialize dynamic querier builder from null query param!");
    }
    this.cachedTimestemp = Instant.now().toEpochMilli();
    this.query = query;
    this.parameterResolver = parameterResolver;
    this.resultResolver = resultResolver;
    this.fetchQueryResolver = fetchQueryResolver;
  }

  @Override
  public Long getCachedTimestemp() {
    return cachedTimestemp;
  }

  @Override
  public QueryParameterResolver getParameterResolver() {
    return parameterResolver;
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public QueryResultResolver getResultResolver() {
    return resultResolver;
  }

  protected FetchQueryResolver getFetchQueryResolver() {
    return fetchQueryResolver;
  }

  protected QueryParameter resolveParameter(Object param) {
    return parameterResolver.resolveQueryParameter(query, param);
  }

}
