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
package org.corant.suites.query.mongodb;

import static org.corant.shared.util.ClassUtils.getComponentClass;
import static org.corant.shared.util.ClassUtils.isPrimitiveOrWrapper;
import static org.corant.shared.util.ClassUtils.primitiveToWrapper;
import static org.corant.shared.util.ConversionUtils.toList;
import static org.corant.shared.util.ConversionUtils.toObject;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.MapUtils.mapOf;
import static org.corant.shared.util.ObjectUtils.asString;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.bson.BsonDateTime;
import org.bson.BsonMaxKey;
import org.bson.BsonMinKey;
import org.bson.BsonObjectId;
import org.bson.BsonRegularExpression;
import org.bson.BsonTimestamp;
import org.bson.types.Decimal128;
import org.corant.suites.query.shared.dynamic.freemarker.AbstractTemplateMethodModelEx;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonpCharacterEscapes;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.TemplateModelException;

/**
 * corant-suites-query-mongodb
 *
 * @author bingo 下午2:00:47
 *
 */
public class MgTemplateMethodModelEx extends AbstractTemplateMethodModelEx<Map<String, Object>> {

  static final Map<Class<?>, Function<Object, Object>> converters = new HashMap<>();

  static {

    converters.put(BsonDateTime.class,
        o -> mapOf("$date", mapOf("$numberLong", asString(((BsonDateTime) o).getValue()))));
    converters.put(ZonedDateTime.class, o -> mapOf("$date",
        mapOf("$numberLong", asString(((ZonedDateTime) o).toInstant().toEpochMilli()))));
    converters.put(Instant.class,
        o -> mapOf("$date", mapOf("$numberLong", asString(((Instant) o).toEpochMilli()))));
    converters.put(LocalDate.class, o -> mapOf("$date",
        mapOf("$numberLong", asString(toObject(o, BsonDateTime.class).getValue()))));
    converters.put(Date.class,
        o -> mapOf("$date", mapOf("$numberLong", asString(((Date) o).toInstant().toEpochMilli()))));
    converters.put(LocalDateTime.class, o -> mapOf("$date",
        mapOf("$numberLong", asString(toObject(o, BsonDateTime.class).getValue()))));
    converters.put(OffsetDateTime.class, o -> mapOf("$date",
        mapOf("$numberLong", asString(toObject(o, BsonDateTime.class).getValue()))));

    converters.put(BsonTimestamp.class, o -> {
      BsonTimestamp bto = (BsonTimestamp) o;
      return mapOf("$timestamp", mapOf("t", bto.getTime(), "i", bto.getInc()));
    });

    converters.put(BigDecimal.class, o -> mapOf("$numberDecimal", o.toString()));
    converters.put(Decimal128.class, o -> mapOf("$numberDecimal", o.toString()));
    converters.put(BigInteger.class, o -> mapOf("$numberDecimal", o.toString()));
    converters.put(Double.class, o -> {
      Double d = (Double) o;
      if (d.isNaN()) {
        return mapOf("$numberDouble", "NaN");
      } else if (d.doubleValue() == Double.POSITIVE_INFINITY) {
        return mapOf("$numberDouble", "Infinity");
      } else if (d.doubleValue() == Double.NEGATIVE_INFINITY) {
        return mapOf("$numberDouble", "-Infinity");
      } else {
        return mapOf("$numberDouble", d.toString());
      }
    });
    converters.put(Long.class, o -> mapOf("$numberLong", o.toString()));
    converters.put(Integer.class, o -> mapOf("$numberInt", o.toString()));

    converters.put(BsonMaxKey.class, o -> mapOf("$maxKey", 1));
    converters.put(BsonMinKey.class, o -> mapOf("$minKey", 1));

    converters.put(BsonObjectId.class,
        o -> mapOf("$oid", ((BsonObjectId) o).getValue().toHexString()));
    converters.put(BsonRegularExpression.class, o -> {
      BsonRegularExpression breo = (BsonRegularExpression) o;
      return mapOf("$regularExpression",
          mapOf("pattern", breo.getPattern(), "options", breo.getOptions()));
    });

  }

  public static final ObjectMapper OM = new ObjectMapper();

  private final Map<String, Object> parameters = new HashMap<>();

  @Override
  public Object defaultConvertParamValue(Object value) {
    Class<?> type = getComponentClass(value);
    if (Enum.class.isAssignableFrom(type)) {
      if (value instanceof Iterable || value.getClass().isArray()) {
        return toList(value, t -> t == null ? null : t.toString());
      } else {
        return value.toString();
      }
    } else {
      return value;
    }
  }

  @SuppressWarnings({"rawtypes"})
  @Override
  public Object exec(List arguments) throws TemplateModelException {
    if (isNotEmpty(arguments)) {
      Object arg = getParamValue(arguments);
      try {
        if (arg != null) {
          Class<?> argCls = primitiveToWrapper(arg.getClass());
          if (converters.containsKey(argCls)) {
            return OM.writeValueAsString(toBsonValue(arg));
          } else if (isPrimitiveOrWrapper(argCls)) {
            return arg;
          } else if (isSimpleType(getComponentClass(arg))) {
            return OM.writeValueAsString(toBsonValue(arg));
          } else {
            return OM.writer(JsonpCharacterEscapes.instance())
                .writeValueAsString(OM.writer().writeValueAsString(arg));
          }
        }
      } catch (JsonProcessingException e) {
        throw new TemplateModelException(e);
      }
    }
    return arguments;
  }

  @Override
  public Map<String, Object> getParameters() {
    return parameters;
  }

  @Override
  public boolean isSimpleType(Class<?> cls) {
    if (super.isSimpleType(cls)) {
      return true;
    } else {
      return BsonMinKey.class.equals(cls) || BsonMaxKey.class.isAssignableFrom(cls)
          || BsonObjectId.class.isAssignableFrom(cls) || Decimal128.class.isAssignableFrom(cls)
          || BsonRegularExpression.class.isAssignableFrom(cls)
          || BsonDateTime.class.isAssignableFrom(cls) || BsonTimestamp.class.isAssignableFrom(cls);
    }
  }

  protected Object toBsonValue(Object args) {
    if (args == null) {
      return args;
    }
    Class<?> cls = primitiveToWrapper(getComponentClass(args));
    if (!converters.containsKey(cls)) {
      return args;
    } else if (args.getClass().isArray() || args instanceof Iterable) {
      return toList(args, converters.get(cls)::apply);
    } else {
      return converters.get(cls).apply(args);
    }
  }

}
