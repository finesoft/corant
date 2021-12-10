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

import static org.corant.context.Beans.resolve;
import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Objects.defaultObject;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.corant.modules.query.NamedQueryService;
import org.corant.modules.query.QueryObjectMapper;
import org.corant.modules.query.QueryParameter.DefaultQueryParameter;
import org.corant.modules.query.QueryService.Forwarding;
import org.corant.modules.query.QueryService.Paging;
import org.corant.modules.query.mapping.Query.QueryType;
import org.corant.shared.util.Streams;

/**
 * corant-modules-query-shared
 *
 * @author bingo 下午5:15:27
 *
 */
public class NamedQueryServices {

  private String queryName;
  private Annotation qualifier;
  private NamedQueryService namedQueryService;
  private DefaultQueryParameter parameter = new DefaultQueryParameter();

  /**
   * @param queryName
   * @param qualifier
   */
  protected NamedQueryServices(String queryName, Annotation qualifier) {
    this.queryName = shouldNotBlank(queryName);
    this.qualifier = qualifier;
  }

  /**
   * @param queryName
   * @param namedQueryService
   */
  protected NamedQueryServices(String queryName, NamedQueryService namedQueryService) {
    this.queryName = shouldNotBlank(queryName);
    this.namedQueryService = shouldNotNull(namedQueryService);
  }

  public static NamedQueryServices of(NamedQueryService namedQueryService, String queryName) {
    return new NamedQueryServices(queryName, namedQueryService);
  }

  public static NamedQueryServices of(String queryName, Annotation qualifier) {
    return new NamedQueryServices(queryName, qualifier);
  }

  public static NamedQueryServices ofCas(String clusterName, String queryName) {
    return new NamedQueryServices(queryName,
        NamedQueryServiceManager.resolveQueryService(QueryType.CAS, clusterName));
  }

  public static NamedQueryServices ofEs(String clusterName, String queryName) {
    return new NamedQueryServices(queryName,
        NamedQueryServiceManager.resolveQueryService(QueryType.ES, clusterName));
  }

  public static NamedQueryServices ofMg(String database, String queryName) {
    return new NamedQueryServices(queryName,
        NamedQueryServiceManager.resolveQueryService(QueryType.MG, database));
  }

  public static NamedQueryServices ofSql(String dataSourceWithDialect, String queryName) {
    return new NamedQueryServices(queryName,
        NamedQueryServiceManager.resolveQueryService(QueryType.SQL, dataSourceWithDialect));
  }

  public <T> Stream<List<T>> batch() {
    return Streams.batchStream(defaultObject(parameter.getLimit(), 16), stream());
  }

  public NamedQueryServices context(Map<String, Object> context) {
    parameter.context(context);
    return this;
  }

  /**
   * Key and value pairs
   *
   * @param objects
   */
  public NamedQueryServices context(Object... objects) {
    parameter.context(objects);
    return this;
  }

  /**
   * Criteria map
   *
   * @param criteria
   * @return criteria
   */
  public NamedQueryServices criteria(Map<String, Object> criteria) {
    parameter.criteria(criteria);
    return this;
  }

  /**
   * Key and value pairs
   *
   * @param objects
   */
  public NamedQueryServices criteria(Object... objects) {
    parameter.criteria(mapOf(objects));
    return this;
  }

  public <T> Forwarding<T> forward() {
    return resolveQueryService().forward(queryName, parameter);
  }

  public <T> T get() {
    return resolveQueryService().get(queryName, parameter);
  }

  public NamedQueryServices limit(Integer limit) {
    parameter.limit(limit);
    return this;
  }

  public NamedQueryServices offset(Integer offset) {
    parameter.offset(offset);
    return this;
  }

  public <T> Paging<T> page() {
    return resolveQueryService().page(queryName, parameter);
  }

  public <T> List<T> select() {
    return resolveQueryService().select(queryName, parameter);
  }

  public <T> Stream<T> stream() {
    return resolveQueryService().stream(queryName, parameter);
  }

  public <T> Stream<T> streamAs(Class<T> cls) {
    final QueryObjectMapper objectMapper = resolve(QueryObjectMapper.class);
    return resolveQueryService().stream(queryName, parameter)
        .map(x -> objectMapper.toObject(x, cls));
  }

  protected NamedQueryService resolveQueryService() {
    return qualifier == null ? namedQueryService : resolve(NamedQueryService.class, qualifier);
  }
}
