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
package org.corant.shared.util;

import static org.corant.shared.util.Lists.collectionOf;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * corant-shared
 *
 * @author bingo 下午2:18:32
 *
 */
public class Sets {

  /**
   * Convert an array to immutable set
   *
   * @param <T>
   * @param objects the array
   * @return an immutable set that combined by the passed in array
   */
  @SafeVarargs
  public static <T> Set<T> immutableSetOf(final T... objects) {
    if (objects == null || objects.length == 0) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(setOf(objects));
  }

  /**
   * Convert an array to linked hash set
   *
   * @param <T>
   * @param objects
   * @return a linked hash set that combined by the passed in array
   */
  @SafeVarargs
  public static <T> LinkedHashSet<T> linkedHashSetOf(final T... objects) {
    return collectionOf(LinkedHashSet::new, objects);
  }

  /**
   * Convert an array to set
   *
   * @param <T>
   * @param objects
   * @return a set that combined by the passed in array
   */
  @SafeVarargs
  public static <T> Set<T> setOf(final T... objects) {
    return collectionOf(HashSet::new, objects);
  }
}
