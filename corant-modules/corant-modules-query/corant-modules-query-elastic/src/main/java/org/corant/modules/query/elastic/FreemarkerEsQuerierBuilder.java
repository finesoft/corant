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
package org.corant.modules.query.elastic;

import java.util.Map;
import org.corant.modules.query.FetchQueryHandler;
import org.corant.modules.query.QueryHandler;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.shared.dynamic.freemarker.DynamicTemplateMethodModelEx;
import org.corant.modules.query.shared.dynamic.freemarker.FreemarkerDynamicQuerierBuilder;
import org.corant.shared.ubiquity.Tuple.Triple;

/**
 * corant-modules-query-elastic
 *
 * @author bingo 下午8:25:44
 */
public class FreemarkerEsQuerierBuilder extends
    FreemarkerDynamicQuerierBuilder<Map<String, Object>, Map<Object, Object>, EsNamedQuerier> {

  // public static final ObjectMapper OM = new ObjectMapper();

  /**
   * @param query
   * @param queryResolver
   * @param fetchQueryResolver
   */
  protected FreemarkerEsQuerierBuilder(Query query, QueryHandler queryResolver,
      FetchQueryHandler fetchQueryResolver) {
    super(query, queryResolver, fetchQueryResolver);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected DefaultEsNamedQuerier build(
      Triple<QueryParameter, Map<String, Object>, String> processed) {
    // try {
    @SuppressWarnings("rawtypes")
    // final Map esQuery = OM.readValue(processed.getRight(), Map.class);
    final Map esQuery = queryHandler.getObjectMapper().mapOf(processed.getRight(), false);
    return new DefaultEsNamedQuerier(getQuery(), processed.getLeft(), getQueryHandler(),
        getFetchQueryHandler(), esQuery);
    // } catch (IOException e) {
    // throw new QueryRuntimeException(e, "Freemarker process stringTemplate is error!");
    // }
  }

  @Override
  protected DynamicTemplateMethodModelEx<Map<String, Object>> getTemplateMethodModelEx() {
    return new EsTemplateMethodModelEx();
  }

}
