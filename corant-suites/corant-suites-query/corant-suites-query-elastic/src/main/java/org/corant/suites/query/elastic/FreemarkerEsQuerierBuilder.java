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

import java.io.IOException;
import java.util.Map;
import org.corant.shared.util.ObjectUtils.Triple;
import org.corant.suites.query.shared.FetchQueryResolver;
import org.corant.suites.query.shared.QueryParameter;
import org.corant.suites.query.shared.QueryParameterResolver;
import org.corant.suites.query.shared.QueryResultResolver;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.shared.dynamic.freemarker.DynamicTemplateMethodModelEx;
import org.corant.suites.query.shared.dynamic.freemarker.DynamicTemplateMethodModelExJson;
import org.corant.suites.query.shared.dynamic.freemarker.FreemarkerDynamicQuerierBuilder;
import org.corant.suites.query.shared.mapping.Query;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * corant-suites-query
 *
 * @author bingo 下午8:25:44
 *
 */
public class FreemarkerEsQuerierBuilder extends
    FreemarkerDynamicQuerierBuilder<Map<String, Object>, Map<Object, Object>, DefaultEsNamedQuerier> {

  public final static ObjectMapper OM = new ObjectMapper();

  /**
   * @param query
   * @param parameterResolver
   * @param resultResolver
   * @param fetchQueryResolver
   */
  protected FreemarkerEsQuerierBuilder(Query query, QueryParameterResolver parameterResolver,
      QueryResultResolver resultResolver, FetchQueryResolver fetchQueryResolver) {
    super(query, parameterResolver, resultResolver, fetchQueryResolver);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected DefaultEsNamedQuerier build(
      Triple<QueryParameter, Map<String, Object>, String> processed) {
    try {
      @SuppressWarnings("rawtypes")
      final Map esQuery = OM.readValue(processed.getRight(), Map.class);
      return new DefaultEsNamedQuerier(getQuery(), processed.getLeft(), getResultResolver(),
          getFetchQueryResolver(), esQuery);
    } catch (IOException e) {
      throw new QueryRuntimeException(e, "Freemarker process stringTemplate is error!");
    }
  }

  @Override
  protected DynamicTemplateMethodModelEx<Map<String, Object>> getTemplateMethodModelEx() {
    return new DynamicTemplateMethodModelExJson();
  }

}
