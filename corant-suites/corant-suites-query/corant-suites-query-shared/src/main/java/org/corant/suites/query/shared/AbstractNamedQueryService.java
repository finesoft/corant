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

import static org.corant.shared.util.ConversionUtils.toBoolean;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.ObjectUtils.asStrings;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.shared.util.ObjectUtils.max;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.corant.shared.exception.NotSupportedException;
import org.corant.suites.query.shared.dynamic.javascript.NashornScriptEngines;
import org.corant.suites.query.shared.dynamic.javascript.NashornScriptEngines.ScriptFunction;
import org.corant.suites.query.shared.mapping.FetchQuery;

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

  protected Logger logger = Logger.getLogger(getClass().getName());

  @Override
  public <T> Stream<T> stream(String queryName, Object parameter) {
    throw new NotSupportedException();
  }

  protected boolean decideFetch(Object result, FetchQuery fetchQuery, Object criteria) {
    // precondition to decide whether execute fetch.
    if (fetchQuery.getPredicateScript().isValid()) {
      ScriptFunction sf =
          NashornScriptEngines.compileFunction(fetchQuery.getPredicateScript().getCode(), "p", "r");
      if (sf != null) {
        Boolean b = toBoolean(sf.apply(new Object[] {criteria, result}));
        if (b == null || !b.booleanValue()) {
          return false;
        }
      }
    }
    return true;
  }

  protected <T> void fetch(List<T> results, Querier querier) {
    List<FetchQuery> fetchQueries = querier.getQuery().getFetchQueries();
    if (!isEmpty(results) && !isEmpty(fetchQueries)) {
      fetchQueries.forEach(f -> results.forEach(e -> this.fetch(e, f, querier)));
    }
  }

  protected abstract <T> void fetch(T result, FetchQuery fetchQuery, Querier parentQuerier);

  protected <T> void fetch(T result, Querier parentQuerier) {
    List<FetchQuery> fetchQueries = parentQuerier.getQuery().getFetchQueries();
    if (result != null && !isEmpty(fetchQueries)) {
      fetchQueries.forEach(f -> this.fetch(result, f, parentQuerier));
    }
  }

  protected void log(String name, Object param, String... script) {
    logger.fine(() -> String.format(
        "%n[QueryService name]: %s; %n[QueryService parameters]: %s; %n[QueryService script]: %s",
        name, QueryObjectMapper.toString(param), String.join("; ", script)));
  }

  protected void log(String name, Object[] param, String... script) {
    logger.fine(() -> String.format(
        "%n[QueryService name]: %s; %n[QueryService parameters]: [%s]; %n[QueryService script]: %s",
        name, String.join(",", asStrings(param)), String.join("; ", script)));
  }

  protected int resolveDefaultLimit(Querier querier) {
    return querier.getQuery().getProperty(PRO_KEY_DEFAULT_LIMIT, Integer.class, DEFAULT_LIMIT);
  }

  protected int resolveLimit(Querier querier) {
    int limit = defaultObject(querier.getQueryParameter().getLimit(), resolveDefaultLimit(querier));
    int max = resolveMaxSelectSize(querier);
    if (limit > max) {
      throw new QueryRuntimeException(
          "Exceeded the maximum number of query [%s] results, limit is [%S].",
          querier.getQuery().getName(), max);
    }
    return limit;
  }

  protected int resolveMaxSelectSize(Querier querier) {
    return querier.getQuery().getProperty(PRO_KEY_MAX_SELECT_SIZE, Integer.class, MAX_SELECT_SIZE);
  }

  protected int resolveOffset(Querier querier) {
    return max(defaultObject(querier.getQueryParameter().getOffset(), 0), 0);
  }
}
