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

import static org.corant.shared.util.Objects.forceCast;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.ubiquity.TypeLiteral;
import org.corant.shared.util.Services;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.NullSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

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
@SuppressWarnings("serial")
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
    defaultObjectMapper.registerModules(new JavaTimeModule(),
        new SimpleModule("SQLDateToLocalDateModule").addSerializer(new SqlDateSerializer()));
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
    forwardingObjectMapper.registerModules(new JavaTimeModule());
    // forwarding module
    SimpleModule fm = new SimpleModule("ForwardingModule");
    // primitives and array
    fm.addSerializer(byte.class, new ForwardingSerializer<>(byte.class));
    fm.addSerializer(byte[].class, new ForwardingSerializer<>(byte[].class));
    fm.addDeserializer(byte[].class, new ForwardingDeserializer<>(byte[].class));
    fm.addSerializer(short[].class, new ForwardingSerializer<>(short[].class));
    fm.addDeserializer(short[].class, new ForwardingDeserializer<>(short[].class));
    fm.addSerializer(int[].class, new ForwardingSerializer<>(int[].class));
    fm.addDeserializer(int[].class, new ForwardingDeserializer<>(int[].class));
    fm.addSerializer(long[].class, new ForwardingSerializer<>(long[].class));
    fm.addDeserializer(long[].class, new ForwardingDeserializer<>(long[].class));
    fm.addSerializer(float[].class, new ForwardingSerializer<>(float[].class));
    fm.addDeserializer(float[].class, new ForwardingDeserializer<>(float[].class));
    fm.addSerializer(double[].class, new ForwardingSerializer<>(double[].class));
    fm.addDeserializer(double[].class, new ForwardingDeserializer<>(double[].class));
    fm.addSerializer(boolean[].class, new ForwardingSerializer<>(boolean[].class));
    fm.addDeserializer(boolean[].class, new ForwardingDeserializer<>(boolean[].class));
    // simple type
    fm.addSerializer(URL.class, new ForwardingSerializer<>(URL.class));
    fm.addSerializer(URI.class, new ForwardingSerializer<>(URI.class));
    fm.addSerializer(Date.class, new ForwardingSerializer<>(Date.class));
    fm.addSerializer(Timestamp.class, new ForwardingSerializer<>(Timestamp.class));
    fm.addSerializer(Duration.class, new ForwardingSerializer<>(Duration.class));
    fm.addSerializer(Period.class, new ForwardingSerializer<>(Period.class));
    fm.addSerializer(Year.class, new ForwardingSerializer<>(Year.class));
    fm.addSerializer(YearMonth.class, new ForwardingSerializer<>(YearMonth.class));
    fm.addSerializer(MonthDay.class, new ForwardingSerializer<>(MonthDay.class));
    fm.addSerializer(LocalDate.class, new ForwardingSerializer<>(LocalDate.class));
    fm.addSerializer(LocalTime.class, new ForwardingSerializer<>(LocalTime.class));
    fm.addSerializer(LocalDateTime.class, new ForwardingSerializer<>(LocalDateTime.class));
    fm.addSerializer(ZonedDateTime.class, new ForwardingSerializer<>(ZonedDateTime.class));
    fm.addSerializer(Instant.class, new ForwardingSerializer<>(Instant.class));
    fm.addSerializer(OffsetTime.class, new ForwardingSerializer<>(OffsetTime.class));
    fm.addSerializer(OffsetDateTime.class, new ForwardingSerializer<>(OffsetDateTime.class));
    fm.addSerializer(Currency.class, new ForwardingSerializer<>(Currency.class));
    fm.addSerializer(Locale.class, new ForwardingSerializer<>(Locale.class));
    fm.addSerializer(UUID.class, new ForwardingSerializer<>(UUID.class));
    fm.addSerializer(Class.class, new ForwardingSerializer<>(Class.class));
    fm.addSerializer(File.class, new ForwardingSerializer<>(File.class));
    forwardingObjectMapper.registerModule(fm);

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

  /**
   * corant-modules-json
   *
   * @author bingo 15:47:35
   */
  static class ForwardingDeserializer<T> extends StdDeserializer<T> {
    protected ForwardingDeserializer(Class<T> vc) {
      super(vc);
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      Object obj = p.readValueAs(Object.class);
      return forceCast(obj);
    }

  }

  /**
   * corant-modules-json
   *
   * @author bingo 15:47:39
   */
  static class ForwardingSerializer<T> extends StdSerializer<T> {

    protected ForwardingSerializer(Class<T> t) {
      super(t);
    }

    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      TokenBuffer buffer = (TokenBuffer) gen;
      ObjectCodec codec = buffer.getCodec();
      buffer.setCodec(null);
      buffer.writeObject(value);
      buffer.setCodec(codec);
    }
  }

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
}
