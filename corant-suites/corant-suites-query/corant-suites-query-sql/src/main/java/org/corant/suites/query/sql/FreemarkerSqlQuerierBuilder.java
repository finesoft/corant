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
package org.corant.suites.query.sql;

import org.corant.shared.ubiquity.Tuple.Triple;
import org.corant.suites.query.shared.FetchQueryHandler;
import org.corant.suites.query.shared.QueryParameter;
import org.corant.suites.query.shared.QueryHandler;
import org.corant.suites.query.shared.dynamic.freemarker.DynamicTemplateMethodModelEx;
import org.corant.suites.query.shared.dynamic.freemarker.FreemarkerDynamicQuerierBuilder;
import org.corant.suites.query.shared.mapping.Query;

/**
 * corant-suites-query
 *
 * @author bingo 下午7:46:22
 *
 */
public class FreemarkerSqlQuerierBuilder
    extends FreemarkerDynamicQuerierBuilder<Object[], String, SqlNamedQuerier> {

  /**
   * @param query
   * @param queryResolver
   * @param fetchQueryResolver
   */
  public FreemarkerSqlQuerierBuilder(Query query, QueryHandler queryResolver,
      FetchQueryHandler fetchQueryResolver) {
    super(query, queryResolver, fetchQueryResolver);
  }

  /**
   * Generate SQL script with placeholder, and converted the parameter to appropriate type.
   */
  @Override
  protected DefaultSqlNamedQuerier build(Triple<QueryParameter, Object[], String> processed) {
    return new DefaultSqlNamedQuerier(getQuery(), processed.getLeft(), getQueryResolver(),
        getFetchQueryResolver(), processed.getMiddle(), processed.getRight());
  }

  @Override
  protected DynamicTemplateMethodModelEx<Object[]> getTemplateMethodModelEx() {
    return new SqlTemplateMethodModelEx();
  }

}
