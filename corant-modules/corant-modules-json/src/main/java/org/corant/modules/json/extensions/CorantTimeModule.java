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

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.TimeZone;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

/**
 * corant-modules-json
 *
 * @author bingo 14:41:30
 */
public class CorantTimeModule extends Module {

  static final String NAME = "CorantTimeModule";

  public static final CorantTimeModule INSTANCE = new CorantTimeModule();

  protected CorantTimeModule() {}

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
    SimpleDeserializers dsers = new SimpleDeserializers();
    sers.addSerializer(new SqlDateSerializer());
    dsers.addDeserializer(LocalDate.class, CorantLocalDateDeserializer.INSTANCE);
    context.addSerializers(sers);
    context.addDeserializers(dsers);
  }

  @Override
  public Version version() {
    return PackageVersion.VERSION;
  }

  /**
   * corant-modules-json
   *
   * @author bingo 19:26:13
   */
  public static class CorantLocalDateDeserializer extends LocalDateDeserializer {

    private static final long serialVersionUID = 1053864501554302944L;

    public static CorantLocalDateDeserializer INSTANCE = new CorantLocalDateDeserializer();

    public CorantLocalDateDeserializer() {}

    public CorantLocalDateDeserializer(DateTimeFormatter dtf) {
      super(dtf);
    }

    public CorantLocalDateDeserializer(LocalDateDeserializer base, Boolean leniency) {
      super(base, leniency);
    }

    public CorantLocalDateDeserializer(LocalDateDeserializer base, DateTimeFormatter dtf) {
      super(base, dtf);
    }

    public CorantLocalDateDeserializer(LocalDateDeserializer base, Shape shape) {
      super(base, shape);
    }

    @Override
    public LocalDate deserialize(JsonParser parser, DeserializationContext context)
        throws IOException {
      if (parser.hasToken(JsonToken.VALUE_STRING)) {
        return _fromString(parser, context, parser.getText());
      }
      // 30-Sep-2020, tatu: New! "Scalar from Object" (mostly for XML)
      if (parser.isExpectedStartObjectToken()) {
        return _fromString(parser, context,
            context.extractScalarFromObject(parser, this, handledType()));
      }
      if (parser.isExpectedStartArrayToken()) {
        JsonToken t = parser.nextToken();
        if (t == JsonToken.END_ARRAY) {
          return null;
        }
        if (context.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
            && (t == JsonToken.VALUE_STRING || t == JsonToken.VALUE_EMBEDDED_OBJECT)) {
          final LocalDate parsed = deserialize(parser, context);
          if (parser.nextToken() != JsonToken.END_ARRAY) {
            handleMissingEndArrayForSingle(parser, context);
          }
          return parsed;
        }
        if (t == JsonToken.VALUE_NUMBER_INT) {
          int year = parser.getIntValue();
          int month = parser.nextIntValue(-1);
          int day = parser.nextIntValue(-1);

          if (parser.nextToken() != JsonToken.END_ARRAY) {
            throw context.wrongTokenException(parser, handledType(), JsonToken.END_ARRAY,
                "Expected array to end");
          }
          return LocalDate.of(year, month, day);
        }
        context.reportInputMismatch(handledType(),
            "Unexpected token (%s) within Array, expected VALUE_NUMBER_INT", t);
      }
      if (parser.hasToken(JsonToken.VALUE_EMBEDDED_OBJECT)) {
        return (LocalDate) parser.getEmbeddedObject();
      }
      // 06-Jan-2018, tatu: Is this actually safe? Do users expect such coercion?
      if (parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
        CoercionAction act =
            context.findCoercionAction(logicalType(), _valueClass, CoercionInputShape.Integer);
        _checkCoercionFail(context, act, handledType(), parser.getLongValue(),
            "Integer value (" + parser.getLongValue() + ")");

        // issue 58 - also check for NUMBER_INT, which needs to be specified when serializing.
        if (_shape == JsonFormat.Shape.NUMBER_INT || isLenient()) {
          long num = parser.getLongValue();
          if (ChronoField.EPOCH_DAY.range().isValidValue(num)) {
            return LocalDate.ofEpochDay(num);
          } else {
            TimeZone ctxtz = context.getTimeZone();
            ZoneId zoneId = ctxtz != null ? ctxtz.toZoneId() : ZoneId.systemDefault();
            return LocalDate.ofInstant(Instant.ofEpochMilli(num), zoneId);
          }
        }
        return _failForNotLenient(parser, context, JsonToken.VALUE_STRING);
      }
      return _handleUnexpectedToken(context, parser, "Expected array or string.");
    }

    @Override
    protected LocalDateDeserializer withDateFormat(DateTimeFormatter dtf) {
      return new CorantLocalDateDeserializer(this, dtf);
    }

    @Override
    protected LocalDateDeserializer withLeniency(Boolean leniency) {
      return new CorantLocalDateDeserializer(this, leniency);
    }

    @Override
    protected LocalDateDeserializer withShape(JsonFormat.Shape shape) {
      return new CorantLocalDateDeserializer(this, shape);
    }

  }

  /**
   * corant-modules-json
   *
   * @author bingo 14:44:06
   */
  public static class SqlDateSerializer extends JsonSerializer<java.sql.Date> {

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
