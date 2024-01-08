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
package org.corant.modules.bundle;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-bundle
 *
 * @author bingo 下午3:44:33
 */
@SuppressWarnings("rawtypes")
public interface EnumerationSource extends Sortable {

  default List<Class<Enum>> getAllEnumClass() {
    return Collections.emptyList();
  }

  default String getEnumClassLiteral(Class<?> enumClass, Locale locale) {
    return null;
  }

  default String getEnumItemLiteral(Enum enumVal, Locale locale) {
    return enumVal.name();
  }

  default <T extends Enum> Map<T, String> getEnumItemLiterals(Class<T> enumClass, Locale locale) {
    return Collections.emptyMap();
  }
}
