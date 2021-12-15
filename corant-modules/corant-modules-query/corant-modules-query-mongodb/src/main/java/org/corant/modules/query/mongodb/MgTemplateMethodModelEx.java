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
package org.corant.modules.query.mongodb;

import static org.corant.shared.util.Classes.getComponentClass;
import static org.corant.shared.util.Conversions.toList;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Objects.asString;
import static org.corant.shared.util.Primitives.isPrimitiveOrWrapper;
import static org.corant.shared.util.Primitives.isSimpleClass;
import static org.corant.shared.util.Primitives.wrap;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.bson.BsonDateTime;
import org.bson.BsonDbPointer;
import org.bson.BsonMaxKey;
import org.bson.BsonMinKey;
import org.bson.BsonObjectId;
import org.bson.BsonRegularExpression;
import org.bson.BsonSymbol;
import org.bson.BsonTimestamp;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.corant.modules.query.shared.dynamic.freemarker.AbstractTemplateMethodModelEx;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonpCharacterEscapes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.DBRef;
import freemarker.template.TemplateModelException;

/**
 * corant-modules-query-mongodb
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
    converters.put(BsonDbPointer.class, o -> {
      BsonDbPointer dp = (BsonDbPointer) o;
      return mapOf("$dbPointer",
          mapOf("$ref", dp.getNamespace(), "$id", mapOf("$oid", dp.getId().toHexString())));
    });
    converters.put(BsonSymbol.class, o -> mapOf("$symbol", ((BsonSymbol) o).getSymbol()));
    converters.put(DBRef.class, o -> {
      DBRef r = (DBRef) o;
      Map<Object, Object> map = mapOf("$ref", r.getCollectionName(), "$id",
          mapOf("$oid", ((ObjectId) r.getId()).toHexString()));
      if (r.getDatabaseName() != null) {
        map.put("$db", r.getDatabaseName());
      }
      return map;
    });
  }

  public static final ObjectMapper OM = new ObjectMapper();

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
          Class<?> argCls = wrap(arg.getClass());
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
    return Collections.emptyMap();// mongodb query we use inline
  }

  @Override
  public boolean isSimpleType(Class<?> cls) {
    return isSimpleClass(cls) || BsonMinKey.class.equals(cls)
        || BsonMaxKey.class.isAssignableFrom(cls) || BsonObjectId.class.isAssignableFrom(cls)
        || Decimal128.class.isAssignableFrom(cls)
        || BsonRegularExpression.class.isAssignableFrom(cls)
        || BsonDateTime.class.isAssignableFrom(cls) || BsonTimestamp.class.isAssignableFrom(cls)
        || BsonSymbol.class.isAssignableFrom(cls) || BsonDbPointer.class.isAssignableFrom(cls)
        || DBRef.class.isAssignableFrom(cls);
  }

  protected Object toBsonValue(Object args) {
    if (args == null) {
      return null;
    }
    Class<?> cls = wrap(getComponentClass(args));
    if (!converters.containsKey(cls)) {
      return args;
    } else if (args.getClass().isArray() || args instanceof Iterable) {
      return toList(args, converters.get(cls));
    } else {
      return converters.get(cls).apply(args);
    }
  }

}
