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
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Objects.asStrings;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.max;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.isBlank;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PreDestroy;
import org.corant.Corant;
import org.corant.modules.query.shared.QueryParameter.StreamQueryParameter;
import org.corant.modules.query.shared.mapping.FetchQuery;
import org.corant.modules.query.shared.mapping.FetchQuery.FetchQueryParameterSource;
import org.corant.modules.query.shared.mapping.Query.QueryType;
import org.corant.shared.util.Retry;

/**
 * corant-modules-query-shared
 *
 * @author bingo 下午4:08:58
 *
 */
public abstract class AbstractNamedQueryService implements NamedQueryService {

  protected static final Map<String, NamedQueryService> fetchQueryServices =
      new ConcurrentHashMap<>();// static?

  protected Logger logger = Logger.getLogger(getClass().getName());

  /**
   * {@inheritDoc}
   * <p>
   * This method use {@link #forward(String, Object)} to fetch next data records.
   * </p>
   *
   * @see AbstractNamedQueryService#stream(String, StreamQueryParameter)
   */
  @Override
  public <T> Stream<T> stream(String queryName, Object parameter) {
    QueryHandler queryResolver = getQuerierResolver().getQueryResolver();
    QueryParameter queryParam = queryResolver.resolveParameter(null, parameter);
    StreamQueryParameter useQueryParam;
    if (queryParam instanceof StreamQueryParameter) {
      useQueryParam = (StreamQueryParameter) queryParam;
    } else {
      useQueryParam = new StreamQueryParameter(queryParam);
    }
    useQueryParam.limit(max(defaultObject(queryParam.getLimit(), Querier.DEFALUT_STREAM_LIMIT), 1));
    return stream(queryName, useQueryParam);
  }

  protected <T> void fetch(List<T> results, Querier querier) {
    List<FetchQuery> fetchQueries;
    if (isNotEmpty(results) && isNotEmpty(fetchQueries = querier.getQuery().getFetchQueries())) {
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
    List<FetchQuery> fetchQueries;
    if (result != null && isNotEmpty(fetchQueries = parentQuerier.getQuery().getFetchQueries())) {
      for (FetchQuery fq : fetchQueries) {
        NamedQueryService fetchQueryService = resolveFetchQueryService(fq);
        if (parentQuerier.decideFetch(result, fq)) {
          fetchQueryService.fetch(result, fq, parentQuerier);
        }
      }
    }
  }

  protected abstract AbstractNamedQuerierResolver<? extends NamedQuerier> getQuerierResolver();

  protected void log(String name, Object param, String... script) {
    logger.fine(() -> String.format(
        "%n[QueryService name]: %s; %n[QueryService parameters]: %s; %n[QueryService script]: %s.",
        name,
        getQuerierResolver().getQueryResolver().getObjectMapper().toJsonString(param, false, true),
        String.join(";\n", script)));
  }

  protected void log(String name, Object[] param, String... script) {
    logger.fine(() -> String.format(
        "%n[QueryService name]: %s; %n[QueryService parameters]: [%s]; %n[QueryService script]: %s.",
        name, String.join(",", asStrings(param)), String.join(";\n", script)));
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
   * Actual execution method for {@link #stream(String, Object)}
   *
   * @param <T>
   * @param queryName
   * @param param
   * @return stream
   */
  protected <T> Stream<T> stream(String queryName, StreamQueryParameter param) {
    return streamOf(new Iterator<T>() {
      Forwarding<T> buffer = null;
      int counter = 0;
      T next = null;

      @Override
      public boolean hasNext() {
        initialize();
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
        initialize();
        if (!buffer.hasResults()) {
          throw new NoSuchElementException();
        }
        counter++;
        next = buffer.getResults().remove(0);
        return next;
      }

      private Forwarding<T> doForward(String queryName, StreamQueryParameter parameter) {
        if (parameter.needRetry()) {
          return Retry.retryer().times(parameter.getRetryTimes())
              .interval(parameter.getRetryInterval())
              .breaker(() -> Corant.current() != null && Corant.current().isRunning())
              .execute(() -> forward(queryName, parameter));
        } else {
          return forward(queryName, parameter);
        }
      }

      private void initialize() {
        if (buffer == null) {
          buffer = defaultObject(doForward(queryName, param), Forwarding::inst);
          counter = buffer.hasResults() ? 1 : 0;
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
