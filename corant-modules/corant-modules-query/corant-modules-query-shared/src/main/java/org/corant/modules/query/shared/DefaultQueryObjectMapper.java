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
package org.corant.modules.query.shared;

import static org.corant.shared.util.Maps.getMapKeyPathValues;
import static org.corant.shared.util.Maps.putMapKeyPathValue;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Primitives.isSimpleClass;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import org.apache.commons.beanutils.BeanUtils;
import org.corant.modules.json.Jsons;
import org.corant.modules.query.QueryObjectMapper;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Names;
import org.corant.shared.ubiquity.TypeLiteral;
import org.corant.shared.util.Conversions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonpCharacterEscapes;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * corant-modules-query-shared
 *
 * @author bingo 下午6:48:41
 */
@ApplicationScoped
public class DefaultQueryObjectMapper implements QueryObjectMapper {

  protected ObjectMapper objectMapper = Jsons.copyMapper();
  protected ObjectWriter objectWriter = objectMapper.writer();
  protected ObjectWriter ppObjectWriter = objectMapper.writerWithDefaultPrettyPrinter();
  protected ObjectWriter escapeObjectWriter = objectWriter.with(JsonpCharacterEscapes.instance());
  protected ObjectWriter escapePpObjectWriter =
      ppObjectWriter.with(JsonpCharacterEscapes.instance());
  protected JavaType mapType = objectMapper.constructType(Map.class);
  protected ObjectReader mapReader = objectMapper.readerFor(mapType);

  @SuppressWarnings("unchecked")
  @Override
  public <T> T copy(T object, TypeLiteral<T> type) {
    try {
      TokenBuffer tb = new TokenBuffer(objectMapper.getFactory().getCodec(), false);
      objectMapper.writeValue(tb, object);
      if (type == null) {
        return (T) objectMapper.readValue(tb.asParser(), object.getClass());
      } else {
        return objectMapper.readValue(tb.asParser(), objectMapper.constructType(type.getType()));
      }
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public <T> T fromJsonString(String jsonString, Class<T> type) {
    try {
      return objectMapper.readValue(jsonString, type);
    } catch (JsonProcessingException e) {
      throw new QueryRuntimeException(e);
    }
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Object getMappedValue(Object object, Object key) {
    String[] keyPath = (String[]) key;
    if (object instanceof Map) {
      if (keyPath.length > 1) {
        List<Object> values = getMapKeyPathValues(object, keyPath);
        return values.isEmpty() ? null : values.size() == 1 ? values.get(0) : values;
      } else {
        return ((Map) object).get(keyPath[0]);
      }
    } else {
      try {
        return BeanUtils.getProperty(object, String.join(".", keyPath));
      } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        throw new QueryRuntimeException(e);
      }
    }
  }

  @Override
  public Map<String, Object> mapOf(Object object, boolean convert) {
    if (object == null) {
      return null;
    }
    try {
      if (!convert) {
        return mapReader.readValue(object.toString());
      } else {
        return objectMapper.convertValue(object, mapType);
      }
    } catch (JsonProcessingException e) {
      throw new QueryRuntimeException(e);
    }
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void putMappedValue(Object object, Object key, Object value) {
    String[] keyPath = (String[]) key;
    if (object instanceof Map) {
      putMapKeyPathValue((Map) object, keyPath, value);
    } else if (object != null) {
      try {
        BeanUtils.setProperty(object, String.join(Names.NAME_SPACE_SEPARATORS, keyPath), value);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new QueryRuntimeException(e, "Inject fetched result occurred error %s.",
            e.getMessage());
      }
    }
  }

  @Override
  public String toJsonString(Object object, boolean escape, boolean pretty) {
    if (object == null) {
      return null;
    }
    try {
      if (pretty) {
        if (escape) {
          return escapePpObjectWriter.writeValueAsString(object);
        } else {
          return ppObjectWriter.writeValueAsString(object);
        }
      } else if (escape) {
        return escapeObjectWriter.writeValueAsString(object);
      } else {
        return objectWriter.writeValueAsString(object);
      }
    } catch (JsonProcessingException e) {
      throw new QueryRuntimeException(e);
    }
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public <T> T toObject(Object from, Type type) {
    if (from == null) {
      return null;
    } else if (type instanceof Class) {
      Class<?> clazz = (Class<?>) type;
      if (clazz.isInstance(from)) {
        return (T) from;
      } else if (isSimpleClass(clazz)) {
        return (T) Conversions.toObject(from, clazz);
      } else {
        return (T) objectMapper.convertValue(from, clazz);
      }
    } else {
      return (T) objectMapper.convertValue(from, objectMapper.constructType(type));
    }
  }

  @Override
  public <T> List<T> toObjects(List<Object> from, Type type) {
    if (from == null) {
      return new ArrayList<>();
    }
    if (type instanceof Class) {
      if (isSimpleClass((Class<?>) type)) {
        from.replaceAll(e -> Conversions.toObject(e, (Class<?>) type));
      } else {
        from.replaceAll(e -> objectMapper.convertValue(e, (Class<?>) type));
      }
    } else {
      final JavaType targetType = objectMapper.constructType(type);
      from.replaceAll(e -> objectMapper.convertValue(e, targetType));
    }
    return forceCast(from);
  }
}
