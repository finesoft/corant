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

import static java.util.stream.Collectors.toList;
import static org.corant.context.Beans.resolve;
import static org.corant.shared.util.Assertions.shouldInstanceOf;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Objects.asStrings;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.max;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.isBlank;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.annotation.PreDestroy;
import org.corant.Corant;
import org.corant.modules.query.NamedQuerier;
import org.corant.modules.query.Querier;
import org.corant.modules.query.QueryObjectMapper;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.StreamQueryParameter;
import org.corant.modules.query.mapping.FetchQuery;
import org.corant.modules.query.mapping.FetchQuery.FetchQueryParameterSource;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.mapping.Query.QueryType;
import org.corant.modules.query.shared.dynamic.DynamicQuerier;
import org.corant.shared.retry.RetryStrategy.MaxAttemptsRetryStrategy;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.ubiquity.Tuple.Triple;
import org.corant.shared.util.Retry;

/**
 * corant-modules-query-shared
 *
 * @author bingo 下午4:08:58
 *
 */
public abstract class AbstractNamedQueryService implements FetchableNamedQueryService {

  protected static final Map<String, FetchableNamedQueryService> fetchQueryServices =
      new ConcurrentHashMap<>();// static?

  protected Logger logger = Logger.getLogger(getClass().getName());

  @Override
  public <T> Forwarding<T> forward(String q, Object p) {
    try {
      return doForward(q, p);
    } catch (Exception e) {
      throw new QueryRuntimeException(e,
          "An error occurred while executing the forward query [%s]!", q);
    }
  }

  @Override
  public <T> T get(String q, Object p) {
    try {
      return doGet(q, p);
    } catch (Exception e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the get query [%s]!",
          q);
    }
  }

  @Override
  public QueryObjectMapper getObjectMapper() {
    return resolve(QueryObjectMapper.class);
  }

  @Override
  public void handleFetching(Object results, Querier parentQuerier) {
    if (results == null) {
      return;
    }
    List<FetchQuery> fetchQueries = parentQuerier.getQuery().getFetchQueries();
    if (isNotEmpty(fetchQueries)) {
      if (parentQuerier.parallelFetch() && fetchQueries.size() > 1) {
        if (results instanceof List) {
          parallelFetch((List<?>) results, parentQuerier);
        } else {
          parallelFetch(results, parentQuerier);
        }
      } else if (results instanceof List) {
        serialFetch((List<?>) results, parentQuerier);
      } else {
        serialFetch(results, parentQuerier);
      }
    }
  }

  /**
   * {@inheritDoc}}
   * <p>
   * Note: In some implementations that require a separate total record query, if the offset exceeds
   * the total number of results in the query result set, the total number of returned paging result
   * set results is 0.
   * </p>
   */
  @Override
  public <T> Paging<T> page(String q, Object p) {
    try {
      return doPage(q, p);
    } catch (Exception e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the page query [%s]!",
          q);
    }
  }

  @Override
  public <T> List<T> select(String q, Object p) {
    try {
      return doSelect(q, p);
    } catch (Exception e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the select query [%s]",
          q);
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   * This method use {@link #forward(String, Object)} to fetch next data records.
   * </p>
   *
   * @see AbstractNamedQueryService#doStream(String, StreamQueryParameter)
   */
  @Override
  public <T> Stream<T> stream(String queryName, Object parameter) {
    DynamicQuerier<?, ?> querier = getQuerierResolver().resolve(queryName, parameter);
    QueryParameter queryParam = querier.getQueryParameter();
    StreamQueryParameter useQueryParam;
    if (queryParam instanceof StreamQueryParameter) {
      useQueryParam = (StreamQueryParameter) queryParam;
    } else {
      useQueryParam = new StreamQueryParameter(queryParam);
    }
    useQueryParam.limit(max(querier.resolveStreamLimit(), 1));
    return doStream(queryName, useQueryParam);
  }

  protected abstract <T> Forwarding<T> doForward(String q, Object p) throws Exception;

  protected abstract <T> T doGet(String q, Object p) throws Exception;

  protected abstract <T> Paging<T> doPage(String q, Object p) throws Exception;

  protected abstract <T> List<T> doSelect(String q, Object p) throws Exception;

  /**
   * Actual execution method for {@link #stream(String, Object)}
   *
   * @param <T> the result record type
   * @param queryName the query name
   * @param param the query parameter
   * @return stream the query result stream
   */
  protected <T> Stream<T> doStream(String queryName, StreamQueryParameter param) {
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
          return Retry.synchronousRetryer()
              .retryStrategy(new MaxAttemptsRetryStrategy(parameter.getRetryTimes() + 1))
              .backoffStrategy(parameter.getRetryBackoffStrategy())
              .retryPrecondition(c -> Corant.current() != null && Corant.current().isRunning())
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

  protected abstract AbstractNamedQuerierResolver<? extends NamedQuerier> getQuerierResolver();

  protected void log(String name, Object param, String... script) {
    logger.fine(() -> String.format(
        "%n[QueryService name]: %s; %n[QueryService parameters]: %s; %n[QueryService script]: %s.",
        name,
        getQuerierResolver().getQueryHandler().getObjectMapper().toJsonString(param, false, true),
        String.join("\n", script)));
  }

  protected void log(String name, Object[] param, String... script) {
    logger.fine(() -> String.format(
        "%n[QueryService name]: %s; %n[QueryService parameters]: [%s]; %n[QueryService script]: %s.",
        name, String.join(",", asStrings(param)), String.join("\n", script)));
  }

  protected <T> void parallelFetch(List<T> results, Querier parentQuerier) {
    // parallel fetch, experimental features for proof of concept.
    List<FetchQuery> fetchQueries = new ArrayList<>(parentQuerier.getQuery().getFetchQueries());
    final Collection<Triple<FetchedResult, Object, FetchableNamedQueryService>> workResults =
        new LinkedBlockingQueue<>();
    fetchQueries.parallelStream().forEach(fq -> {
      final FetchableNamedQueryService fqs = resolveFetchQueryService(fq);
      if (fq.isEagerInject()) {
        for (T result : results) {
          if (parentQuerier.decideFetch(result, fq)) {
            FetchedResult fr = fqs.fetch(result, fq, parentQuerier);
            workResults.add(Triple.of(fr, result, fqs));
          }
        }
      } else {
        List<T> decideResults =
            results.stream().filter(r -> parentQuerier.decideFetch(r, fq)).collect(toList());
        boolean fetch = true;
        if (isEmpty(decideResults) && isNotEmpty(fq.getParameters())
            && fq.getParameters().stream()
                .noneMatch(fp -> fp.getSource() == FetchQueryParameterSource.C
                    || fp.getSource() == FetchQueryParameterSource.P)) {
          fetch = false;
        }
        if (fetch) {
          FetchedResult fr = fqs.fetch(decideResults, fq, parentQuerier);
          workResults.add(Triple.of(fr, decideResults, fqs));
        }
      }
    });

    for (FetchQuery fq : fetchQueries) {
      Iterator<Triple<FetchedResult, Object, FetchableNamedQueryService>> it =
          workResults.iterator();
      while (it.hasNext()) {
        Triple<FetchedResult, Object, FetchableNamedQueryService> pair = it.next();
        if (areEqual(pair.left().fetchQuery, fq)) {
          postFetch(pair.getRight(), pair.left(), parentQuerier, pair.middle());
          it.remove();
        }
      }
    }
  }

  protected <T> void parallelFetch(T result, Querier parentQuerier) {
    // parallel fetch, experimental functions for proof of concept.
    List<FetchQuery> fetchQueries = new ArrayList<>(parentQuerier.getQuery().getFetchQueries());
    Collection<Pair<FetchedResult, FetchableNamedQueryService>> workResults =
        new LinkedBlockingQueue<>();
    fetchQueries.parallelStream().forEach(fq -> {
      FetchableNamedQueryService fetchQueryService = resolveFetchQueryService(fq);
      if (parentQuerier.decideFetch(result, fq)) {
        FetchedResult fr = fetchQueryService.fetch(result, fq, parentQuerier);
        workResults.add(Pair.of(fr, fetchQueryService));
      }
    });
    for (FetchQuery fq : fetchQueries) {
      Iterator<Pair<FetchedResult, FetchableNamedQueryService>> it = workResults.iterator();
      while (it.hasNext()) {
        Pair<FetchedResult, FetchableNamedQueryService> fr = it.next();
        if (areEqual(fr.left().fetchQuery, fq)) {
          postFetch(fr.right(), fr.left(), parentQuerier, result);
          it.remove();
        }
      }
    }
  }

  protected void postFetch(FetchableNamedQueryService service, FetchedResult fetchedResult,
      Querier parentQuerier, Object result) {
    if (fetchedResult != null && isNotEmpty(fetchedResult.fetchedList)) {
      service.handleFetching(fetchedResult.fetchedList, fetchedResult.fetchQuerier);// Next fetch
      fetchedResult.fetchQuerier.handleResultHints(fetchedResult.fetchedList);
      if (result instanceof List) {
        parentQuerier.handleFetchedResults((List<?>) result, fetchedResult.fetchedList,
            fetchedResult.fetchQuery);
      } else {
        parentQuerier.handleFetchedResult(result, fetchedResult.fetchedList,
            fetchedResult.fetchQuery);
      }
    }
  }

  protected FetchableNamedQueryService resolveFetchQueryService(final FetchQuery fq) {
    return fetchQueryServices.computeIfAbsent(fq.getId(), id -> {
      final Query query =
          resolve(QueryMappingService.class).getQuery(fq.getReferenceQuery().getVersionedName());
      final QueryType type = defaultObject(fq.getReferenceQuery().getType(), query.getType());
      final String qualifier =
          defaultObject(fq.getReferenceQuery().getQualifier(), query.getQualifier());
      if (type == null && isBlank(qualifier)) {
        return this;
      } else {
        return shouldInstanceOf(NamedQueryServiceManager.resolveQueryService(type, qualifier),
            FetchableNamedQueryService.class,
            "Can't find any query service to execute fetch query [%s]", fq.getReferenceQuery());
      }
    });
  }

  protected <T> void serialFetch(List<T> results, Querier parentQuerier) {
    for (FetchQuery fq : parentQuerier.getQuery().getFetchQueries()) {
      FetchableNamedQueryService fetchQueryService = resolveFetchQueryService(fq);
      if (fq.isEagerInject()) {
        for (T result : results) {
          if (parentQuerier.decideFetch(result, fq)) {
            FetchedResult fr = fetchQueryService.fetch(result, fq, parentQuerier);
            postFetch(fetchQueryService, fr, parentQuerier, result);
          }
        }
      } else {
        List<T> decideResults =
            results.stream().filter(r -> parentQuerier.decideFetch(r, fq)).collect(toList());
        if (isEmpty(decideResults) && isNotEmpty(fq.getParameters())
            && fq.getParameters().stream()
                .noneMatch(fp -> fp.getSource() == FetchQueryParameterSource.C
                    || fp.getSource() == FetchQueryParameterSource.P)) {
          continue;
        }
        FetchedResult fr = fetchQueryService.fetch(decideResults, fq, parentQuerier);
        postFetch(fetchQueryService, fr, parentQuerier, decideResults);
      }
    }
  }

  protected <T> void serialFetch(T result, Querier parentQuerier) {
    for (FetchQuery fq : parentQuerier.getQuery().getFetchQueries()) {
      FetchableNamedQueryService fetchQueryService = resolveFetchQueryService(fq);
      if (parentQuerier.decideFetch(result, fq)) {
        FetchedResult fr = fetchQueryService.fetch(result, fq, parentQuerier);
        postFetch(fetchQueryService, fr, parentQuerier, result);
      }
    }
  }

  @PreDestroy
  synchronized void onPreDestroy() {
    fetchQueryServices.clear();
    logger.fine(() -> "Clear named query service cached fetch query services.");
  }
}
