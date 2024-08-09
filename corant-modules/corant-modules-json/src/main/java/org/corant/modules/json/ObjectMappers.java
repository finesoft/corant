/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.json;

import java.lang.reflect.Type;
import java.util.Map;
import org.corant.modules.json.extensions.CorantForwardingModule;
import org.corant.modules.json.extensions.CorantTimeModule;
import org.corant.modules.json.extensions.CorantTupleModule;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.ubiquity.TypeLiteral;
import org.corant.shared.util.Services;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.std.NullSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * corant-modules-json
 * <p>
 * A convenient class that provides commonly used {@link ObjectMapper} and provides mutual
 * conversion between object and {@link java.util.Map}.
 * <p>
 * Note: In POJO object to {@link java.util.Map} converting, this class cancels JSON serialization
 * and de-serialization of a specific simple types(primitives and its array, java time types). If
 * the property type in the object is Object[] or Collection-related subclasses, since JSON
 * conversion only supports lists, all involved properties will be converted to
 * {@link java.util.ArrayList} when converted to {@link java.util.Map}.
 *
 * @author bingo 11:52:57
 */
public class ObjectMappers {

  protected static final ObjectMapper defaultObjectMapper = new ObjectMapper();
  protected static final ObjectMapper forwardingObjectMapper = new ObjectMapper();

  static {
    // default object mapper setting
    defaultObjectMapper.enable(Feature.ALLOW_COMMENTS);
    defaultObjectMapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    defaultObjectMapper.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    defaultObjectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    defaultObjectMapper.getSerializerProvider().setNullKeySerializer(NullSerializer.instance);
    defaultObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    defaultObjectMapper.registerModules(new JavaTimeModule(), CorantTimeModule.INSTANCE,
        CorantTupleModule.INSTANCE);
    // disable nanoseconds
    defaultObjectMapper.disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
    defaultObjectMapper.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
    defaultObjectMapper.findAndRegisterModules();
    Services.selectRequired(GlobalObjectMapperConfigurator.class).sorted(Sortable::compare)
        .forEach(c -> c.configure(defaultObjectMapper));

    // forwarding object mapper setting
    forwardingObjectMapper.enable(Feature.ALLOW_COMMENTS);
    forwardingObjectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    forwardingObjectMapper.getSerializerProvider().setNullKeySerializer(NullSerializer.instance);
    forwardingObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    forwardingObjectMapper.registerModules(new JavaTimeModule(), CorantForwardingModule.INSTANCE,
        CorantTupleModule.INSTANCE);

  }

  // to map
  protected static final JavaType mapType = forwardingObjectMapper.constructType(Map.class);
  protected static final JavaType docMapType =
      forwardingObjectMapper.constructType(new TypeReference<Map<String, Object>>() {});
  // from map
  protected static final ObjectReader mapReader =
      defaultObjectMapper.readerFor(defaultObjectMapper.constructType(Map.class));

  public static ObjectMapper copyDefaultObjectMapper() {
    return defaultObjectMapper.copy();
  }

  public static ObjectMapper copyForwardingObjectMapper() {
    return forwardingObjectMapper.copy();
  }

  public static <T> T fromMap(Map<?, ?> map, Class<T> klass) {
    if (klass != null) {
      return map == null ? null : defaultObjectMapper.convertValue(map, klass);
    }
    throw new CorantRuntimeException("The target class can't null");
  }

  public static <T> T fromMap(Map<?, ?> map, Type type) {
    if (type != null) {
      return map == null ? null
          : defaultObjectMapper.convertValue(map, defaultObjectMapper.constructType(type));
    }
    throw new CorantRuntimeException("The target type can't null");
  }

  public static <T> T fromMap(Map<?, ?> map, TypeLiteral<T> typeLiteral) {
    if (typeLiteral != null) {
      return map == null ? null
          : defaultObjectMapper.convertValue(map,
              defaultObjectMapper.constructType(typeLiteral.getType()));
    }
    throw new CorantRuntimeException("The target type literal can't null");
  }

  public static <T> T fromMap(Map<?, ?> map, TypeReference<T> targetTypeRef) {
    if (targetTypeRef != null) {
      return map == null ? null : defaultObjectMapper.convertValue(map, targetTypeRef);
    }
    throw new CorantRuntimeException("The target type reference can't null");
  }

  public static ObjectReader mapReader() {
    return mapReader;
  }

  public static <K, V> Map<K, V> readStringAsMap(String content) {
    try {
      return mapReader.readValue(content);
    } catch (JsonProcessingException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static Map<String, Object> toDocMap(Object object) {
    return object == null ? null : forwardingObjectMapper.convertValue(object, docMapType);
  }

  public static <K, V> Map<K, V> toMap(Object object) {
    return object == null ? null : forwardingObjectMapper.convertValue(object, mapType);
  }
}
