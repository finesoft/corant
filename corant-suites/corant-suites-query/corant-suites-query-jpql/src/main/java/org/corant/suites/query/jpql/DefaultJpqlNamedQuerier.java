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
package org.corant.suites.query.jpql;

import java.util.Map;
import org.corant.suites.query.shared.FetchQueryResolver;
import org.corant.suites.query.shared.QueryParameter;
import org.corant.suites.query.shared.QueryResultResolver;
import org.corant.suites.query.shared.dynamic.AbstractDynamicQuerier;
import org.corant.suites.query.shared.mapping.Query;

/**
 * corant-suites-query
 *
 * @author bingo 下午4:35:55
 *
 */
public class DefaultJpqlNamedQuerier extends AbstractDynamicQuerier<Object[], String>
    implements JpqlNamedQuerier {

  protected final String name;
  protected final String script;
  protected final Object[] scriptParameter;

  /**
   * @param query
   * @param queryParameter
   * @param resultResolver
   * @param fetchQueryResolver
   * @param scriptParameter
   * @param script
   */
  protected DefaultJpqlNamedQuerier(Query query, QueryParameter queryParameter,
      QueryResultResolver resultResolver, FetchQueryResolver fetchQueryResolver,
      Object[] scriptParameter, String script) {
    super(query, queryParameter, resultResolver, fetchQueryResolver);
    name = query.getName();
    this.scriptParameter = scriptParameter;
    this.script = script.replaceAll("[\\t\\n\\r]", " ");
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getScript(Map<?, ?> additionals) {
    return script;
  }

  @Override
  public Object[] getScriptParameter() {
    return scriptParameter;
  }

}
