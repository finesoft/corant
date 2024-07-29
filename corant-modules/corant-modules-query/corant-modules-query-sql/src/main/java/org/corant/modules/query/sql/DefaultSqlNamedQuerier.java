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
package org.corant.modules.query.sql;

import java.util.Map;
import org.corant.modules.query.FetchQueryHandler;
import org.corant.modules.query.QueryHandler;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.shared.dynamic.AbstractDynamicQuerier;

/**
 * corant-modules-query-sql
 *
 * @author bingo 下午4:35:55
 */
public class DefaultSqlNamedQuerier extends AbstractDynamicQuerier<Object[], String>
    implements SqlNamedQuerier {

  private final Object[] scriptParameter;
  private final String script;

  protected DefaultSqlNamedQuerier(Query query, QueryParameter queryParameter,
      QueryHandler queryResolver, FetchQueryHandler fetchQueryResolver, Object[] scriptParameter,
      String script) {
    super(query, queryParameter, queryResolver, fetchQueryResolver);
    this.scriptParameter = scriptParameter;
    this.script = script;// .replaceAll("[\\t\\n\\r]", " ");
  }

  @Override
  public String getScript(Map<?, ?> additions) {
    return script;
  }

  @Override
  public Object[] getScriptParameter() {
    return scriptParameter;
  }

}
