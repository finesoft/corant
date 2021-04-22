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
package org.corant.modules.query.shared;

import static org.corant.shared.util.Objects.forceCast;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * corant-modules-query-shared
 *
 * @author bingo 11:39:34
 *
 */
public interface QueryObjectMapper {

  <T> T fromJsonString(String jsonString, Class<T> type);

  Map<String, Object> mapOf(Object object, boolean convert);

  String toJsonString(Object object, boolean Escape, boolean pretty);

  <T> T toObject(Object from, Class<T> type);

  default <T> List<T> toObjects(List<Object> from, Class<T> type) {
    if (from == null) {
      return new ArrayList<>();
    }
    from.replaceAll(e -> toObject(e, type));
    return forceCast(from);
  }

}
