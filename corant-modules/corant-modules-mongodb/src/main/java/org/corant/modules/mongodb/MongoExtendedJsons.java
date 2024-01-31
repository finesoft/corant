/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.mongodb;

import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableMap;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Objects.asString;
import static org.corant.shared.util.Primitives.wrap;
import static org.corant.shared.util.Primitives.wrapArray;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import org.bson.BsonDateTime;
import org.bson.BsonDbPointer;
import org.bson.BsonMaxKey;
import org.bson.BsonMinKey;
import org.bson.BsonObjectId;
import org.bson.BsonRegularExpression;
import org.bson.BsonSymbol;
import org.bson.BsonTimestamp;
import org.bson.conversions.Bson;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.corant.shared.ubiquity.Experimental;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonpCharacterEscapes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.mongodb.BasicDBObject;
import com.mongodb.DBRef;

/**
 * corant-modules-mongodb
 * <p>
 *
 * <a href="https://www.mongodb.com/docs/v6.0/reference/mongodb-extended-json-v1/">
 * mongodb-extended-json-v1</a>
 * <p>
 *
 * <pre>
 * <b>Example Field Name       Canonical Format                                         Relaxed Format</b>
 * "_id:"                   {"$oid":"5d505646cf6d4fe581014ab2"}                     {"$oid":"5d505646cf6d4fe581014ab2"}
 * "arrayField":            ["hello",{"$numberInt":"10"}]                           ["hello",10]
 * "dateField":             {"$date":{"$numberLong":"1565546054692"}}               {"$date":"2019-08-11T17:54:14.692Z"}
 * "dateBefore1970":        {"$date":{"$numberLong":"-1577923200000"}}              {"$date":{"$numberLong":"-1577923200000"}}
 * "decimal128Field":       {"$numberDecimal":"10.99"}                              {"$numberDecimal":"10.99"}
 * "documentField":         {"a":"hello"}                                           {"a":"hello"}
 * "doubleField":           {"$numberDouble":"10.5"}                                10.5
 * "infiniteNumber"         {"$numberDouble":"Infinity"}                            {"$numberDouble":"Infinity"}
 * "int32field":            {"$numberInt":"10"}                                     10
 * "int64Field":            {"$numberLong":"50"}                                    50
 * "minKeyField":           {"$minKey":1}                                           {"$minKey":1}
 * "maxKeyField":           {"$maxKey":1}                                           {"$maxKey":1}
 * "regexField":            {"$regularExpression":{"pattern":"^H","options":"i"}}   {"$regularExpression":{"pattern":"^H","options":"i"}}
 * "timestampField":        {"$timestamp":{"t":1565545664,"i":1}}                   {"$timestamp":{"t":1565545664,"i":1}}
 * </pre>
 *
 * @author bingo 下午6:09:18
 */
public class MongoExtendedJsons {

  public static final ObjectMapper OM = new ObjectMapper();// TODO FIXME
  public static final ObjectWriter EOW = OM.writer(JsonpCharacterEscapes.instance());
  public static final Map<Class<?>, Function<Object, Object>> EXTJSON_CONVERTERS;

  static {
    Map<Class<?>, Function<Object, Object>> extJsonConverters = new HashMap<>();
    extJsonConverters.put(BsonDateTime.class, o -> singletonMap("$date",
        singletonMap("$numberLong", asString(((BsonDateTime) o).getValue()))));
    extJsonConverters.put(ZonedDateTime.class, o -> singletonMap("$date",
        singletonMap("$numberLong", asString(((ZonedDateTime) o).toInstant().toEpochMilli()))));
    extJsonConverters.put(Instant.class, o -> singletonMap("$date",
        singletonMap("$numberLong", asString(((Instant) o).toEpochMilli()))));
    extJsonConverters.put(LocalDate.class, o -> singletonMap("$date",
        singletonMap("$numberLong", asString(toObject(o, BsonDateTime.class).getValue()))));
    extJsonConverters.put(Date.class, o -> singletonMap("$date",
        singletonMap("$numberLong", asString(((Date) o).toInstant().toEpochMilli()))));
    extJsonConverters.put(LocalDateTime.class, o -> singletonMap("$date",
        singletonMap("$numberLong", asString(toObject(o, BsonDateTime.class).getValue()))));
    extJsonConverters.put(OffsetDateTime.class, o -> singletonMap("$date",
        singletonMap("$numberLong", asString(toObject(o, BsonDateTime.class).getValue()))));
    extJsonConverters.put(Timestamp.class, o -> {
      Instant it = ((Timestamp) o).toInstant();
      // FIXME the internal increment values
      return singletonMap("$timestamp", mapOf("t", (int) it.getEpochSecond(), "i", it.getNano()));
    });
    extJsonConverters.put(BsonTimestamp.class, o -> {
      BsonTimestamp bto = (BsonTimestamp) o;
      return singletonMap("$timestamp", mapOf("t", bto.getTime(), "i", bto.getInc()));
    });

    extJsonConverters.put(BigDecimal.class, o -> singletonMap("$numberDecimal", o.toString()));
    extJsonConverters.put(Decimal128.class, o -> singletonMap("$numberDecimal", o.toString()));
    extJsonConverters.put(BigInteger.class, o -> singletonMap("$numberDecimal", o.toString()));
    extJsonConverters.put(Double.class, o -> {
      Double d = (Double) o;
      if (d.isNaN()) {
        return singletonMap("$numberDouble", "NaN");
      } else if (d.doubleValue() == Double.POSITIVE_INFINITY) {
        return singletonMap("$numberDouble", "Infinity");
      } else if (d.doubleValue() == Double.NEGATIVE_INFINITY) {
        return singletonMap("$numberDouble", "-Infinity");
      } else {
        return singletonMap("$numberDouble", d.toString());
      }
    });
    extJsonConverters.put(Long.class, o -> singletonMap("$numberLong", o.toString()));
    extJsonConverters.put(Integer.class, o -> singletonMap("$numberInt", o.toString()));

    extJsonConverters.put(BsonMaxKey.class, o -> singletonMap("$maxKey", 1));
    extJsonConverters.put(BsonMinKey.class, o -> singletonMap("$minKey", 1));

    extJsonConverters.put(BsonObjectId.class,
        o -> singletonMap("$oid", ((BsonObjectId) o).getValue().toHexString()));
    extJsonConverters.put(BsonRegularExpression.class, o -> {
      BsonRegularExpression breo = (BsonRegularExpression) o;
      return singletonMap("$regularExpression",
          mapOf("pattern", breo.getPattern(), "options", breo.getOptions()));
    });
    extJsonConverters.put(BsonDbPointer.class, o -> {
      BsonDbPointer dp = (BsonDbPointer) o;
      return singletonMap("$dbPointer",
          mapOf("$ref", dp.getNamespace(), "$id", singletonMap("$oid", dp.getId().toHexString())));
    });
    extJsonConverters.put(BsonSymbol.class,
        o -> singletonMap("$symbol", ((BsonSymbol) o).getSymbol()));
    extJsonConverters.put(DBRef.class, o -> {
      DBRef r = (DBRef) o;
      Map<Object, Object> map = mapOf("$ref", r.getCollectionName(), "$id",
          singletonMap("$oid", ((ObjectId) r.getId()).toHexString()));
      if (r.getDatabaseName() != null) {
        map.put("$db", r.getDatabaseName());
      }
      return map;
    });
    EXTJSON_CONVERTERS = unmodifiableMap(extJsonConverters);
  }

  /**
   * Convert the given value to extended JSON if necessary.
   * <p>
   * <b>"Extended JSON"</b> conversion examples:
   * <ul>
   * <li>Long -> {"$numberLong","3259"}</li>
   * <li>Integer -> {"$numberInt","3259"}</li>
   * </ul>
   *
   * @param value the value to be converted
   * @return extended JSON value or the original given value
   */
  @Experimental
  public static Object extended(Object value) {
    if (value != null) {
      final Class<?> valueClass = wrap(value.getClass());
      if (EXTJSON_CONVERTERS.containsKey(valueClass)) {
        return EXTJSON_CONVERTERS.get(valueClass).apply(value);
      } else if (value instanceof Map) {
        Map<Object, Object> valMap = new HashMap<>(((Map<?, ?>) value).size());
        for (Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
          valMap.put(entry.getKey(), extended(entry.getValue()));
        }
        return valMap;
      } else if (value instanceof Collection) {
        List<Object> list = new ArrayList<>(((Collection<?>) value).size());
        for (Object item : (Collection<?>) value) {
          list.add(extended(item));
        }
        return list;
      } else if (valueClass.isArray()) {
        Object[] valueArray = wrapArray(value);
        int length = valueArray.length;
        Object[] array = new Object[length];
        for (int i = 0; i < length; i++) {
          array[i] = extended(valueArray[i]);
        }
        return array;
      } else {
        return value;
      }
    }
    return null;
  }

  /**
   * Convert the given object to a BSON type object directly.
   * <p>
   * NOTE: If the given object is {@link Bson} instance, the method returns the object directly.
   *
   * @param object the object to be converted, assuming that the object has been converted according
   *        to "Extended JSON" before conversion.
   * @param extended whether convert the given object to "Extended JSON" before conversion
   * @return a BSON object
   * @throws JsonProcessingException if error occurred.
   */
  public static Bson toBson(final Object object, final boolean extended)
      throws JsonProcessingException {
    return object == null || object instanceof Bson ? (Bson) object
        : BasicDBObject.parse(EOW.writeValueAsString((extended ? extended(object) : object)));
  }

  /**
   * Convert the given collection to a BSON type object list directly.
   * <p>
   * NOTE: If the element of the given collection is {@link Bson} instance, the method returns the
   * object directly.
   *
   * @param collection the collection to be converted.
   * @param extended whether convert the given object to "Extended JSON" before conversion
   * @return a BSON object list
   * @throws JsonProcessingException if error occurred.
   *
   * @see #toBson(Object,boolean)
   */
  public static List<Bson> toBsons(final Collection<?> collection, final boolean extended)
      throws JsonProcessingException {
    if (collection == null) {
      return null;
    } else {
      List<Bson> list = new ArrayList<>(collection.size());
      for (Object o : collection) {
        list.add(toBson(o, extended));
      }
      return list;
    }
  }

  /**
   * Convert the given object array to a BSON type object list directly.
   * <p>
   * NOTE: If the element of the given array is {@link Bson} instance, the method returns the object
   * directly.
   *
   * @param array the object array to be converted.
   * @param extended whether convert the given object to "Extended JSON" before conversion
   * @return a BSON object list
   * @throws JsonProcessingException if error occurred.
   *
   * @see #toBson(Object,boolean)
   */
  public static List<Bson> toBsons(final Object[] array, final boolean extended)
      throws JsonProcessingException {
    if (array == null) {
      return null;
    } else {
      List<Bson> list = new ArrayList<>(array.length);
      for (Object o : array) {
        list.add(toBson(o, extended));
      }
      return list;
    }
  }

}
