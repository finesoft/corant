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
package org.corant.modules.query.jpql;

import java.util.ArrayList;
import java.util.List;
import org.corant.modules.query.FetchQueryHandler;
import org.corant.modules.query.QueryHandler;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.shared.dynamic.javascript.JavaScriptDynamicQuerierBuilder;

/**
 * corant-modules-query-jpql
 *
 * @author bingo 下午7:46:22
 */
public class JavascriptJpqlQuerierBuilder
    extends JavaScriptDynamicQuerierBuilder<Object[], String, JpqlNamedQuerier> {

  /**
   * @param query the query
   * @param queryHandler query handler
   * @param fetchQueryHandler fetch query handler
   */
  protected JavascriptJpqlQuerierBuilder(Query query, QueryHandler queryHandler,
      FetchQueryHandler fetchQueryHandler) {
    super(query, queryHandler, fetchQueryHandler);
  }

  /**
   * Generate JPQL script with placeholder, and converted the parameter to appropriate type.
   */
  @Override
  public DefaultJpqlNamedQuerier build(QueryParameter queryParam) {
    List<Object> useParam = new ArrayList<>();
    Object script = getExecution().apply(new Object[] {queryParam, useParam});
    return new DefaultJpqlNamedQuerier(getQuery(), queryParam, getQueryHandler(),
        getFetchQueryHandler(), useParam.toArray(new Object[useParam.size()]), script.toString());
  }

}
