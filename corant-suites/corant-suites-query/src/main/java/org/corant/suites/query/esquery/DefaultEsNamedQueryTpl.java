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

import java.util.Map;
import org.corant.kernel.service.ConversionService;
import org.corant.suites.query.dynamic.template.DynamicQueryTplResolver;
import org.corant.suites.query.dynamic.template.FreemarkerDynamicQueryTpl;
import org.corant.suites.query.mapping.Query;

/**
 * corant-suites-query
 *
 * @author bingo 下午8:25:44
 *
 */
public class DefaultEsNamedQueryTpl
    extends FreemarkerDynamicQueryTpl<DefaultEsNamedQuerier, Map<String, Object>> {

  /**
   * @param query
   * @param conversionService
   */
  public DefaultEsNamedQueryTpl(Query query, ConversionService conversionService) {
    super(query, conversionService);
  }

  @Override
  protected DefaultEsNamedQuerier doProcess(Map<String, Object> param) {
    return null;
  }

  @Override
  protected DynamicQueryTplResolver<Map<String, Object>> getTemplateMethodModel() {
    return null;
  }

}
