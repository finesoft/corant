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
package org.corant.suites.query.esquery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.corant.suites.query.esquery.EsInLineNamedQueryResolver.Querier;
import org.corant.suites.query.mapping.FetchQuery;

/**
 * asosat-query
 *
 * @author bingo 下午4:35:55
 *
 */
public class DefaultEsNamedQuerier implements Querier<String, FetchQuery> {

  protected final String script;
  protected final Class<?> resultClass;
  protected final List<FetchQuery> fetchQueries;
  protected final Map<String, String> hints = new HashMap<>();

  /**
   * @param script
   * @param resultClass
   * @param hints
   * @param fetchQueries
   */
  public DefaultEsNamedQuerier(String script, Class<?> resultClass, Map<String, String> hints,
      List<FetchQuery> fetchQueries) {
    super();
    this.script = script;
    this.resultClass = resultClass;
    this.fetchQueries = fetchQueries;
    if (hints != null) {
      this.hints.putAll(hints);
    }
  }

  @Override
  public List<FetchQuery> getFetchQueries() {
    return fetchQueries;
  }

  @Override
  public Map<String, String> getHints() {
    return hints;
  }

  /**
   * @return the resultClass
   */
  @SuppressWarnings("unchecked")
  @Override
  public <T> Class<T> getResultClass() {
    return (Class<T>) resultClass;
  }

  @Override
  public String getScript() {
    return script;
  }

}
