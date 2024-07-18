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

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Primitives.isSimpleClass;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.BsonDateTime;
import org.bson.BsonDbPointer;
import org.bson.BsonMaxKey;
import org.bson.BsonMinKey;
import org.bson.BsonObjectId;
import org.bson.BsonRegularExpression;
import org.bson.BsonSymbol;
import org.bson.BsonTimestamp;
import org.bson.types.Decimal128;
import org.corant.modules.mongodb.MongoExtendedJsons;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.shared.dynamic.freemarker.AbstractTemplateMethodModelEx;
import org.corant.shared.util.Primitives;
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
  private final Query query;

  public MgTemplateMethodModelEx(Query query) {
    this.query = query;
  }

  @Override
  public Object defaultConvertParamValue(Object value) {
    if (value instanceof Iterable<?> it) {
      return convertIterableParamValue(it);
    } else if (value.getClass().isArray()) {
      return convertArrayParamValue((Object[]) value);
    } else if (value instanceof Enum<?> e) {
      return e.toString();
    }
    return value;
  }

  @SuppressWarnings({"rawtypes"})
  @Override
  public Object exec(List arguments) throws TemplateModelException {
    if (isNotEmpty(arguments)) {
      Object arg = getParamValue(arguments);
      try {
        if (arg != null) {
          if (arg instanceof Iterable<?> it) {
            if (isEmpty(it)) {
              return arg;
            } else if (isSimpleElement(it)) {
              return OM.writeValueAsString(MongoExtendedJsons.extended(it));
            }
          } else if (arg.getClass().isArray()) {
            Object[] array = Primitives.wrapArray(arg);
            if (isEmpty(array)) {
              return arg;
            } else if (isSimpleElement(array)) {
              return OM.writeValueAsString(MongoExtendedJsons.extended(array));
            }
          } else if (isSimpleObject(arg)) {
            return OM.writeValueAsString(MongoExtendedJsons.extended(arg));
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

  protected Object convertArrayParamValue(Object[] array) {
    boolean need = false;
    for (Object e : array) {
      if (e instanceof Enum<?>) {
        need = true;
        break;
      }
    }
    if (need) {
      Collection<Object> collection = new ArrayList<>();
      for (Object e : array) {
        if (e instanceof Enum<?>) {
          collection.add(e.toString());
        } else {
          collection.add(e);
        }
      }
      return collection;
    }
    return array;
  }

  protected Object convertIterableParamValue(Iterable<?> it) {
    boolean need = false;
    for (Object e : it) {
      if (e instanceof Enum<?>) {
        need = true;
        break;
      }
    }
    if (need) {
      Collection<Object> collection = (it instanceof Set<?>) ? new HashSet<>() : new ArrayList<>();
      for (Object e : it) {
        if (e instanceof Enum<?>) {
          collection.add(e.toString());
        } else {
          collection.add(e);
        }
      }
      return collection;
    }
    return it;
  }

  @Override
  protected Query getQuery() {
    return query;
  }

}
