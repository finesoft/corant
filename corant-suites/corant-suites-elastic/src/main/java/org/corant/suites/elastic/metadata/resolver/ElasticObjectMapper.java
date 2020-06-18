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
package org.corant.suites.elastic.metadata.resolver;

import java.io.IOException;
import java.sql.Date;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Classes;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;

/**
 * corant-suites-elastic
 *
 * <p>
 * This class is used for all elastic document conversion
 * </p>
 *
 * @author bingo 上午10:31:55
 *
 */
public class ElasticObjectMapper {
  static final Map<Class<?>, Class<?>> SIMPLE_TYPE_WRAPPER_MAP = new HashMap<>();

  static {
    SIMPLE_TYPE_WRAPPER_MAP.putAll(Classes.PRIMITIVE_WRAPPER_MAP);
    SIMPLE_TYPE_WRAPPER_MAP.put(String.class, String.class);
    SIMPLE_TYPE_WRAPPER_MAP.put(Date.class, Date.class);
    SIMPLE_TYPE_WRAPPER_MAP.put(Temporal.class, Temporal.class);
    SIMPLE_TYPE_WRAPPER_MAP.put(Currency.class, Currency.class);
    SIMPLE_TYPE_WRAPPER_MAP.put(Number.class, Number.class);
    SIMPLE_TYPE_WRAPPER_MAP.put(Locale.class, Locale.class);
    SIMPLE_TYPE_WRAPPER_MAP.put(Enum.class, Enum.class);
    SIMPLE_TYPE_WRAPPER_MAP.put(TimeZone.class, TimeZone.class);
  }

  public static final SimpleModule SIMPLIE_MODEL = new SimpleModule();
  static {
    SIMPLIE_MODEL.addSerializer(new LocalDateSerializer(DateTimeFormatter.ISO_LOCAL_DATE));
    SIMPLIE_MODEL.addSerializer(new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    SIMPLIE_MODEL.addSerializer(new LocalTimeSerializer(DateTimeFormatter.ISO_LOCAL_TIME));
  }

  public static final ObjectMapper ESJOM = new ObjectMapper();

  static {
    ESJOM.registerModule(new JavaTimeModule());
    ESJOM.registerModule(SIMPLIE_MODEL);
    ESJOM.disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
    ESJOM.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
    ESJOM.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    ESJOM.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public static void deregisterSimpleType(Class<?> type) {
    SIMPLE_TYPE_WRAPPER_MAP.remove(type);
  }

  public static boolean isSimpleType(Class<?> type) {
    Class<?> useType = type.isArray() ? type.getComponentType() : type;
    return SIMPLE_TYPE_WRAPPER_MAP.containsKey(useType)
        || SIMPLE_TYPE_WRAPPER_MAP.containsValue(useType)
        || SIMPLE_TYPE_WRAPPER_MAP.keySet().stream().anyMatch(cls -> cls.isAssignableFrom(type));
  }

  public static void registerSimpleType(Class<?> type, Class<?> clazz) {
    SIMPLE_TYPE_WRAPPER_MAP.put(type, clazz);
  }

  public static Map<String, Object> toMap(Object document) {
    if (document != null) {
      return ESJOM.convertValue(document, new TypeReference<Map<String, Object>>() {});
    } else {
      return new HashMap<>();
    }
  }

  public static <T> T toObject(Object data, Class<T> cls) {
    if (data == null) {
      return null;
    } else {
      return ESJOM.copy().convertValue(data, cls);
    }
  }

  public static <T> T toObject(Object object, TypeReference<T> tr) {
    if (object == null) {
      return null;
    } else {
      return ESJOM.convertValue(object, tr);
    }
  }

  public static <T> T toObject(String str, Class<T> cls) {
    if (str == null) {
      return null;
    } else {
      try {
        return ESJOM.readValue(str, cls);
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
    }
  }
}
