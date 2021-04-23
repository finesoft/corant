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

import static org.corant.shared.util.Objects.forceCast;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import org.corant.modules.json.Jsons;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonpCharacterEscapes;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

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

  @Override
  public <T> T fromJsonString(String jsonString, Class<T> type) {
    try {
      return objectMapper.readValue(jsonString, type);
    } catch (JsonProcessingException e) {
      throw new QueryRuntimeException(e);
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
      } else {
        if (escape) {
          return escapeObjectWriter.writeValueAsString(object);
        } else {
          return objectWriter.writeValueAsString(object);
        }
      }
    } catch (JsonProcessingException e) {
      throw new QueryRuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T toObject(Object from, Class<T> type) {
    if (from == null) {
      return null;
    } else {
      if (type.isInstance(from)) {
        return (T) from;
      } else {
        return objectMapper.convertValue(from, type);
      }
    }
  }

  @Override
  public <T> List<T> toObjects(List<Object> from, Class<T> type) {
    if (from == null) {
      return new ArrayList<>();
    }
    final JavaType targetType = objectMapper.constructType(type);
    from.replaceAll(e -> objectMapper.convertValue(e, targetType));
    return forceCast(from);
  }
}
