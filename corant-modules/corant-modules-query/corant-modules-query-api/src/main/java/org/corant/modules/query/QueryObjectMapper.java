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
package org.corant.modules.query;

import static org.corant.shared.util.Objects.forceCast;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.corant.shared.ubiquity.TypeLiteral;

/**
 * corant-modules-query-api
 *
 * This interface is used for processing such as object conversion and extraction in the query, and
 * is suitable for object conversion in the query result set, query parameter conversion and
 * extraction, etc.
 *
 * @author bingo 11:39:34
 *
 */
public interface QueryObjectMapper {

  /**
   * Copy an object
   *
   * @param <T> the object type
   * @param object the object to be copyed
   * @param type the object type
   */
  default <T> T copy(T object, TypeLiteral<T> type) {
    return object;
  }

  /**
   * Deserialize JSON content from given JSON content String and expected type.
   *
   * @param <T> the expected type
   * @param jsonString the JSON content
   * @param type the expected class
   * @return the object
   */
  <T> T fromJsonString(String jsonString, Class<T> type);

  /**
   * From the object of the given map struct, get the corresponding value through the given key.
   *
   * @param object the map struct object
   * @param key the key
   */
  Object getMappedValue(Object object, Object key);

  /**
   * Since the query processing process requires free extraction and conversion, a large number of
   * Map structure objects are used in the query process. This method is used to read JSON strings
   * and convert them to Map or convert ordinary POJO objects to Map.
   *
   * @param object the object may be JSON strings or POJO object
   * @param convert if true use for POJO and Map conversion otherwise use for read JSON strings to
   *        Map
   * @return a Map structure object
   */
  Map<String, Object> mapOf(Object object, boolean convert);

  /**
   * Inject the given object into the given map struct object through the given key
   *
   * @param object the map struct object which to inject
   * @param key the key
   * @param value the value to be injected
   */
  void putMappedValue(Object object, Object key, Object value);

  /**
   * Serialize given object to JSON strings.
   *
   * @param object the object
   * @param Escape whether to enable escape
   * @param pretty whether to enable pretty print
   * @return the JSON strings
   */
  String toJsonString(Object object, boolean Escape, boolean pretty);

  /**
   * Convert the given object to expected typed object.
   *
   * @param <T> the expected type class
   * @param from the object to be converted
   * @param type the expected type
   * @return the converted object
   */
  <T> T toObject(Object from, Type type);

  /**
   * Convert the given objects list to expected typed objects list.
   *
   * @param <T> the expected type class
   * @param from the objects list to be converted
   * @param type the expected type
   * @return the converted objects list
   */
  default <T> List<T> toObjects(List<Object> from, Type type) {
    if (from == null) {
      return new ArrayList<>();
    }
    from.replaceAll(e -> toObject(e, type));
    return forceCast(from);
  }

}
