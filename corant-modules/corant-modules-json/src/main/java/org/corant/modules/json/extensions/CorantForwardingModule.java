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
package org.corant.modules.json.extensions;

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
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;

/**
 * corant-modules-json
 *
 * @author bingo 14:43:22
 */
public class CorantForwardingModule extends Module {

  static final String NAME = "CorantForwardingModule";

  public static final CorantForwardingModule INSTANCE = new CorantForwardingModule();

  protected CorantForwardingModule() {}

  @Override
  public boolean equals(Object o) {
    return this == o;
  }

  @Override
  public String getModuleName() {
    return NAME;
  }

  @Override
  public int hashCode() {
    return NAME.hashCode();
  }

  @Override
  public void setupModule(SetupContext context) {
    SimpleSerializers sers = new SimpleSerializers();
    SimpleDeserializers desers = new SimpleDeserializers();

    // primitives and array
    sers.addSerializer(byte.class, new ForwardingSerializer<>(byte.class));
    sers.addSerializer(byte[].class, new ForwardingSerializer<>(byte[].class));
    sers.addSerializer(short[].class, new ForwardingSerializer<>(short[].class));
    sers.addSerializer(int[].class, new ForwardingSerializer<>(int[].class));
    sers.addSerializer(long[].class, new ForwardingSerializer<>(long[].class));
    sers.addSerializer(float[].class, new ForwardingSerializer<>(float[].class));
    sers.addSerializer(double[].class, new ForwardingSerializer<>(double[].class));
    sers.addSerializer(boolean[].class, new ForwardingSerializer<>(boolean[].class));
    desers.addDeserializer(byte[].class, new ForwardingDeserializer<>(byte[].class));
    desers.addDeserializer(short[].class, new ForwardingDeserializer<>(short[].class));
    desers.addDeserializer(int[].class, new ForwardingDeserializer<>(int[].class));
    desers.addDeserializer(long[].class, new ForwardingDeserializer<>(long[].class));
    desers.addDeserializer(float[].class, new ForwardingDeserializer<>(float[].class));
    desers.addDeserializer(double[].class, new ForwardingDeserializer<>(double[].class));
    desers.addDeserializer(boolean[].class, new ForwardingDeserializer<>(boolean[].class));

    // simple type
    sers.addSerializer(URL.class, new ForwardingSerializer<>(URL.class));
    sers.addSerializer(URI.class, new ForwardingSerializer<>(URI.class));
    sers.addSerializer(Date.class, new ForwardingSerializer<>(Date.class));
    sers.addSerializer(Timestamp.class, new ForwardingSerializer<>(Timestamp.class));
    sers.addSerializer(Duration.class, new ForwardingSerializer<>(Duration.class));
    sers.addSerializer(Period.class, new ForwardingSerializer<>(Period.class));
    sers.addSerializer(Year.class, new ForwardingSerializer<>(Year.class));
    sers.addSerializer(YearMonth.class, new ForwardingSerializer<>(YearMonth.class));
    sers.addSerializer(MonthDay.class, new ForwardingSerializer<>(MonthDay.class));
    sers.addSerializer(LocalDate.class, new ForwardingSerializer<>(LocalDate.class));
    sers.addSerializer(LocalTime.class, new ForwardingSerializer<>(LocalTime.class));
    sers.addSerializer(LocalDateTime.class, new ForwardingSerializer<>(LocalDateTime.class));
    sers.addSerializer(ZonedDateTime.class, new ForwardingSerializer<>(ZonedDateTime.class));
    sers.addSerializer(Instant.class, new ForwardingSerializer<>(Instant.class));
    sers.addSerializer(OffsetTime.class, new ForwardingSerializer<>(OffsetTime.class));
    sers.addSerializer(OffsetDateTime.class, new ForwardingSerializer<>(OffsetDateTime.class));
    sers.addSerializer(Currency.class, new ForwardingSerializer<>(Currency.class));
    sers.addSerializer(Locale.class, new ForwardingSerializer<>(Locale.class));
    sers.addSerializer(UUID.class, new ForwardingSerializer<>(UUID.class));
    sers.addSerializer(Class.class, new ForwardingSerializer<>(Class.class));
    sers.addSerializer(File.class, new ForwardingSerializer<>(File.class));
    context.addDeserializers(desers);
    context.addSerializers(sers);
  }

  @Override
  public Version version() {
    return PackageVersion.VERSION;
  }

  /**
   * corant-modules-json
   *
   * @author bingo 15:47:35
   */
  public static class ForwardingDeserializer<T> extends StdDeserializer<T> {

    private static final long serialVersionUID = 1749752026054900601L;

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
  public static class ForwardingSerializer<T> extends StdSerializer<T> {

    private static final long serialVersionUID = 6117326046001935999L;

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
