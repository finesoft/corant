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
package org.corant.suites.json;

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Strings.isNotBlank;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.ubiquity.Tuple.Range;
import org.corant.shared.ubiquity.Tuple.Triple;
import org.corant.shared.util.Bytes;
import org.corant.suites.bundle.GlobalMessageCodes;
import org.corant.suites.bundle.exception.GeneralRuntimeException;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.NullSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

/**
 * corant-suites-json
 *
 * @author bingo 下午2:40:27
 *
 */
public class JsonUtils {

  static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    SimpleModule simpleModule = new SimpleModule().addSerializer(new SqlDateSerializer())
        .addDeserializer(Pair.class, new PairDeserializer())
        .addSerializer(Pair.class, new PairSerializer())
        .addDeserializer(Range.class, new RangeDeserializer())
        .addSerializer(Range.class, new RangeSerializer())
        .addDeserializer(Triple.class, new TripleDeserializer())
        .addSerializer(Triple.class, new TripleSerializer());
    objectMapper.registerModules(simpleModule);
    objectMapper.registerModules(new JavaTimeModule());
    objectMapper.getSerializerProvider().setNullKeySerializer(NullSerializer.instance);
    objectMapper.enable(Feature.ALLOW_COMMENTS);
    objectMapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    objectMapper.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
    objectMapper.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
    objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  private JsonUtils() {}

  /**
   * @return The ObjectMapper clone that use in this application
   */
  public static ObjectMapper copyMapper() {
    return objectMapper.copy();
  }

  /**
   * Convert bytes to object
   *
   * @param <T>
   * @param bytes
   * @param cls
   * @return fromBytes
   */
  public static <T> T fromBytes(byte[] bytes, Class<T> cls) {
    if (isEmpty(bytes)) {
      return null;
    } else {
      try {
        return objectMapper.readerFor(cls).readValue(bytes);
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
    }
  }

  /**
   * Convert input string to Map.
   *
   * @param <K>
   * @param <V>
   * @param keyCls
   * @param valueCls
   * @param cmd
   * @return fromString
   */
  public static <K, V> Map<K, V> fromString(Class<K> keyCls, Class<V> valueCls, String cmd) {
    if (!isNotBlank(cmd)) {
      return null;
    }
    try {
      return objectMapper.readValue(cmd,
          objectMapper.getTypeFactory().constructParametricType(Map.class, keyCls, valueCls));
    } catch (IOException e) {
      throw new GeneralRuntimeException(e, GlobalMessageCodes.ERR_OBJ_SEL, cmd);
    }
  }

  /**
   * Convert input json string to Map
   *
   * @param cmd The json string
   * @return Map
   */
  @SuppressWarnings("unchecked")
  public static <K, V> Map<K, V> fromString(String cmd) {
    return fromString(cmd, Map.class);
  }

  /**
   * Convert input string to Parameterized type object.
   *
   * @param cmd
   * @param parametrized
   * @param parameterClasses
   * @return Object
   */
  @SafeVarargs
  public static <C, E> C fromString(String cmd, Class<C> parametrized,
      Class<E>... parameterClasses) {
    if (!isNotBlank(cmd)) {
      return null;
    }
    try {
      return objectMapper.readValue(cmd,
          objectMapper.getTypeFactory().constructParametricType(parametrized, parameterClasses));
    } catch (IOException e) {
      throw new GeneralRuntimeException(e, GlobalMessageCodes.ERR_OBJ_SEL, cmd);
    }
  }

  /**
   * Convert input string to type object.
   *
   * @param cmd
   * @param clazz
   * @return
   */
  public static <T> T fromString(String cmd, Class<T> clazz) {
    if (isNotBlank(cmd)) {
      try {
        return objectMapper.readValue(cmd, clazz);
      } catch (IOException e) {
        throw new GeneralRuntimeException(e, GlobalMessageCodes.ERR_OBJ_SEL, cmd, clazz.getName());
      }
    }
    return null;
  }

  /**
   * Convert object to bytes
   *
   * @param obj
   * @return toBytes
   */
  public static byte[] toBytes(Object obj) {
    if (obj == null) {
      return Bytes.EMPTY_ARRAY;
    } else {
      try {
        return objectMapper.writeValueAsBytes(obj);
      } catch (JsonProcessingException e) {
        throw new CorantRuntimeException(e);
      }
    }
  }

  /**
   * Convert object to json string
   *
   * @param obj
   * @throws IOException
   * @throws JsonMappingException
   * @throws JsonGenerationException
   */
  public static String toString(Object obj) {
    return toString(obj, false);
  }

  /**
   * Convert object to json string
   *
   * @param obj
   * @param pretty
   * @return toJsonStr
   */
  public static String toString(Object obj, boolean pretty) {
    if (obj != null) {
      try {
        if (pretty) {
          return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } else {
          return objectMapper.writeValueAsString(obj);
        }
      } catch (JsonProcessingException e) {
        throw new GeneralRuntimeException(e, GlobalMessageCodes.ERR_OBJ_SEL, obj);
      }
    }
    return null;
  }

  /**
   * corant-suites-json
   *
   * @author bingo 下午12:10:10
   *
   */
  @SuppressWarnings("rawtypes")
  static class PairDeserializer extends JsonDeserializer<Pair> {
    @Override
    public Pair deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
        throws IOException {
      final Object[] array = jsonParser.readValueAs(Object[].class);
      return Pair.of(array[0], array[1]);
    }
  }

  /**
   * corant-suites-json
   *
   * @author bingo 下午12:10:14
   *
   */
  @SuppressWarnings("rawtypes")
  static class PairSerializer extends JsonSerializer<Pair> {
    @Override
    public void serialize(Pair pair, JsonGenerator gen, SerializerProvider serializerProvider)
        throws IOException {
      gen.writeStartArray(pair, 2);
      gen.writeObject(pair.getLeft());
      gen.writeObject(pair.getRight());
      gen.writeEndArray();
    }
  }

  /**
   * corant-suites-json
   *
   * @author bingo 下午12:10:10
   *
   */
  @SuppressWarnings("rawtypes")
  static class RangeDeserializer extends JsonDeserializer<Range> {
    @SuppressWarnings("unchecked")
    @Override
    public Range deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
        throws IOException {
      final Object[] array = jsonParser.readValueAs(Object[].class);
      return Range.of((Comparable) array[0], (Comparable) array[1]);
    }
  }

  /**
   * corant-suites-json
   *
   * @author bingo 下午12:10:14
   *
   */
  @SuppressWarnings("rawtypes")
  static class RangeSerializer extends JsonSerializer<Range> {
    @Override
    public void serialize(Range range, JsonGenerator gen, SerializerProvider serializerProvider)
        throws IOException {
      gen.writeStartArray(range, 2);
      gen.writeObject(range.getStart());
      gen.writeObject(range.getEnd());
      gen.writeEndArray();
    }
  }

  /** 日期转数组 */
  static class SqlDateSerializer extends JsonSerializer<java.sql.Date> {

    @Override
    public Class<java.sql.Date> handledType() {
      return java.sql.Date.class;
    }

    @Override
    public void serialize(java.sql.Date value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      LocalDate date = value.toLocalDate();
      LocalDateSerializer.INSTANCE.serialize(date, gen, provider);
    }
  }

  /**
   * corant-suites-json
   *
   * @author bingo 下午12:10:10
   *
   */
  @SuppressWarnings("rawtypes")
  static class TripleDeserializer extends JsonDeserializer<Triple> {
    @Override
    public Triple deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
        throws IOException {
      final Object[] array = jsonParser.readValueAs(Object[].class);
      return Triple.of(array[0], array[1], array[2]);
    }
  }

  /**
   * corant-suites-json
   *
   * @author bingo 下午12:10:14
   *
   */
  @SuppressWarnings("rawtypes")
  static class TripleSerializer extends JsonSerializer<Triple> {
    @Override
    public void serialize(Triple triple, JsonGenerator gen, SerializerProvider serializerProvider)
        throws IOException {
      gen.writeStartArray(triple, 3);
      gen.writeObject(triple.getLeft());
      gen.writeObject(triple.getMiddle());
      gen.writeObject(triple.getRight());
      gen.writeEndArray();
    }
  }
}
