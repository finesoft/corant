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

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.ObjectUtils.asStrings;
import static org.corant.shared.util.ObjectUtils.max;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.NotSupportedException;
import org.corant.suites.query.shared.mapping.FetchQuery;
import org.corant.suites.query.shared.mapping.QueryHint;
import org.corant.suites.query.shared.spi.ResultHintHandler;

/**
 * corant-suites-query-shared
 *
 * @author bingo 下午4:08:58
 *
 */
@ApplicationScoped
public abstract class AbstractNamedQuery implements NamedQuery {

  public static final int MAX_SELECT_SIZE = 128;
  public static final String PRO_KEY_MAX_SELECT_SIZE = ".max-select-size";

  @Inject
  protected Logger logger;

  @Inject
  @Any
  protected Instance<ResultHintHandler> resultHintHandlers;

  public Object adaptiveSelect(String q, Map<String, Object> param) {
    if (param != null && param.containsKey(QueryUtils.OFFSET_PARAM_NME)) {
      if (param.containsKey(QueryUtils.LIMIT_PARAM_NME)) {
        return this.page(q, param);
      } else {
        return this.forward(q, param);
      }
    } else {
      return this.select(q, param);
    }
  }

  @Override
  public <T> Stream<T> stream(String q, Map<String, Object> param) {
    throw new NotSupportedException();
  }

  protected <T> void fetch(List<T> list, List<FetchQuery> fetchQueries, Map<String, Object> param) {
    if (!isEmpty(list) && !isEmpty(fetchQueries)) {
      list.forEach(e -> fetchQueries.forEach(f -> this.fetch(e, f, new HashMap<>(param))));
    }
  }

  protected abstract <T> void fetch(T obj, FetchQuery fetchQuery, Map<String, Object> param);

  protected <T> void fetch(T obj, List<FetchQuery> fetchQueries, Map<String, Object> param) {
    if (obj != null && !isEmpty(fetchQueries)) {
      fetchQueries.forEach(f -> this.fetch(obj, f, new HashMap<>(param)));
    }
  }

  protected int getMaxSelectSize(Querier querier) {
    return max(querier.getProperty(PRO_KEY_MAX_SELECT_SIZE, Integer.class, MAX_SELECT_SIZE),
        Integer.valueOf(1));
  }

  protected void handleResultHints(Class<?> resultClass, List<QueryHint> hints, Object param,
      Object result) {
    if (result != null && !resultHintHandlers.isUnsatisfied()) {
      hints.forEach(qh -> {
        AtomicBoolean exclusive = new AtomicBoolean(false);
        resultHintHandlers.stream().filter(h -> h.canHandle(resultClass, qh))
            .sorted(ResultHintHandler::compare).forEachOrdered(h -> {
              if (!exclusive.get()) {
                try {
                  h.handle(qh, param, result);
                  exclusive.set(h.exclusive());
                } catch (Exception e) {
                  throw new CorantRuntimeException(e);
                }
              }
            });
      });
    }
  }

  protected void log(String name, Map<String, Object> param, String... script) {
    logger.fine(
        () -> String.format("%n[Query name]: %s; %n[Query parameters]: [%s]; %n[Query script]: %s",
            name, String.join(",", asStrings(param)), String.join("; ", script)));
  }

  protected void log(String name, Object[] param, String... script) {
    logger.fine(
        () -> String.format("%n[Query name]: %s; %n[Query parameters]: [%s]; %n[Query script]: %s",
            name, String.join(",", asStrings(param)), String.join("; ", script)));
  }

}
