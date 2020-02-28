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

import static org.corant.shared.util.ClassUtils.getComponentClass;
import static org.corant.shared.util.ClassUtils.isPrimitiveOrWrapper;
import static org.corant.shared.util.ClassUtils.primitiveToWrapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.corant.suites.query.shared.dynamic.freemarker.DynamicTemplateMethodModelEx;
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
public class EsTemplateMethodModelEx implements DynamicTemplateMethodModelEx<Map<String, Object>> {

  public static final String TYPE = "JP";
  public static final ObjectMapper OM = new ObjectMapper();
  private final Map<String, Object> parameters = new HashMap<>();

  @SuppressWarnings({"rawtypes"})
  @Override
  public Object exec(List arguments) throws TemplateModelException {
    if (arguments != null && arguments.size() == 1) {
      Object arg = getParamValue(arguments.get(0));
      try {
        if (arg != null) {
          Class<?> argCls = primitiveToWrapper(arg.getClass());
          if (isPrimitiveOrWrapper(argCls)) {
            return convertParamValue(arg);
          } else if (isSimpleType(getComponentClass(argCls))) {
            return OM.writeValueAsString(convertParamValue(arg));
          } else {
            return OM.writer(JsonpCharacterEscapes.instance())
                .writeValueAsString(OM.writer().writeValueAsString(convertParamValue(arg)));
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
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean isSimpleType(Class<?> cls) {
    return DynamicTemplateMethodModelEx.super.isSimpleType(cls);
  }

  protected Object convertParamValue(Object arg) {
    return arg;
  }

}
