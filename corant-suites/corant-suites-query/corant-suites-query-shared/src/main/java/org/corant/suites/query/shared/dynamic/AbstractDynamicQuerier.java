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

import static org.corant.shared.util.ObjectUtils.forceCast;
import java.util.List;
import java.util.Map;
import org.corant.suites.query.shared.QueryParameter;
import org.corant.suites.query.shared.QueryParameterResolver;
import org.corant.suites.query.shared.QueryResultResolver;
import org.corant.suites.query.shared.mapping.FetchQuery;
import org.corant.suites.query.shared.mapping.Query;

/**
 * corant-suites-query-shared
 *
 * @author bingo 下午5:56:22
 *
 */
public abstract class AbstractDynamicQuerier<P, S> implements DynamicQuerier<P, S> {

  protected final Query query;
  protected final QueryParameterResolver parameterResolver;
  protected final QueryResultResolver resultResolver;
  protected final QueryParameter queryParameter;

  /**
   * @param query
   * @param queryParameter
   * @param parameterResolver
   * @param resultResolver
   */
  protected AbstractDynamicQuerier(Query query, QueryParameter queryParameter,
      QueryParameterResolver parameterResolver, QueryResultResolver resultResolver) {
    super();
    this.query = query;
    this.queryParameter = queryParameter;
    this.parameterResolver = parameterResolver;
    this.resultResolver = resultResolver;
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public QueryParameter getQueryParameter() {
    return queryParameter;
  }

  @Override
  public void resolveFetchedResult(Object result, Object fetchResult, String injectProName) {
    resultResolver.resolveFetchedResult(result, fetchResult, injectProName);
  }

  @Override
  public Map<String, Object> resolveFetchQueryCriteria(Object result, FetchQuery fetchQuery) {
    return parameterResolver.resolveFetchQueryCriteria(result, fetchQuery, getQueryParameter());
  }

  @Override
  public <T> List<T> resolveResult(List<?> results) {
    return resultResolver.resolve(results, forceCast(getQuery().getResultClass()),
        getQuery().getHints(), getQueryParameter());
  }

  @Override
  public <T> T resolveResult(Object result) {
    return resultResolver.resolve(result, forceCast(getQuery().getResultClass()),
        getQuery().getHints(), getQueryParameter());
  }

  @Override
  public void resolveResultHints(Object result) {
    resultResolver.resolveResultHints(result, Map.class, getQuery().getHints(),
        getQueryParameter());// FIXME map class
  }

}
