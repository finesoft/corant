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
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.ubiquity.Experimental;
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
import com.fasterxml.jackson.databind.JsonNode;
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
 */
public class Jsons {

  static final Logger logger = Logger.getLogger(Jsons.class.getName());
  static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
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
    Services.selectRequired(GlobalObjectMapperConfigurator.class).sorted(Sortable::compare)
        .forEach(c -> c.configure(objectMapper));
  }
  static final JavaType mapType = objectMapper.constructType(Map.class);
  static final JavaType listType = objectMapper.constructType(List.class);
  static final ObjectReader mapReader = objectMapper.readerFor(mapType);
  static final ObjectReader listReader = objectMapper.readerFor(listType);

  private Jsons() {}

  /**
   * Convert an object to given target class object.
   *
   * @param <T> the target type
   * @param object the object to be converted
   * @param clazz the target class
   * @return the converted object
   *
   * @see ObjectMapper#convertValue(Object, Class)
   */
  public static <T> T convert(Object object, Class<T> clazz) {
    return object == null ? null : objectMapper.convertValue(object, clazz);
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
   * @param object the object to be converted
   * @param typeLiteral the target type literal
   * @return the converted object
   *
   * @see ObjectMapper#convertValue(Object, JavaType)
   */
  public static <T> T convert(Object object, TypeLiteral<T> typeLiteral) {
    return object == null ? null
        : objectMapper.convertValue(object, objectMapper.constructType(typeLiteral.getType()));
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
   * @param object the object to be converted
   * @param targetTypeRef the target type reference
   * @return the converted object
   *
   * @see ObjectMapper#convertValue(Object, TypeReference)
   */
  public static <T> T convert(Object object, TypeReference<T> targetTypeRef) {
    return object == null ? null : objectMapper.convertValue(object, targetTypeRef);
  }

  /**
   * Clone a POJO with given type literal and object mapper
   *
   * <p>
   * NOTE: This method is experimental.
   *
   * @param <T> the object type
   * @param pojo the object to clone
   * @param objectMapper the given object mapper use for handling
   * @param type the object type
   * @return object clone
   *
   * @see TokenBuffer
   */
  @Experimental
  @SuppressWarnings("unchecked")
  public static <T> T copy(Object pojo, ObjectMapper objectMapper, TypeLiteral<T> type) {
    if (pojo == null) {
      return null;
    } else {
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
  }

  /**
   * Clone a POJO with given type reference and object mapper
   *
   * <p>
   * NOTE: This method is experimental.
   *
   * @param <T> the object type
   * @param pojo the object to clone
   * @param objectMapper the given object mapper use for handling
   * @param type the object type
   * @return object clone
   *
   * @see TokenBuffer
   */
  @Experimental
  @SuppressWarnings("unchecked")
  public static <T> T copy(Object pojo, ObjectMapper objectMapper, TypeReference<T> type) {
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
  @Experimental
  public static <T> T copy(Object pojo, TypeLiteral<T> type) {
    return copy(pojo, objectMapper, type);
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
  public static <T> T copy(Object pojo, TypeReference<T> type) {
    return copy(pojo, objectMapper, type);
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
  @Experimental
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
   * Returns the ObjectMapper clone that use in this application
   */
  public static ObjectMapper copyMapper() {
    return objectMapper.copy();
  }

  /**
   * Returns an object from any given JSON string.
   *
   * @see Jsons#fromJsonNode(JsonNode)
   * @see ObjectMapper#readTree(byte[])
   * @see JsonNode
   * @param jsonBytes the bytes used to read JSON content for building the JSON tree
   * @return an object that the given JSON bytes expressed
   */
  public static Object fromAnyBytes(byte[] jsonBytes) {
    if (jsonBytes == null || jsonBytes.length == 0) {
      return null;
    }
    final JsonNode node;
    try {
      node = objectMapper.readTree(jsonBytes);
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
    return fromJsonNode(node);
  }

  /**
   * Returns an object from any given JSON string.
   *
   * @see Jsons#fromJsonNode(JsonNode)
   * @see ObjectMapper#readTree(String)
   * @see JsonNode
   *
   * @param jsonString the string used to read JSON content for building the JSON tree
   * @return an object that the given JSON string expressed
   */
  public static Object fromAnyString(String jsonString) {
    if (jsonString == null) {
      return null;
    }
    final JsonNode node;
    try {
      node = objectMapper.readTree(jsonString);
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
    return fromJsonNode(node);
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
   * Returns a typed object from given JSON bytes and class
   *
   * @param <T> the expected object type
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
   * Returns a typed object from given JSON bytes and type literal
   *
   * @param <T> the expected object type
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
   * Returns a typed object from given JSON bytes and type reference
   *
   * @param <T> the expected object type
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
   * Returns an object from any given JSON node.
   * <p>
   * Note: If the given JSON node is expressed as an array, it will return ArrayList, if it is
   * expressed as an object, it will return LinkedHashMap; if it is expressed as other Java basic
   * types, such as floating point numbers, it will return BigDecimal, and if it is expressed as a
   * 32/16 bits integer numbers, it will return int, 64 bits will return long, more than 64 bits
   * will return BigInteger.
   *
   * @see JsonNode
   * @param node the JSON node to be extracted
   * @return an object
   */
  public static Object fromJsonNode(JsonNode node) {
    if (node == null) {
      return null;
    }
    try {
      if (node.isArray()) {
        return objectMapper.treeToValue(node, listType);
      } else if (node.isObject()) {
        return objectMapper.treeToValue(node, mapType);
      } else if (node.isFloat()) {
        return node.floatValue();
      } else if (node.isDouble()) {
        return node.doubleValue();
      } else if (node.isBoolean()) {
        return node.booleanValue();
      } else if (node.isShort()) {
        return node.shortValue();
      } else if (node.isInt()) {
        return node.intValue();
      } else if (node.isLong()) {
        return node.longValue();
      } else if (node.isBigInteger()) {
        return node.bigIntegerValue();
      } else if (node.isBigDecimal()) {
        return node.decimalValue();
      } else if (node.isTextual()) {
        return node.textValue();
      } else if (node.isBinary()) {
        return node.binaryValue();
      } else if (node.isNull()) {
        return null;
      }
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
    throw new NotSupportedException();
  }

  /**
   * Returns java object from the given jakarta json value object.
   *
   * @param jsonValue jakarta json value object
   * @return java object
   */
  public static Object fromJsonValue(JsonValue jsonValue) {
    if (jsonValue instanceof JsonNumber) {
      JsonNumber jcv = (JsonNumber) jsonValue;
      if (jcv.isIntegral()) {
        return jcv.longValue();
      } else {
        return jcv.doubleValue();
      }
    } else if (jsonValue instanceof JsonArray) {
      JsonArray ja = (JsonArray) jsonValue;
      ArrayList<Object> list = new ArrayList<>(ja.size());
      for (JsonValue jv : ja) {
        list.add(fromJsonValue(jv));
      }
      return list;
    } else if (jsonValue instanceof JsonObject) {
      JsonObject jo = (JsonObject) jsonValue;
      Map<String, Object> map = new LinkedHashMap<>(jo.size());
      jo.forEach((k, v) -> map.put(k, fromJsonValue(v)));
      return map;
    } else if (jsonValue != null) {
      return ((JsonString) jsonValue).getString();
    } else {
      return null;
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
    if (jsonString == null) {
      return null;
    }
    try {
      return objectMapper.readValue(jsonString,
          objectMapper.getTypeFactory().constructParametricType(Map.class, keyCls, valueCls));
    } catch (IOException e) {
      throw new CorantRuntimeException(e, "Can't parse json string");
    }
  }

  /**
   * Returns a Map object from given JSON string
   *
   * @param jsonString the JSON string to be converted
   */
  public static <K, V> Map<K, V> fromString(String jsonString) {
    try {
      return jsonString == null ? null : mapReader.readValue(jsonString);
    } catch (JsonProcessingException e) {
      throw new CorantRuntimeException(e);
    }
  }

  /**
   * Returns a typed object from given JSON string and class
   *
   * @param jsonString the JSON serialized string of the object
   * @param clazz the expected object class
   */
  public static <T> T fromString(String jsonString, Class<T> clazz) {
    if (jsonString != null) {
      try {
        return objectMapper.readValue(jsonString, clazz);
      } catch (IOException e) {
        throw new CorantRuntimeException(e, "Unable to parse JSON string as object of type %s",
            clazz.getName());
      }
    }
    return null;
  }

  /**
   * Returns an typed object from given JSON string and type literal.
   *
   * @param jsonString the JSON string to be deserialized
   * @param targetTypeLiteral the target type literal
   */
  public static <T> T fromString(String jsonString, TypeLiteral<T> targetTypeLiteral) {
    if (jsonString == null) {
      return null;
    }
    try {
      return objectMapper.readValue(jsonString,
          objectMapper.constructType(targetTypeLiteral.getType()));
    } catch (IOException e) {
      throw new CorantRuntimeException(e, "Unable to parse JSON string as object of type %s",
          targetTypeLiteral);
    }
  }

  /**
   * Returns a typed object from given JSON string and type reference.
   *
   * @param jsonString the JSON string to be deserialized
   * @param targetTypeRef the target type reference
   */
  public static <T> T fromString(String jsonString, TypeReference<T> targetTypeRef) {
    if (jsonString == null) {
      return null;
    }
    try {
      return objectMapper.readValue(jsonString, targetTypeRef);
    } catch (IOException e) {
      throw new CorantRuntimeException(e, "Unable to parse JSON string as object of type %s",
          targetTypeRef.getType());
    }
  }

  /**
   * Returns serialized JSON byte array from given object or empty byte array if the given object is
   * null.
   *
   * @param obj the object to be serialized
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
        throw new CorantRuntimeException(e, "Unable to parse object into json string.");
      }
    }
    return null;
  }

  /**
   * Returns a typed object from given JSON bytes and class, when an exception occurs, it returns
   * null.
   *
   * @param <T> the expected object type
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
   * Returns a typed object from given JSON string and class, when an exception occurs, it returns
   * null.
   *
   * @param jsonString the JSON serialized string of the object
   * @param clazz the expected object class
   */
  public static <T> T tryFromString(String jsonString, Class<T> clazz) {
    if (jsonString != null) {
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
