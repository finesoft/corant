/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.query.jaxrs;

import static java.util.Collections.emptyMap;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.defaultString;
import static org.corant.shared.util.Strings.isNotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import org.corant.modules.query.FetchableNamedQuerier;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.QueryParameter.DefaultQueryParameter;
import org.corant.modules.query.mapping.FetchQuery;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.shared.AbstractNamedQuerierResolver;
import org.corant.modules.query.shared.AbstractNamedQueryService;
import org.corant.shared.ubiquity.Experimental;
import org.corant.shared.ubiquity.TypeLiteral;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocationBuilder;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

/**
 * corant-modules-query-jaxrs
 *
 * @author bingo 21:35:05
 */
@Experimental
public abstract class AbstractJaxrsNamedQueryService extends AbstractNamedQueryService {

  protected static final GenericType<List<Map<String, Object>>> listMapType =
      new GenericType<>(TypeLiteral.LIST_DOC_TYPE.getType());

  protected static final GenericType<Paging<Map<String, Object>>> pagingMapType =
      new GenericType<>(new TypeLiteral<Paging<Map<String, Object>>>() {}.getType());

  @Override
  public FetchedResult fetch(Object result, FetchQuery fetchQuery,
      FetchableNamedQuerier parentQuerier) {
    QueryParameter fetchParam = parentQuerier.resolveFetchQueryParameter(result, fetchQuery);
    String refQueryName = fetchQuery.getReferenceQuery().getVersionedName();
    JaxrsNamedQuerier querier = getQuerierResolver().resolve(getQuery(refQueryName), fetchParam);
    int maxFetchSize = querier.resolveMaxFetchSize(result, fetchQuery);
    QueryParameter usedFetchParam = new DefaultQueryParameter(fetchParam).limit(maxFetchSize);
    List<Map<String, Object>> results =
        resolveInvocation(usedFetchParam, querier).invoke(listMapType);
    return new FetchedResult(fetchQuery, querier, results);
  }

  @Override
  protected <T> Forwarding<T> doForward(Query query, QueryParameter queryParameter)
      throws Exception {
    JaxrsNamedQuerier querier = getQuerierResolver().resolve(query, queryParameter);
    int limit = querier.resolveLimit();
    DefaultQueryParameter useQueryParam = new DefaultQueryParameter(queryParameter);
    useQueryParam.limit(limit + 1);
    List<Map<String, Object>> list = resolveInvocation(useQueryParam, querier).invoke(listMapType);
    Forwarding<T> result = Forwarding.inst();
    int size = sizeOf(list);
    if (size > 0) {
      if (size > limit) {
        list.remove(limit);
        result.withHasNext(true);
      }
      handleFetching(list, querier);
    }
    return result.withResults(querier.handleResults(list));
  }

  @SuppressWarnings("unchecked")
  @Override
  protected <T> T doGet(Query query, QueryParameter queryParameter) throws Exception {
    JaxrsNamedQuerier querier = getQuerierResolver().resolve(query, queryParameter);
    Object result = resolveInvocation(queryParameter, querier).invoke(Object.class);
    if (result == null) {
      return null;
    }
    Map<Object, Object> mapResult;
    if (result instanceof Map map) {
      mapResult = map;
    } else {
      mapResult = mapOf("value", result);
    }
    handleFetching(mapResult, querier);
    return querier.handleResult(mapResult);
  }

  @Override
  protected <T> Paging<T> doPage(Query query, QueryParameter queryParameter) throws Exception {
    JaxrsNamedQuerier querier = getQuerierResolver().resolve(query, queryParameter);
    Paging<Map<String, Object>> page =
        resolveInvocation(queryParameter, querier).invoke(pagingMapType);
    List<Map<String, Object>> list = page.getResults();
    int size = sizeOf(list);
    if (size > 0) {
      handleFetching(list, querier);
    }
    Paging<T> result = Paging.of(page.getOffset(), page.getPageSize());
    return result.withTotal(page.getTotal()).withResults(querier.handleResults(list));
  }

  @Override
  protected <T> List<T> doSelect(Query query, QueryParameter queryParameter) throws Exception {
    JaxrsNamedQuerier querier = getQuerierResolver().resolve(query, queryParameter);
    List<Map<String, Object>> results =
        resolveInvocation(queryParameter, querier).invoke(listMapType);
    if (querier.handleResultSize(results) > 0) {
      handleFetching(results, querier);
    }
    return querier.handleResults(results);
  }

  protected abstract AbstractNamedQuerierResolver<JaxrsNamedQuerier> getQuerierResolver();

  protected Invocation resolveInvocation(QueryParameter queryParameter, JaxrsNamedQuerier querier) {
    WebTarget target = querier.getTarget();
    JaxrsQueryParameter parameter = querier.getParameter();
    if (isNotBlank(parameter.getPath())) {
      target = target.path(parameter.getPath());
    }
    JaxrsNamedQueryClientConfig clientConfig = querier.getClientConfig();
    Builder builder = target.request(parameter.getRequestMediaTypeArray())
        .accept(parameter.getAcceptMediaTypeArray());
    Predicate<String> propagateHeaderNameFilter = defaultObject(
        parameter.getPropagateHeaderNameFilter(), clientConfig.getPropagateHeaderNameFilter());
    if (propagateHeaderNameFilter != null) {
      // TODO FIXME use client request filter
      HttpRequest request = ResteasyProviderFactory.getInstance().getContextData(HttpRequest.class);
      if (request != null) {
        request.getHttpHeaders().getRequestHeaders().forEach((k, v) -> {
          if (propagateHeaderNameFilter.test(k)) {
            builder.header(k, v);
          }
        });
      }
    }
    if (isNotEmpty(clientConfig.getHeaders())) {
      clientConfig.getHeaders().forEach(builder::header);
    }
    Entity<?> entity = resolveInvocationEntity(querier.getParameter());
    // FIXME logging
    log(querier.getName(), () -> {
      List<String> exes = new ArrayList<>();
      exes.add("Path: " + clientConfig.getRoot() + defaultString(parameter.getPath()));
      exes.add("Method: " + parameter.getHttpMethod());
      exes.add("Content-type: " + querier.getParameter().getEntityMediaType());
      if (builder instanceof ClientInvocationBuilder cb) {
        cb.getHeaders().asMap().forEach((k, v) -> exes.add(k + ": " + v));
      }
      return exes;
    }, entity == null ? null : entity.getEntity());
    if (entity != null) {
      return builder.build(querier.getParameter().getHttpMethod(), entity);
    } else {
      return builder.build(querier.getParameter().getHttpMethod());
    }
  }

  protected Entity<?> resolveInvocationEntity(JaxrsQueryParameter parameter) {
    if (parameter.isOnlyUsePathParameters()) {
      return null;
    }
    Object entity = parameter.isOnlyUseEmptyMapAsParameter() ? emptyMap() : parameter.getEntity();
    return Entity.entity(entity, parameter.getEntityMediaType());

  }
}