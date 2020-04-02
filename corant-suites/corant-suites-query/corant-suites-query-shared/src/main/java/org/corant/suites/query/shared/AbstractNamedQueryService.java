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

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.ConversionUtils.toObject;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.ObjectUtils.asStrings;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.shared.util.ObjectUtils.forceCast;
import static org.corant.shared.util.ObjectUtils.max;
import static org.corant.shared.util.StreamUtils.streamOf;
import static org.corant.shared.util.StringUtils.isBlank;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PreDestroy;
import org.corant.kernel.util.Retries.SupplierRetrier;
import org.corant.suites.query.shared.QueryParameter.StreamQueryParameter;
import org.corant.suites.query.shared.mapping.FetchQuery;
import org.corant.suites.query.shared.mapping.FetchQuery.FetchQueryParameterSource;
import org.corant.suites.query.shared.mapping.Query;
import org.corant.suites.query.shared.mapping.Query.QueryType;

/**
 * corant-suites-query-shared
 *
 * @author bingo 下午4:08:58
 *
 */
public abstract class AbstractNamedQueryService implements NamedQueryService {

  public static final int MAX_SELECT_SIZE = 128;
  public static final int DEFAULT_LIMIT = 16;
  public static final String PRO_KEY_MAX_SELECT_SIZE = ".max-select-size";
  public static final String PRO_KEY_DEFAULT_LIMIT = ".default-limit";

  final Map<String, NamedQueryService> fetchQueryServices = new ConcurrentHashMap<>();// static?

  protected Logger logger = Logger.getLogger(getClass().getName());

  /**
   * {@inheritDoc}
   * <p>
   * This method use {@link forward} to fetch next data records.
   * </p>
   *
   * @see AbstractNamedQueryService#stream(String, StreamQueryParameter)
   */
  @Override
  public <T> Stream<T> stream(String queryName, Object parameter) {
    QueryResolver queryResolver = getQuerierResolver().getQueryResolver();
    QueryParameter queryParam = queryResolver.resolveParameter(null, parameter);
    StreamQueryParameter useQueryParam = null;
    if (queryParam instanceof StreamQueryParameter) {
      useQueryParam = (StreamQueryParameter) queryParam;
    } else {
      useQueryParam = new StreamQueryParameter(queryParam);
    }
    useQueryParam.limit(max(defaultObject(queryParam.getLimit(), getDefaultLimit()), 1));
    return stream(queryName, useQueryParam);
  }

  protected <T> void fetch(List<T> results, Querier querier) {
    List<FetchQuery> fetchQueries = querier.getQuery().getFetchQueries();
    if (!isEmpty(results) && !isEmpty(fetchQueries)) {
      for (FetchQuery fq : fetchQueries) {
        NamedQueryService fetchQueryService = resolveFetchQueryService(fq);
        if (fq.isEagerInject()) {
          for (T result : results) {
            if (querier.decideFetch(result, fq)) {
              fetchQueryService.fetch(result, fq, querier);
            }
          }
        } else {
          List<T> decideResults =
              results.stream().filter(r -> querier.decideFetch(r, fq)).collect(Collectors.toList());
          if (isEmpty(decideResults) && isNotEmpty(fq.getParameters())
              && fq.getParameters().stream()
                  .noneMatch(fp -> fp.getSource() == FetchQueryParameterSource.C
                      || fp.getSource() == FetchQueryParameterSource.P)) {
            continue;
          }
          fetchQueryService.fetch(decideResults, fq, querier);
        }
      }
    }
  }

  protected <T> void fetch(T result, Querier parentQuerier) {
    List<FetchQuery> fetchQueries = parentQuerier.getQuery().getFetchQueries();
    if (result != null && !isEmpty(fetchQueries)) {
      for (FetchQuery fq : fetchQueries) {
        NamedQueryService fetchQueryService = resolveFetchQueryService(fq);
        if (parentQuerier.decideFetch(result, fq)) {
          fetchQueryService.fetch(result, fq, parentQuerier);
        }
      }
    }
  }

  protected int getDefaultLimit() {
    return DEFAULT_LIMIT;
  }

  protected int getDefaultMaxSelectSize() {
    return MAX_SELECT_SIZE;
  }

  protected abstract AbstractNamedQuerierResolver<? extends NamedQuerier> getQuerierResolver();

  protected void log(String name, Object param, String... script) {
    logger.fine(() -> String.format(
        "%n[QueryService name]: %s; %n[QueryService parameters]: %s; %n[QueryService script]: %s",
        name, QueryObjectMapper.toString(param), String.join(";\n", script)));
  }

  protected void log(String name, Object[] param, String... script) {
    logger.fine(() -> String.format(
        "%n[QueryService name]: %s; %n[QueryService parameters]: [%s]; %n[QueryService script]: %s",
        name, String.join(",", asStrings(param)), String.join(";\n", script)));
  }

  /**
   * Resolve default limit from query parameter context or query object
   *
   * @param querier
   * @return resolveDefaultLimit
   */
  protected int resolveDefaultLimit(Querier querier) {
    int limit = resolveProperties(querier, PRO_KEY_DEFAULT_LIMIT, Integer.class, getDefaultLimit());
    return limit <= 0 ? Integer.MAX_VALUE : limit;
  }

  protected NamedQueryService resolveFetchQueryService(final FetchQuery fq) {
    final QueryType type = fq.getReferenceQuery().getType();
    final String qualifier = fq.getReferenceQuery().getQualifier();
    return fetchQueryServices.computeIfAbsent(fq.getId(), id -> {
      if (type == null && isBlank(qualifier)) {
        return this;
      } else {
        return shouldNotNull(NamedQueryServiceManager.resolveQueryService(type, qualifier),
            "Can't find any query service to execute fetch query [%s]", fq.getReferenceQuery());
      }
    });
  }

  /**
   * Resolve limit from query parameter or query object, if the resolved limit <=0 then return
   * Integer.MAX_VALUE.
   *
   * NOTE: the resolved limit can not great than the max select size.
   *
   * @param querier
   * @return resolveLimit
   */
  protected int resolveLimit(Querier querier) {
    int limit = defaultObject(querier.getQueryParameter().getLimit(), resolveDefaultLimit(querier));
    int max = resolveMaxSelectSize(querier);
    if (limit > max) {
      throw new QueryRuntimeException(
          "Exceeded the maximum number of query [%s] results, limit is [%s].",
          querier.getQuery().getName(), max);
    }
    return limit;
  }

  /**
   * Resolve max select size from query parameter context or query object, if the resolved size <=0
   * then return Integer.MAX_VALUE.
   *
   * @param querier
   * @return resolveMaxSelectSize
   * @see AbstractNamedQueryService#resolveProperties(Querier, String, Class, Object)
   * @see #MAX_SELECT_SIZE
   * @see #PRO_KEY_MAX_SELECT_SIZE
   */
  protected int resolveMaxSelectSize(Querier querier) {
    int mss = resolveProperties(querier, PRO_KEY_MAX_SELECT_SIZE, Integer.class,
        getDefaultMaxSelectSize());
    return mss <= 0 ? Integer.MAX_VALUE : mss;
  }

  /**
   * Resolve offset from query parameter, if the resolved offset < 0 or offset is null then return
   * 0.
   *
   * @param querier
   * @return resolveOffset
   */
  protected int resolveOffset(Querier querier) {
    return max(defaultObject(querier.getQueryParameter().getOffset(), 0), 0);
  }

  /**
   * Resolve properties from querier, first we try resolve it from the query parameter context, if
   * not found try resolve it from query properties.
   *
   * @param <P> the property
   * @param querier the querier
   * @param key the property key
   * @param cls the property value class
   * @param dflt the default value if property not set
   * @return resolveProperties
   *
   * @see QueryParameter#getContext()
   * @see Query#getProperties()
   */
  protected <P> P resolveProperties(Querier querier, String key, Class<P> cls, P dflt) {
    P obj = toObject(querier.getQueryParameter().getContext().get(key), cls);
    if (obj == null) {
      obj = querier.getQuery().getProperty(key, cls);
    }
    return defaultObject(obj, dflt);
  }

  /**
   * Actual execution method for {@link #stream(String, Object)}
   *
   * @param <T>
   * @param queryName
   * @param param
   * @return stream
   */
  protected <T> Stream<T> stream(String queryName, StreamQueryParameter param) {
    return streamOf(new Iterator<T>() {
      final Forwarding<T> buffer = doForward(queryName, param);
      int counter = 1;
      T next = null;

      @Override
      public boolean hasNext() {
        if (!param.terminateIf(counter, next)) {
          if (!buffer.hasResults()) {
            if (buffer.hasNext()) {
              buffer.with(doForward(queryName, param.forward(next)));
              return buffer.hasResults();
            }
          } else {
            return true;
          }
        }
        return false;
      }

      @Override
      public T next() {
        if (!buffer.hasResults()) {
          throw new NoSuchElementException();
        }
        counter++;
        next = buffer.getResults().remove(0);
        return next;
      }

      Forwarding<T> doForward(String queryName, StreamQueryParameter parameter) {
        if (parameter.needRetry()) {
          return forceCast(new SupplierRetrier<>(() -> forward(queryName, parameter))
              .times(parameter.getRetryTimes()).interval(parameter.getRetryInterval())
              .transfer(parameter.getErrorTransfer()).execute());
        } else {
          return forward(queryName, parameter);
        }
      }
    });
  }

  @PreDestroy
  synchronized void onPreDestroy() {
    fetchQueryServices.clear();
    logger.fine(() -> "Clear named query service cached fetch query services.");
  }
}
