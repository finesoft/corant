package org.corant.suites.query.mongodb;
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

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;
import org.bson.conversions.Bson;
import org.corant.shared.util.ObjectUtils.Triple;
import org.corant.suites.query.mongodb.MgNamedQuerier.MgOperator;
import org.corant.suites.query.shared.FetchQueryResolver;
import org.corant.suites.query.shared.QueryParameter;
import org.corant.suites.query.shared.QueryResolver;
import org.corant.suites.query.shared.dynamic.freemarker.DynamicTemplateMethodModelEx;
import org.corant.suites.query.shared.dynamic.freemarker.DynamicTemplateMethodModelExJson;
import org.corant.suites.query.shared.dynamic.freemarker.FreemarkerDynamicQuerierBuilder;
import org.corant.suites.query.shared.mapping.Query;

/**
 * corant-suites-query
 *
 * @author bingo 下午8:25:44
 *
 */
public class FreemarkerMgQuerierBuilder extends
    FreemarkerDynamicQuerierBuilder<Map<String, Object>, EnumMap<MgOperator, Bson>, DefaultMgNamedQuerier> {

  /**
   * @param query
   * @param queryResolver
   * @param fetchQueryResolver
   */
  protected FreemarkerMgQuerierBuilder(Query query, QueryResolver queryResolver,
      FetchQueryResolver fetchQueryResolver) {
    super(query, queryResolver, fetchQueryResolver);
  }

  /**
   * @param query
   * @param queryParameter
   * @param parameterResolver
   * @param queryResolver
   * @param mgQuery
   * @param originalScript
   */

  @Override
  protected DefaultMgNamedQuerier build(
      Triple<QueryParameter, Map<String, Object>, String> processed) {
    try {
      @SuppressWarnings("rawtypes")
      final Map mgQuery = DefaultMgNamedQuerier.OM.readValue(processed.getRight(), Map.class);
      return new DefaultMgNamedQuerier(getQuery(), processed.getLeft(), getQueryResolver(),
          getFetchQueryResolver(), mgQuery, processed.getRight());
    } catch (IOException e) {
      logger.log(Level.SEVERE, e, () -> "Can't not build mongodb named querier");
    }
    return null;
  }

  @Override
  protected DynamicTemplateMethodModelEx<Map<String, Object>> getTemplateMethodModelEx() {
    return new DynamicTemplateMethodModelExJson();
  }

}
