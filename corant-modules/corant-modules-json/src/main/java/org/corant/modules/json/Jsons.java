package org.corant.modules.json;
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

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Strings.isNotBlank;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.corant.modules.bundle.GlobalMessageCodes;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.GeneralRuntimeException;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.ubiquity.Tuple.Range;
import org.corant.shared.ubiquity.Tuple.Triple;
import org.corant.shared.ubiquity.TypeLiteral;
import org.corant.shared.util.Bytes;
import org.corant.shared.util.Services;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.NullSerializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

/**
 * corant-modules-json
 *
 * @author bingo 下午2:40:27
 *
 */
public class Jsons {

  static final Logger logger = Logger.getLogger(Jsons.class.getName());
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
    Services.select(GlobalObjectMapperConfigurator.class).sorted(Sortable::compare)
        .forEach(c -> c.configure(objectMapper));
  }
  static final JavaType mapType = objectMapper.constructType(Map.class);
  static final ObjectReader mapReader = objectMapper.readerFor(mapType);

  private Jsons() {}

  /**
   * Convert an object to given target class object.
   *
   * @param <T> the target type
   * @param object the object to be convert
   * @param clazz the target class
   * @return the converted object
   *
   * @see ObjectMapper#convertValue(Object, Class)
   */
  public static <T> T convert(Object object, Class<T> clazz) {
    return objectMapper.convertValue(object, clazz);
  }

  /**
   * Convert an object to given target type literal object.
   *
   * <p>
   * example:
   *
   * <pre>
   * Map&lt;String, String&gt; map = convert(object, new TypeLiteral&lt;Map&lt;String, String&gt;&gt;() {});
   * </pre>
   *
   * @param <T> the target type
   * @param object the object to be convert
   * @param typeLiteral the target type literal
   * @return the converted object
   *
   * @see ObjectMapper#convertValue(Object, JavaType)
   */
  public static <T> T convert(Object object, TypeLiteral<T> typeLiteral) {
    return objectMapper.convertValue(object, objectMapper.constructType(typeLiteral.getType()));
  }

  /**
   * Convert an object to given target type object.
   *
   * <p>
   * example:
   *
   * <pre>
   * Map&lt;String, String&gt; map = convert(object, new TypeReference&lt;Map&lt;String, String&gt;&gt;() {});
   * </pre>
   *
   * @param <T> the target type
   * @param object the object to be convert
   * @param targetTypeRef the target type reference
   * @return the converted object
   *
   * @see ObjectMapper#convertValue(Object, TypeReference)
   */
  public static <T> T convert(Object object, TypeReference<T> targetTypeRef) {
    return objectMapper.convertValue(object, targetTypeRef);
  }

  /**
   * Clone a POJO
   *
   * <p>
   * NOTE: This method is experimental.
   *
   * @param <T> the object type
   * @param pojo the object to clone
   * @return object clone
   *
   * @see TokenBuffer
   */
  @SuppressWarnings("unchecked")
  public static <T> T copy(T pojo) {
    if (pojo == null) {
      return null;
    } else {
      try {
        TokenBuffer tb = new TokenBuffer(objectMapper.getFactory().getCodec(), false);
        objectMapper.writeValue(tb, pojo);
        return (T) objectMapper.readValue(tb.asParser(), pojo.getClass());
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
    }
  }

  /**
   * Clone a POJO with given type literal
   *
   * <p>
   * NOTE: This method is experimental.
   *
   * @param <T> the object type
   * @param pojo the object to clone
   * @param type the object type
   * @return object clone
   *
   * @see TokenBuffer
   */
  @SuppressWarnings("unchecked")
  public static <T> T copy(T pojo, TypeLiteral<T> type) {
    try {
      TokenBuffer tb = new TokenBuffer(objectMapper.getFactory().getCodec(), false);
      objectMapper.writeValue(tb, pojo);
      if (type == null) {
        return (T) objectMapper.readValue(tb.asParser(), pojo.getClass());
      } else {
        return objectMapper.readValue(tb.asParser(), objectMapper.constructType(type.getType()));
      }
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  /**
   * Clone a POJO with given type reference
   *
   * <p>
   * NOTE: This method is experimental.
   *
   * @param <T> the object type
   * @param pojo the object to clone
   * @param type the object type
   * @return object clone
   *
   * @see TokenBuffer
   */
  @SuppressWarnings("unchecked")
  public static <T> T copy(T pojo, TypeReference<T> type) {
    if (pojo == null) {
      return null;
    } else {
      try {
        TokenBuffer tb = new TokenBuffer(objectMapper.getFactory().getCodec(), false);
        objectMapper.writeValue(tb, pojo);
        if (type == null) {
          return (T) objectMapper.readValue(tb.asParser(), pojo.getClass());
        } else {
          return objectMapper.readValue(tb.asParser(), type);
        }
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
    }
  }

  /**
   * Returns the ObjectMapper clone that use in this application
   */
  public static ObjectMapper copyMapper() {
    return objectMapper.copy();
  }

  /**
   * Returns a Map object from given JSON bytes
   *
   * @param jsonBytes the JSON serialized bytes of the object
   */
  public static <K, V> Map<K, V> fromBytes(byte[] jsonBytes) {
    if (isEmpty(jsonBytes)) {
      return null;
    } else {
      try {
        return mapReader.readValue(jsonBytes);
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
    }
  }

  /**
   * Returns an typed object from given JSON bytes and class
   *
   * @param <T> the the expected object type
   * @param jsonBytes the JSON serialized bytes of the object
   * @param cls the expected object class
   */
  public static <T> T fromBytes(byte[] jsonBytes, Class<T> cls) {
    if (isEmpty(jsonBytes)) {
      return null;
    } else {
      try {
        return objectMapper.readerFor(cls).readValue(jsonBytes);
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
    }
  }

  /**
   * Returns an typed object from given JSON bytes and type literal
   *
   * @param <T> the the expected object type
   * @param jsonBytes the JSON serialized bytes of the object
   * @param type the expected object type
   */
  public static <T> T fromBytes(byte[] jsonBytes, TypeLiteral<T> type) {
    if (isEmpty(jsonBytes)) {
      return null;
    } else {
      try {
        return objectMapper.readerFor(objectMapper.constructType(type.getType()))
            .readValue(jsonBytes);
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
    }
  }

  /**
   * Returns an typed object from given JSON bytes and type reference
   *
   * @param <T> the the expected object type
   * @param jsonBytes the JSON serialized bytes of the object
   * @param type the expected object type
   */
  public static <T> T fromBytes(byte[] jsonBytes, TypeReference<T> type) {
    if (isEmpty(jsonBytes)) {
      return null;
    } else {
      try {
        return objectMapper.readValue(jsonBytes, type);
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
    }
  }

  /**
   * Returns a Map object from given JSON string with key class and value class.
   *
   * @param <K> the Map key type
   * @param <V> the Map value type
   * @param keyCls the map key class
   * @param valueCls the map value class
   * @param jsonString the JSON serialized string
   */
  public static <K, V> Map<K, V> fromString(Class<K> keyCls, Class<V> valueCls, String jsonString) {
    if (!isNotBlank(jsonString)) {
      return null;
    }
    try {
      return objectMapper.readValue(jsonString,
          objectMapper.getTypeFactory().constructParametricType(Map.class, keyCls, valueCls));
    } catch (IOException e) {
      throw new GeneralRuntimeException(e, GlobalMessageCodes.ERR_OBJ_SEL, jsonString);
    }
  }

  /**
   * Returns a Map object from given JSON string
   *
   * @param jsonString the JSON string to be convert
   */
  public static <K, V> Map<K, V> fromString(String jsonString) {
    try {
      return mapReader.readValue(jsonString);
    } catch (JsonProcessingException e) {
      throw new CorantRuntimeException(e);
    }
  }

  /**
   * Returns an typed object from given JSON string and class
   *
   * @param jsonString the JSON serialized string of the object
   * @param clazz the expected object class
   */
  public static <T> T fromString(String jsonString, Class<T> clazz) {
    if (isNotBlank(jsonString)) {
      try {
        return objectMapper.readValue(jsonString, clazz);
      } catch (IOException e) {
        throw new GeneralRuntimeException(e, GlobalMessageCodes.ERR_OBJ_SEL, jsonString,
            clazz.getName());
      }
    }
    return null;
  }

  /**
   * Returns an typed object from given JSON string and type literal.
   *
   * @param jsonString the JSON string to be deserialize
   * @param targetTypeLiteral the target type literal
   */
  public static <T> T fromString(String jsonString, TypeLiteral<T> targetTypeLiteral) {
    if (!isNotBlank(jsonString)) {
      return null;
    }
    try {
      return objectMapper.readValue(jsonString,
          objectMapper.constructType(targetTypeLiteral.getType()));
    } catch (IOException e) {
      throw new GeneralRuntimeException(e, GlobalMessageCodes.ERR_OBJ_SEL, jsonString);
    }
  }

  /**
   * Returns an typed object from given JSON string and type reference.
   *
   * @param jsonString the JSON string to be deserialize
   * @param targetTypeRef the target type reference
   */
  public static <T> T fromString(String jsonString, TypeReference<T> targetTypeRef) {
    if (!isNotBlank(jsonString)) {
      return null;
    }
    try {
      return objectMapper.readValue(jsonString, targetTypeRef);
    } catch (IOException e) {
      throw new GeneralRuntimeException(e, GlobalMessageCodes.ERR_OBJ_SEL, jsonString);
    }
  }

  /**
   * Returns serialized JSON byte array from given object or empty byte array if the given object is
   * null.
   *
   * @param obj
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
   * Returns serialized JSON string from given object or null if the given object is null.
   *
   * @param obj the object to be serialized
   */
  public static String toString(Object obj) {
    return toString(obj, false);
  }

  /**
   * Returns serialized JSON string from given object or null if the given object is null.
   *
   * @param obj the object to be serialized
   * @param pretty whether to enable pretty printer indentation
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
   * Returns an typed object from given JSON bytes and class, when a exception occurs, it returns
   * null.
   *
   * @param <T> the the expected object type
   * @param jsonBytes the JSON serialized bytes of the object
   * @param cls the expected object class
   */
  public static <T> T tryFromBytes(byte[] jsonBytes, Class<T> cls) {
    if (isNotEmpty(jsonBytes)) {
      try {
        return objectMapper.readerFor(cls).readValue(jsonBytes);
      } catch (IOException e) {
        logger.log(Level.WARNING, e,
            () -> String.format("Can't deserialize bytes to %s object.", cls.getName()));
      }
    }
    return null;
  }

  /**
   * Returns an typed object from given JSON string and class, when a exception occurs, it returns
   * null.
   *
   * @param jsonString the JSON serialized string of the object
   * @param clazz the expected object class
   */
  public static <T> T tryFromString(String jsonString, Class<T> clazz) {
    if (isNotBlank(jsonString)) {
      try {
        return objectMapper.readValue(jsonString, clazz);
      } catch (IOException e) {
        logger.log(Level.WARNING, e,
            () -> String.format("Can't deserialize String to %s object.", clazz.getName()));
      }
    }
    return null;
  }

  /**
   * corant-modules-json
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
   * corant-modules-json
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
   * corant-modules-json
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
   * corant-modules-json
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
      gen.writeObject(range.getMin());
      gen.writeObject(range.getMax());
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
   * corant-modules-json
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
   * corant-modules-json
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
