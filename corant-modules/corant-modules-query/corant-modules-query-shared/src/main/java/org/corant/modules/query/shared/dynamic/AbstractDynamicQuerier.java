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

import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Objects.max;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.corant.modules.query.FetchQueryHandler;
import org.corant.modules.query.QuerierConfig;
import org.corant.modules.query.QueryHandler;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.FetchQuery;
import org.corant.modules.query.mapping.Query;

/**
 * corant-modules-query-shared
 *
 * @author bingo 下午5:56:22
 *
 */
public abstract class AbstractDynamicQuerier<P, S> implements DynamicQuerier<P, S> {

  protected static final Logger logger = Logger.getLogger(AbstractDynamicQuerier.class.getName());

  protected final Query query;
  protected final QueryHandler queryHandler;
  protected final FetchQueryHandler fetchQueryHandler;
  protected final QueryParameter queryParameter;
  final QuerierConfig config;

  protected volatile Integer limit;
  protected volatile Integer streamLimit;
  protected volatile Integer maxSelectSize;
  protected volatile Integer offset;
  protected volatile Boolean thrownExceedMaxSelectSize;
  protected volatile Duration timeout;

  protected AbstractDynamicQuerier(Query query, QueryParameter queryParameter,
      QueryHandler queryHandler, FetchQueryHandler fetchQueryHandler) {
    this.query = query;
    this.queryParameter = queryParameter;
    this.queryHandler = queryHandler;
    this.fetchQueryHandler = fetchQueryHandler;
    this.config = queryHandler.getQuerierConfig();
  }

  @Override
  public boolean decideFetch(Object result, FetchQuery fetchQuery) {
    return fetchQueryHandler.canFetch(result, queryParameter, fetchQuery);
  }

  @Override
  public String getName() {
    return query.getName();
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
  public void handleFetchedResult(Object result, List<?> fetchResult, FetchQuery fetchQuery) {
    fetchQueryHandler.handleFetchedResult(queryParameter, result, fetchResult, fetchQuery);
  }

  @Override
  public void handleFetchedResults(List<?> results, List<?> fetchResult, FetchQuery fetchQuery) {
    fetchQueryHandler.handleFetchedResults(queryParameter, results, fetchResult, fetchQuery);
  }

  @Override
  public <T> T handleResult(Object result) {
    if (result == null) {
      return null;
    } else {
      handleResultHints(result);
      return queryHandler.handleResult(result, getQuery(), getQueryParameter());
    }
  }

  @Override
  public void handleResultHints(Object result) {
    // FIXME map class
    queryHandler.handleResultHints(result, Map.class, getQuery(), getQueryParameter());
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> List<T> handleResults(List<?> results) {
    if (isNotEmpty(results)) {
      handleResultHints(results);
    }
    return queryHandler.handleResults((List<Object>) results, getQuery(), getQueryParameter());
  }

  @Override
  public int handleResultSize(List<?> results) {
    int size = sizeOf(results);
    final int maxSize;
    if (size > 0 && size > (maxSize = resolveMaxSelectSize())) {
      if (thrownExceedMaxSelectSize()) {
        throw new QueryRuntimeException(
            "The size of query[%s] result is exceeded, the allowable range is %s.", getName(),
            maxSize);
      } else {
        logger.warning(String.format(
            "The size of query[%s] result is exceeded, the allowable range is %s, the excess records are silently dropped.",
            query.getName(), maxSize));
        do {
          results.remove(--size);
        } while (size > maxSize);
      }
    }
    return size;
  }

  @Override
  public QueryParameter resolveFetchQueryParameter(Object result, FetchQuery fetchQuery) {
    return fetchQueryHandler.resolveFetchQueryParameter(result, fetchQuery, getQueryParameter());
  }

  @Override
  public int resolveLimit() {
    if (limit == null) {
      synchronized (this) {
        if (limit == null) {
          if ((limit = defaultObject(getQueryParameter().getLimit(),
              () -> resolveProperty(QuerierConfig.PRO_KEY_LIMIT, Integer.class,
                  config.getDefaultLimit()))) <= 0) {
            limit = config.getMaxLimit();
          }
          int max = resolveMaxSelectSize();
          if (limit > max) {
            throw new QueryRuntimeException(
                "Exceeded the maximum number of query [%s] results, limit is [%s].", getName(),
                max);
          }
        }
      }
    }
    return limit;
  }

  @Override
  public int resolveMaxFetchSize(Object parentResult, FetchQuery fetchQuery) {
    int maxFetchSize = fetchQuery.getMaxSize();
    if (maxFetchSize > 0 && parentResult instanceof Collection) {
      maxFetchSize = maxFetchSize * ((Collection<?>) parentResult).size();
    }
    return maxFetchSize;
  }

  @Override
  public int resolveMaxSelectSize() {
    if (maxSelectSize == null) {
      synchronized (this) {
        if (maxSelectSize == null
            && (maxSelectSize = resolveProperty(QuerierConfig.PRO_KEY_MAX_SELECT_SIZE,
                Integer.class, config.getDefaultSelectSize())) <= 0) {
          maxSelectSize = max(config.getMaxSelectSize(), 0);
        }
      }
    }
    return maxSelectSize;
  }

  /**
   * Resolve offset from query parameter, if the resolved offset < 0 or offset is null then return
   * 0.
   */
  @Override
  public int resolveOffset() {
    if (offset == null) {
      synchronized (this) {
        if (offset == null) {
          offset = max(getQueryParameter().getOffset(), 0);
        }
      }
    }
    return offset;
  }

  /**
   * Resolve properties from querier, first we try resolve it from the query parameter context, if
   * not found try resolve it from query properties.
   *
   * @param <X> the property value type
   * @param key the property key
   * @param cls the property value class
   * @param dflt the default value if property not set
   * @return the property
   *
   * @see QueryParameter#getContext()
   * @see Query#getProperties()
   */
  @Override
  public <X> X resolveProperty(String key, Class<X> cls, X dflt) {
    Object pro =
        defaultObject(getQueryParameter().getContext().get(key), () -> getQuery().getProperty(key));
    if (pro != null) {
      if (cls.isInstance(pro)) {
        return forceCast(pro);
      }
      return defaultObject(toObject(pro, cls), dflt);
    }
    return dflt;
  }

  @Override
  public int resolveStreamLimit() {
    if (streamLimit == null) {
      synchronized (this) {
        if (streamLimit == null) {
          if ((streamLimit = defaultObject(getQueryParameter().getLimit(),
              () -> resolveProperty(QuerierConfig.PRO_KEY_STREAM_LIMIT, Integer.class,
                  config.getDefaultStreamLimit()))) <= 0) {
            streamLimit = config.getMaxLimit();
          }
          int max = resolveMaxSelectSize();
          if (streamLimit > max) {
            throw new QueryRuntimeException(
                "Exceeded the maximum number of query [%s] results, limit is [%s].", getName(),
                max);
          }
        }
      }
    }
    return streamLimit;
  }

  @Override
  public Duration resolveTimeout() {
    if (timeout == null) {
      synchronized (this) {
        if (timeout == null) {
          timeout = defaultObject(
              resolveProperty(QuerierConfig.PRO_KEY_TIMEOUT, Duration.class, config.getTimeout()),
              () -> Duration.ZERO);
        }
      }
    }
    return timeout.equals(Duration.ZERO) ? null : timeout;
  }

  protected int getUnLimitSize() {
    return QuerierConfig.UN_LIMIT_SELECT_SIZE;
  }

  boolean thrownExceedMaxSelectSize() {
    if (this.thrownExceedMaxSelectSize == null) {
      this.thrownExceedMaxSelectSize =
          resolveProperty(QuerierConfig.PRO_KEY_THROWN_ON_MAX_LIMIT_SIZE, Boolean.class,
              config.isThrownOnMaxSelectSize());
    }
    return thrownExceedMaxSelectSize;
  }
}
