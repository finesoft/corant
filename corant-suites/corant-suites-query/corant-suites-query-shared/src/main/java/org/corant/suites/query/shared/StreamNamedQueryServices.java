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
package org.corant.suites.query.shared;

import static org.corant.context.Instances.resolve;
import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Maps.mapOf;
import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.corant.shared.util.Streams;
import org.corant.suites.query.shared.QueryParameter.StreamQueryParameter;
import org.corant.suites.query.shared.mapping.Query.QueryType;

/**
 * corant-suites-query-shared
 *
 * @author bingo 下午5:15:27
 *
 */
public class StreamNamedQueryServices {

  private String queryName;
  private Annotation qualifier;
  private NamedQueryService namedQueryService;
  private StreamQueryParameter parameter = new StreamQueryParameter();

  /**
   * @param queryName
   * @param qualifier
   */
  protected StreamNamedQueryServices(String queryName, Annotation qualifier) {
    super();
    this.queryName = shouldNotBlank(queryName);
    this.qualifier = qualifier;
  }

  /**
   * @param queryName
   * @param namedQueryService
   */
  protected StreamNamedQueryServices(String queryName, NamedQueryService namedQueryService) {
    this.queryName = shouldNotBlank(queryName);
    this.namedQueryService = shouldNotNull(namedQueryService);
  }

  public static StreamNamedQueryServices of(NamedQueryService namedQueryService, String queryName) {
    return new StreamNamedQueryServices(queryName, namedQueryService);
  }

  public static StreamNamedQueryServices of(String queryName, Annotation qualifier) {
    return new StreamNamedQueryServices(queryName, qualifier);
  }

  public static StreamNamedQueryServices ofCas(String clusterName, String queryName) {
    return new StreamNamedQueryServices(queryName,
        NamedQueryServiceManager.resolveQueryService(QueryType.CAS, clusterName));
  }

  public static StreamNamedQueryServices ofEs(String clusterName, String queryName) {
    return new StreamNamedQueryServices(queryName,
        NamedQueryServiceManager.resolveQueryService(QueryType.ES, clusterName));
  }

  public static StreamNamedQueryServices ofMg(String database, String queryName) {
    return new StreamNamedQueryServices(queryName,
        NamedQueryServiceManager.resolveQueryService(QueryType.MG, database));
  }

  public static StreamNamedQueryServices ofSql(String dataSourceWithDialect, String queryName) {
    return new StreamNamedQueryServices(queryName,
        NamedQueryServiceManager.resolveQueryService(QueryType.SQL, dataSourceWithDialect));
  }

  public <T> Stream<List<T>> batch() {
    return Streams.batchStream(parameter.getLimit(), stream());
  }

  public StreamNamedQueryServices context(Map<String, Object> context) {
    parameter.context(context);
    return this;
  }

  /**
   * Key and value pairs
   *
   * @param objects
   */
  public StreamNamedQueryServices context(Object... objects) {
    parameter.context(objects);
    return this;
  }

  public StreamNamedQueryServices criteria(Map<String, Object> context) {
    parameter.criteria(context);
    return this;
  }

  /**
   * Key and value pairs
   *
   * @param objects
   */
  public StreamNamedQueryServices criteria(Object... objects) {
    parameter.criteria(mapOf(objects));
    return this;
  }

  public StreamNamedQueryServices enhancer(BiConsumer<Object, StreamQueryParameter> enhancer) {
    parameter.enhancer(enhancer);
    return this;
  }

  public StreamNamedQueryServices errorTransfer(
      Function<Exception, RuntimeException> errorTransfer) {
    parameter.errorTransfer(errorTransfer);
    return this;
  }

  public StreamNamedQueryServices limit(Integer limit) {
    parameter.limit(limit);
    return this;
  }

  public StreamNamedQueryServices offset(Integer offset) {
    parameter.offset(offset);
    return this;
  }

  public StreamNamedQueryServices retryInterval(Duration retryInterval) {
    parameter.retryInterval(retryInterval);
    return this;
  }

  public StreamNamedQueryServices retryTimes(int retryTimes) {
    parameter.retryTimes(retryTimes);
    return this;
  }

  public <T> Stream<T> stream() {
    if (qualifier == null) {
      return namedQueryService.stream(queryName, parameter);
    }
    return resolve(NamedQueryService.class, qualifier).stream(queryName, parameter);
  }

  public StreamNamedQueryServices terminateByCounter(Predicate<Integer> terminater) {
    return terminater((c, o) -> terminater.test(c));
  }

  public StreamNamedQueryServices terminateByLastRecord(Predicate<Object> terminater) {
    return terminater((c, o) -> terminater.test(o));
  }

  public StreamNamedQueryServices terminater(BiPredicate<Integer, Object> terminater) {
    parameter.terminater(terminater);
    return this;
  }

}
