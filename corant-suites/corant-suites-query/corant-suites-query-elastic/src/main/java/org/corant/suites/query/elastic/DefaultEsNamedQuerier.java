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
package org.corant.suites.query.elastic;

import java.util.Map;
import org.corant.suites.query.elastic.EsInLineNamedQueryResolver.EsQuerier;
import org.corant.suites.query.shared.QueryParameter;
import org.corant.suites.query.shared.QueryParameterResolver;
import org.corant.suites.query.shared.QueryResultResolver;
import org.corant.suites.query.shared.dynamic.AbstractDynamicQuerier;
import org.corant.suites.query.shared.mapping.Query;

/**
 * corant-suites-query
 *
 * @author bingo 下午4:35:55
 *
 */
public class DefaultEsNamedQuerier extends AbstractDynamicQuerier<Map<String, Object>, String>
    implements EsQuerier {

  protected final String script;
  protected final String name;

  /**
   * @param query
   * @param queryParameter
   * @param parameterResolver
   * @param resultResolver
   * @param script
   */
  protected DefaultEsNamedQuerier(Query query, QueryParameter queryParameter,
      QueryParameterResolver parameterResolver, QueryResultResolver resultResolver, String script) {
    super(query, queryParameter, parameterResolver, resultResolver);
    name = query.getName();
    this.script = script;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getScript() {
    return script;
  }

  @Override
  public Map<String, Object> getScriptParameter() {
    return null;
  }

}
