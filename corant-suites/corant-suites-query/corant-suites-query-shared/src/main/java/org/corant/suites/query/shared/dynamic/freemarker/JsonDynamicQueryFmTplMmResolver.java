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
package org.corant.suites.query.shared.dynamic.freemarker;

import static org.corant.shared.util.ClassUtils.getComponentClass;
import static org.corant.shared.util.ClassUtils.isPrimitiveOrWrapper;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonpCharacterEscapes;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.TemplateModelException;

/**
 * corant-suites-query
 *
 * @author bingo 下午7:56:57
 *
 */
public class JsonDynamicQueryFmTplMmResolver
    implements DynamicQueryTplMmResolver<Map<String, Object>> {

  public final static ObjectMapper OM = new ObjectMapper();
  final Map<String, Object> parameters = new HashMap<>();

  @SuppressWarnings({"rawtypes"})
  @Override
  public Object exec(List arguments) throws TemplateModelException {
    if (arguments != null && arguments.size() == 1) {
      Object arg = getParamValue(arguments.get(0));
      try {
        if (arg != null) {
          if (isPrimitiveOrWrapper(arg.getClass())) {
            return arg;
          } else if (isSimpleType(arg)) {
            return OM.writeValueAsString(arg);
          } else {
            return OM.writer(JsonpCharacterEscapes.instance())
                .writeValueAsString(OM.writer().writeValueAsString(arg));
          }
        }
      } catch (JsonProcessingException e) {
        throw new TemplateModelException(e);
      }
    }
    return arguments;
  }

  @Override
  public Map<String, Object> getParameters() {
    return parameters;
  }

  @Override
  public QueryTemplateMethodModelType getType() {
    return QueryTemplateMethodModelType.JP;
  }

  @Override
  public DynamicQueryTplMmResolver<Map<String, Object>> injectTo(Map<String, Object> parameters) {
    this.parameters.putAll(parameters);
    DynamicQueryTplMmResolver.super.injectTo(parameters);
    return this;
  }

  boolean isSimpleType(Object arg) {
    Class<?> cls = getComponentClass(arg);
    return String.class.equals(cls) || Number.class.isAssignableFrom(cls)
        || Temporal.class.isAssignableFrom(cls) || Date.class.isAssignableFrom(cls)
        || Enum.class.isAssignableFrom(cls);
  }
}
