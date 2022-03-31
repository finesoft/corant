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
package org.corant.modules.security.shared;

import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Maps.getMapCollection;
import static org.corant.shared.util.Maps.getMapObject;
import static org.corant.shared.util.Objects.defaultObject;
import java.beans.Transient;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import org.corant.shared.ubiquity.TypeLiteral;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午11:16:12
 *
 */
public interface AttributeSet {

  default Object getAttribute(String name) {
    Map<String, ?> atts = getAttributes();
    return atts != null ? atts.get(name) : null;
  }

  default <T> T getAttribute(String name, Class<T> type) {
    Object att;
    Map<String, ?> atts = getAttributes();
    if (atts != null && (att = atts.get(name)) != null) {
      return toObject(att, type);
    }
    return null;
  }

  default <T> T getAttribute(String name, Class<T> type, T alt) {
    return defaultObject(getAttribute(name, type), alt);
  }

  default <T> T getAttribute(String name, Function<Object, T> converter) {
    Object att;
    Map<String, ?> atts = getAttributes();
    if (atts != null && (att = atts.get(name)) != null) {
      return converter.apply(att);
    }
    return null;
  }

  default <T, C extends Collection<T>> C getAttribute(String name, IntFunction<C> collectionFactory,
      Class<T> itemType) {
    return getMapCollection(getAttributes(), name, collectionFactory, itemType, null);
  }

  default <T, C extends Collection<T>> C getAttribute(String name, IntFunction<C> collectionFactory,
      Function<Object, T> converter) {
    return getMapCollection(getAttributes(), name, collectionFactory, converter);
  }

  default <T> T getAttribute(String name, TypeLiteral<T> type) {
    return getMapObject(getAttributes(), name, type);
  }

  @Transient
  default Set<String> getAttributeNames() {
    Map<String, ?> atts = getAttributes();
    if (atts != null) {
      return atts.keySet();
    }
    return null;
  }

  Map<String, ?> getAttributes();

}
