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
package org.corant.modules.query.mongodb;

import static org.corant.modules.mongodb.MongoExtendedJsons.EXTJSON_CONVERTERS;
import static org.corant.shared.util.Classes.getComponentClass;
import static org.corant.shared.util.Conversions.toList;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Primitives.isPrimitiveOrWrapper;
import static org.corant.shared.util.Primitives.isSimpleClass;
import static org.corant.shared.util.Primitives.wrap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.bson.BsonDateTime;
import org.bson.BsonDbPointer;
import org.bson.BsonMaxKey;
import org.bson.BsonMinKey;
import org.bson.BsonObjectId;
import org.bson.BsonRegularExpression;
import org.bson.BsonSymbol;
import org.bson.BsonTimestamp;
import org.bson.types.Decimal128;
import org.corant.modules.query.shared.dynamic.freemarker.AbstractTemplateMethodModelEx;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonpCharacterEscapes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.DBRef;
import freemarker.template.TemplateModelException;

/**
 * corant-modules-query-mongodb
 *
 * @author bingo 下午2:00:47
 */
public class MgTemplateMethodModelEx extends AbstractTemplateMethodModelEx<Map<String, Object>> {

  public static final ObjectMapper OM = new ObjectMapper();

  @Override
  public Object defaultConvertParamValue(Object value) {
    Class<?> type = getComponentClass(value);
    if (Enum.class.isAssignableFrom(type)) {
      if (value instanceof Iterable || value.getClass().isArray()) {
        return toList(value, t -> t == null ? null : t.toString());
      } else {
        return value.toString();
      }
    } else {
      return value;
    }
  }

  @SuppressWarnings({"rawtypes"})
  @Override
  public Object exec(List arguments) throws TemplateModelException {
    if (isNotEmpty(arguments)) {
      Object arg = getParamValue(arguments);
      try {
        if (arg != null) {
          Class<?> argCls = wrap(arg.getClass());
          if (EXTJSON_CONVERTERS.containsKey(argCls)) {
            return OM.writeValueAsString(toBsonValue(arg));
          } else if (isPrimitiveOrWrapper(argCls)) {
            return arg;
          } else if (argCls.isArray()) {
            if (isEmpty(arg)) {
              return arg;
            } else if (isSimpleType(getComponentClass(arg))) {
              return OM.writeValueAsString(toBsonValue(arg));
            }
          } else if (Collection.class.isAssignableFrom(argCls)) {
            if (isEmpty(arg)) {
              return arg;
            } else if (isSimpleType(getComponentClass(arg))) {
              return OM.writeValueAsString(toBsonValue(arg));
            }
          } else if (isSimpleType(getComponentClass(arg))) {
            return OM.writeValueAsString(toBsonValue(arg));
          } else {
            // Simple injection prevention
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
    return Collections.emptyMap();// mongodb query we use inline
  }

  @Override
  public boolean isSimpleType(Class<?> cls) {
    return isSimpleClass(cls) || BsonMinKey.class.equals(cls)
        || BsonMaxKey.class.isAssignableFrom(cls) || BsonObjectId.class.isAssignableFrom(cls)
        || Decimal128.class.isAssignableFrom(cls)
        || BsonRegularExpression.class.isAssignableFrom(cls)
        || BsonDateTime.class.isAssignableFrom(cls) || BsonTimestamp.class.isAssignableFrom(cls)
        || BsonSymbol.class.isAssignableFrom(cls) || BsonDbPointer.class.isAssignableFrom(cls)
        || DBRef.class.isAssignableFrom(cls);
  }

  protected Object toBsonValue(Object args) {
    if (args == null) {
      return null;
    }
    Class<?> cls = wrap(getComponentClass(args));
    if (!EXTJSON_CONVERTERS.containsKey(cls)) {
      return args;
    } else if (args.getClass().isArray() || args instanceof Iterable) {
      return toList(args, EXTJSON_CONVERTERS.get(cls));
    } else {
      return EXTJSON_CONVERTERS.get(cls).apply(args);
    }
  }

}
