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
import org.corant.kernel.api.ConversionService;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.shared.dynamic.freemarker.DynamicQueryTplMmResolver;
import org.corant.suites.query.shared.dynamic.freemarker.FreemarkerDynamicQueryProcessor;
import org.corant.suites.query.shared.mapping.Query;

/**
 * corant-suites-query
 *
 * @author bingo 下午7:46:22
 *
 */
public class JpqlNamedQueryFmProcessor
    extends FreemarkerDynamicQueryProcessor<DefaultJpqlNamedQuerier, Object[]> {

  public JpqlNamedQueryFmProcessor(Query query, ConversionService conversionService) {
    super(query, conversionService);
  }

  /**
   * Generate JPQL script with placeholder, and converted the parameter to appropriate type.
   */
  @Override
  public DefaultJpqlNamedQuerier doProcess(String script, Object[] param) {
    try {
      return new DefaultJpqlNamedQuerier(getQueryName(), script, param, getResultClass(),
          getHints(), getProperties());
    } catch (NullPointerException e) {
      throw new QueryRuntimeException(e, "Freemarker process stringTemplate occurred and error");
    }
  }

  @Override
  protected DynamicQueryTplMmResolver<Object[]> handleTemplateMethodModel(
      Map<String, Object> param) {
    return new JpqlNamedQueryFmTplMmResolver().injectTo(param);
  }

}
