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
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Lists.transform;
import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Objects.defaultObject;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.corant.modules.query.NamedQueryService;
import org.corant.modules.query.QueryObjectMapper;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.QueryParameter.DefaultQueryParameter;
import org.corant.modules.query.QueryService.Forwarding;
import org.corant.modules.query.QueryService.Paging;
import org.corant.modules.query.mapping.Query.QueryType;
import org.corant.shared.ubiquity.TypeLiteral;
import org.corant.shared.util.Streams;

/**
 * corant-modules-query-shared
 *
 * @author bingo 下午5:15:27
 */
public class NamedQueryServices {

  private String queryName;
  private Annotation qualifier;
  private NamedQueryService namedQueryService;
  private DefaultQueryParameter parameter = new DefaultQueryParameter();

  /**
   * @param queryName the query name
   * @param qualifier the named query service qualifier
   */
  protected NamedQueryServices(String queryName, Annotation qualifier) {
    this.queryName = shouldNotBlank(queryName);
    this.qualifier = qualifier;
  }

  /**
   * @param queryName the query name
   * @param namedQueryService the named query service
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
   * @param objects the context of query parameter
   */
  public NamedQueryServices context(Object... objects) {
    parameter.context(objects);
    return this;
  }

  /**
   * Criteria map
   *
   * @param criteria the criteria of query parameter
   * @return criteria
   */
  public NamedQueryServices criteria(Map<String, Object> criteria) {
    parameter.criteria(criteria);
    return this;
  }

  /**
   * Key and value pairs
   *
   * @param objects the criteria of query parameter, use 'key,value' pairs
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

  /**
   * corant-modules-query-shared
   * <p>
   * TODO FIXME unfinished yet!
   *
   * @author bingo 12:00:43
   */
  public static class NamedQueryActuator<R, X> {

    protected String name;
    protected DefaultQueryParameter queryParameter = new DefaultQueryParameter();
    protected Class<R> finalResultClass;
    protected Class<X> resultClass;
    protected Function<X, R> converter;
    protected List<NamedFetchQueryActuator<R, X, ?>> fetchBuilders = new ArrayList<>();

    public static <T> NamedQueryActuator<T, Map<String, Object>> of(String name, Class<T> finalResultClass) {
      NamedQueryActuator<T, Map<String, Object>> b = new NamedQueryActuator<>();
      b.name = name;
      b.finalResultClass = finalResultClass;
      b.resultClass = TypeLiteral.DOC_TYPE.getRawType();
      return b;
    }

    public static <T1, T2> NamedQueryActuator<T1, T2> of(String name, Class<T1> finalResultClass,
        Class<T2> resultClass) {
      NamedQueryActuator<T1, T2> b = new NamedQueryActuator<>();
      b.name = name;
      b.finalResultClass = finalResultClass;
      b.resultClass = resultClass;
      return b;
    }

    public NamedQueryActuator<R, X> context(Object... keyValues) {
      queryParameter.context(mapOf(keyValues));
      return this;
    }

    public NamedQueryActuator<R, X> convert(Function<X, R> converter) {
      this.converter = converter;
      return this;
    }

    public NamedQueryActuator<R, X> criteria(Object... keyValues) {
      queryParameter.criteria(mapOf(keyValues));
      return this;
    }

    public NamedFetchQueryActuator<R, X, Map<String, Object>> fetch(String name) {
      NamedFetchQueryActuator<R, X, Map<String, Object>> b =
          new NamedFetchQueryActuator<>(name, resultClass, TypeLiteral.DOC_TYPE.getRawType());
      fetchBuilders.add(b);
      return b;
    }

    public <FR> NamedFetchQueryActuator<R, X, FR> fetch(String name, Class<FR> fetchResultClass) {
      NamedFetchQueryActuator<R, X, FR> b = new NamedFetchQueryActuator<>(name, resultClass, fetchResultClass);
      fetchBuilders.add(b);
      return b;
    }

    public R get() {
      X result = resolve(NamedQueryService.class).get(name, queryParameter);
      if (result != null && isNotEmpty(fetchBuilders)) {
        List<X> results = listOf(result);
        fetch(results);
        return converter.apply(results.get(0));
      }
      return converter.apply(result);
    }

    public NamedQueryActuator<R, X> limit(int limit) {
      queryParameter.limit(limit);
      return this;
    }

    public NamedQueryActuator<R, X> offset(int offset) {
      queryParameter.offset(offset);
      return this;
    }

    public List<R> select() {
      resolveParameter();
      List<X> results = resolve(NamedQueryService.class).select(name, queryParameter);
      if (isNotEmpty(results)) {
        if (isNotEmpty(fetchBuilders)) {
          fetch(results);
        }
        return transform(results, converter);
      }
      return new ArrayList<>();
    }

    protected void fetch(List<X> results) {
      if (fetchBuilders.isEmpty()) {
        return;
      }
      for (NamedFetchQueryActuator<R, X, ?> fb : fetchBuilders) {
        fb.fetch(results, queryParameter);
      }
    }

    protected void resolveParameter() {
      Map<String, Object> context = queryParameter.getContext();
      context.put(QueryParameter.CTX_QHH_DONT_CONVERT_RESULT, true);
      queryParameter.context(context);
    }
  }

  /**
   * corant-modules-query-shared
   * <p>
   * TODO FIXME unfinished yet!
   *
   * @author bingo 14:41:07
   */
  public static class NamedFetchQueryActuator<PFR, PR, FR> {
    protected String name;
    protected Class<PR> parentResultClass;
    protected Class<FR> resultClass;
    protected BiFunction<List<PR>, QueryParameter, QueryParameter> parameterResolver;
    protected BiPredicate<PR, QueryParameter> predicate;
    protected BiConsumer<List<PR>, List<FR>> injector;
    protected List<NamedFetchQueryActuator<FR, FR, ?>> fetchBuilders = new ArrayList<>();

    NamedFetchQueryActuator(String name, Class<PR> parentResultClass, Class<FR> resultClass) {
      this.name = name;
      this.parentResultClass = parentResultClass;
      this.resultClass = resultClass;
    }

    public <FFR> NamedFetchQueryActuator<FR, FR, FFR> fetch(String name, Class<FFR> fetchResultClass) {
      NamedFetchQueryActuator<FR, FR, FFR> b = new NamedFetchQueryActuator<>(name, resultClass, fetchResultClass);
      fetchBuilders.add(b);
      return b;
    }

    public NamedFetchQueryActuator<PFR, PR, FR> injector(BiConsumer<List<PR>, List<FR>> injector) {
      this.injector = injector;
      return this;
    }

    public NamedFetchQueryActuator<PFR, PR, FR> parameterResolver(
        BiFunction<List<PR>, QueryParameter, QueryParameter> parameterResolver) {
      this.parameterResolver = parameterResolver;
      return this;
    }

    public NamedFetchQueryActuator<PFR, PR, FR> predicate(BiPredicate<PR, QueryParameter> predicate) {
      this.predicate = predicate;
      return this;
    }

    protected void fetch(List<PR> parentResults, QueryParameter parentQueryParameter) {
      List<PR> filterResults = parentResults.stream()
          .filter(r -> predicate.test(r, parentQueryParameter)).collect(Collectors.toList());
      if (isNotEmpty(filterResults)) {
        QueryParameter fetchQueryParam =
            parameterResolver.apply(filterResults, parentQueryParameter);
        final List<FR> fetchResults =
            resolve(NamedQueryService.class).select(name, fetchQueryParam);
        if (isNotEmpty(fetchResults)) {
          if (isNotEmpty(fetchBuilders)) {
            for (NamedFetchQueryActuator<FR, FR, ?> fb : fetchBuilders) {
              fb.fetch(fetchResults, parentQueryParameter);
            }
          }
          injector.accept(parentResults, fetchResults);
        }
      }
    }
  }
}
