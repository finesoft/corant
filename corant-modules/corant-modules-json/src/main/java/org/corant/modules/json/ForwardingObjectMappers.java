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
import java.util.UUID;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.NullSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * corant-modules-json
 * <p>
 * A simple object that cancels JSON serialization and de-serialization of a specific simple type,
 * mainly used to convert POJO object into {@link java.util.Map} object.
 * <p>
 * Note: If the property type in the object is Object[] or Collection-related subclasses, since JSON
 * conversion only supports lists, all involved properties will be converted to
 * {@link java.util.ArrayList} when converted to Map.
 *
 * @author bingo 11:52:57
 */
@SuppressWarnings("serial")
public class ForwardingObjectMappers {

  protected static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.enable(Feature.ALLOW_COMMENTS);
    objectMapper.getSerializerProvider().setNullKeySerializer(NullSerializer.instance);
    objectMapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    // forwarding module
    SimpleModule module = new SimpleModule();
    // primitives and array
    module.addSerializer(byte.class, new ForwardingSerializer<>(byte.class));
    module.addSerializer(byte[].class, new ForwardingSerializer<>(byte[].class));
    module.addDeserializer(byte[].class, new ForwardingDeserializer<>(byte[].class));
    module.addSerializer(short[].class, new ForwardingSerializer<>(short[].class));
    module.addDeserializer(short[].class, new ForwardingDeserializer<>(short[].class));
    module.addSerializer(int[].class, new ForwardingSerializer<>(int[].class));
    module.addDeserializer(int[].class, new ForwardingDeserializer<>(int[].class));
    module.addSerializer(long[].class, new ForwardingSerializer<>(long[].class));
    module.addDeserializer(long[].class, new ForwardingDeserializer<>(long[].class));
    module.addSerializer(float[].class, new ForwardingSerializer<>(float[].class));
    module.addDeserializer(float[].class, new ForwardingDeserializer<>(float[].class));
    module.addSerializer(double[].class, new ForwardingSerializer<>(double[].class));
    module.addDeserializer(double[].class, new ForwardingDeserializer<>(double[].class));
    module.addSerializer(boolean[].class, new ForwardingSerializer<>(boolean[].class));
    module.addDeserializer(boolean[].class, new ForwardingDeserializer<>(boolean[].class));
    // simple type
    module.addSerializer(URL.class, new ForwardingSerializer<>(URL.class));
    module.addSerializer(URI.class, new ForwardingSerializer<>(URI.class));
    module.addSerializer(Date.class, new ForwardingSerializer<>(Date.class));
    module.addSerializer(Timestamp.class, new ForwardingSerializer<>(Timestamp.class));
    module.addSerializer(Duration.class, new ForwardingSerializer<>(Duration.class));
    module.addSerializer(Period.class, new ForwardingSerializer<>(Period.class));
    module.addSerializer(Year.class, new ForwardingSerializer<>(Year.class));
    module.addSerializer(YearMonth.class, new ForwardingSerializer<>(YearMonth.class));
    module.addSerializer(MonthDay.class, new ForwardingSerializer<>(MonthDay.class));
    module.addSerializer(LocalDate.class, new ForwardingSerializer<>(LocalDate.class));
    module.addSerializer(LocalTime.class, new ForwardingSerializer<>(LocalTime.class));
    module.addSerializer(LocalDateTime.class, new ForwardingSerializer<>(LocalDateTime.class));
    module.addSerializer(ZonedDateTime.class, new ForwardingSerializer<>(ZonedDateTime.class));
    module.addSerializer(Instant.class, new ForwardingSerializer<>(Instant.class));
    module.addSerializer(OffsetTime.class, new ForwardingSerializer<>(OffsetTime.class));
    module.addSerializer(OffsetDateTime.class, new ForwardingSerializer<>(OffsetDateTime.class));
    module.addSerializer(Currency.class, new ForwardingSerializer<>(Currency.class));
    module.addSerializer(Locale.class, new ForwardingSerializer<>(Locale.class));
    module.addSerializer(UUID.class, new ForwardingSerializer<>(UUID.class));
    module.addSerializer(Class.class, new ForwardingSerializer<>(Class.class));
    module.addSerializer(File.class, new ForwardingSerializer<>(File.class));

    objectMapper.registerModule(module);
  }

  public static ObjectMapper objectMapper() {
    return objectMapper.copy();
  }

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

}
