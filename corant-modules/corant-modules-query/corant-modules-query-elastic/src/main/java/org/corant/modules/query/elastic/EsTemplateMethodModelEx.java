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

import static org.corant.shared.util.Classes.getComponentClass;
import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Primitives.isPrimitiveOrWrapper;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.corant.modules.query.shared.dynamic.freemarker.AbstractTemplateMethodModelEx;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonpCharacterEscapes;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.TemplateModelException;

/**
 * corant-modules-query-elastic
 *
 * @author bingo 下午7:56:57
 */
public class EsTemplateMethodModelEx extends AbstractTemplateMethodModelEx<Map<String, Object>> {

  public static final ObjectMapper OM = new ObjectMapper();

  @SuppressWarnings({"rawtypes"})
  @Override
  public Object exec(List arguments) throws TemplateModelException {
    if (isNotEmpty(arguments)) {
      Object arg = getParamValue(arguments);
      try {
        if (arg != null) {
          Class<?> argCls = getUserClass(arg.getClass());
          if (isPrimitiveOrWrapper(argCls)) {
            return arg;
          } else if (argCls.isArray()) {
            if (isEmpty(arg)) {
              return arg;
            } else if (isSimpleType(getComponentClass(arg))) {
              return OM.writeValueAsString(arg);
            }
          } else if (Collection.class.isAssignableFrom(argCls)) {
            if (isEmpty(arg)) {
              return arg;
            } else if (isSimpleType(getComponentClass(arg))) {
              return OM.writeValueAsString(arg);
            }
          } else if (isSimpleType(getComponentClass(arg))) {
            return OM.writeValueAsString(arg);
          } else {
            // FIXME throw exception or not ?
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
    return Collections.emptyMap();// elastic search query we use inline
  }

}
