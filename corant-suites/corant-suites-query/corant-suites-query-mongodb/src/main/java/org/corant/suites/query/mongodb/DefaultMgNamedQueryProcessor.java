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
import java.util.HashMap;
import java.util.Map;
import org.corant.kernel.api.ConversionService;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.shared.dynamic.freemarker.DynamicQueryTplMmResolver;
import org.corant.suites.query.shared.dynamic.freemarker.FreemarkerDynamicQueryProcessor;
import org.corant.suites.query.shared.dynamic.freemarker.JsonDynamicQueryFmTplMmResolver;
import org.corant.suites.query.shared.mapping.Query;

/**
 * corant-suites-query
 *
 * @author bingo 下午8:25:44
 *
 */
@SuppressWarnings("rawtypes")
public class DefaultMgNamedQueryProcessor
    extends FreemarkerDynamicQueryProcessor<DefaultMgNamedQuerier, Map<String, Object>> {

  /**
   * @param query
   * @param conversionService
   */
  public DefaultMgNamedQueryProcessor(Query query, ConversionService conversionService) {
    super(query, conversionService);
  }

  @Override
  protected Map<String, Object> convertParameter(Map<String, Object> param) {
    Map<String, Object> convertedParam = new HashMap<>();
    if (param != null) {
      convertedParam.putAll(param);
      getParamConvertSchema().forEach((pn, pc) -> {
        if (convertedParam.containsKey(pn)) {
          Object cvtVal = conversionService.convert(param.get(pn), pc);
          convertedParam.put(pn, cvtVal);
        }
      });
    }
    return convertedParam;
  }

  @Override
  protected DefaultMgNamedQuerier doProcess(String script, Map<String, Object> param) {
    try {
      // OM.readValue(sw.toString(), Object.class) FIXME Do some protection
      final Map mgQuery = DefaultMgNamedQuerier.OM.readValue(script, Map.class);
      doSomthing(mgQuery, param);
      // OM.writer(JsonpCharacterEscapes.instance()).writeValueAsString(mgQuery)
      return new DefaultMgNamedQuerier(getQueryName(), mgQuery, script, getResultClass(),
          getHints(), getFetchQueries(), getProperties());
    } catch (IOException | NullPointerException e) {
      throw new QueryRuntimeException(e, "Freemarker process stringTemplate is error!");
    }
  }

  @Override
  protected DynamicQueryTplMmResolver<Map<String, Object>> handleTemplateMethodModel(
      Map<String, Object> useParam) {
    return new JsonDynamicQueryFmTplMmResolver().injectTo(useParam);
  }

  void doSomthing(Map mgQuery, Map<String, Object> param) {}

}
