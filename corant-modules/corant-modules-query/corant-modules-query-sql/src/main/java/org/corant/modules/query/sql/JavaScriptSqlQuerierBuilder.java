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

import java.util.ArrayList;
import java.util.List;
import org.corant.modules.query.FetchQueryHandler;
import org.corant.modules.query.QueryHandler;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.shared.dynamic.javascript.JavaScriptDynamicQuerierBuilder;

/**
 * corant-modules-query-sql
 *
 * @author bingo 下午7:46:22
 *
 */
public class JavaScriptSqlQuerierBuilder
    extends JavaScriptDynamicQuerierBuilder<Object[], String, SqlNamedQuerier> {

  /**
   * @param query
   * @param queryResolver
   * @param fetchQueryResolver
   */
  public JavaScriptSqlQuerierBuilder(Query query, QueryHandler queryResolver,
      FetchQueryHandler fetchQueryResolver) {
    super(query, queryResolver, fetchQueryResolver);
  }

  /**
   * Generate SQL script with placeholder, and converted the parameter to appropriate type.
   */
  @Override
  public DefaultSqlNamedQuerier build(Object param) {
    QueryParameter queryParameter = resolveParameter(param);// convert parameter
    List<Object> useParam = new ArrayList<>();
    Object script = execution.apply(new Object[] {queryParameter, useParam});
    return new DefaultSqlNamedQuerier(getQuery(), queryParameter, getQueryHandler(),
        getFetchQueryHandler(), useParam.toArray(new Object[useParam.size()]), script.toString());
  }

}
