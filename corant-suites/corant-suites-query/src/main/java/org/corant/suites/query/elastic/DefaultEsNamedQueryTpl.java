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
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import org.corant.kernel.service.ConversionService;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.query.QueryRuntimeException;
import org.corant.suites.query.dynamic.template.DynamicQueryTplMmResolver;
import org.corant.suites.query.dynamic.template.FreemarkerDynamicQueryTpl;
import org.corant.suites.query.mapping.Query;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonpCharacterEscapes;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.TemplateException;

/**
 * corant-suites-query
 *
 * @author bingo 下午8:25:44
 *
 */
public class DefaultEsNamedQueryTpl
    extends FreemarkerDynamicQueryTpl<DefaultEsNamedQuerier, Map<String, Object>> {

  public final static ObjectMapper OM = new ObjectMapper();

  /**
   * @param query
   * @param conversionService
   */
  public DefaultEsNamedQueryTpl(Query query, ConversionService conversionService) {
    super(query, conversionService);
  }

  @Override
  protected Map<String, Object> convertParameter(Map<String, Object> param) {
    Map<String, Object> convertedParam = new HashMap<>();
    Map<String, Object> tempParam = new HashMap<>();
    if (param != null) {
      tempParam.putAll(param);
      getParamConvertSchema().forEach((pn, pc) -> {
        if (tempParam.containsKey(pn)) {
          Object cvtVal = conversionService.convert(param.get(pn), pc);
          tempParam.put(pn, cvtVal);
        }
      });
    }
    tempParam.forEach((k, v) -> {
      try {
        String jsonVal = v == null ? null : OM.writeValueAsString(v);
        convertedParam.put(k, jsonVal);
      } catch (JsonProcessingException e) {
        throw new CorantRuntimeException(e, "Can not convert parameter %s to json string", k);
      }
    });
    return convertedParam;
  }

  @Override
  protected DefaultEsNamedQuerier doProcess(Map<String, Object> param,
      DynamicQueryTplMmResolver<Map<String, Object>> tmm) {
    try (StringWriter sw = new StringWriter()) {
      getTemplate().process(param, sw);
      return new DefaultEsNamedQuerier(
          OM.writer(JsonpCharacterEscapes.instance())
              .writeValueAsString(OM.readValue(sw.toString(), Object.class)),
          getResultClass(), getHints(), getFetchQueries());// FIXME Do some
                                                           // protection
    } catch (TemplateException | IOException | NullPointerException e) {
      throw new QueryRuntimeException(e, "Freemarker process stringTemplate is error!");
    }
  }

  @Override
  protected DynamicQueryTplMmResolver<Map<String, Object>> getTemplateMethodModel(
      Map<String, Object> param) {
    return null;// We are in line
  }

}
