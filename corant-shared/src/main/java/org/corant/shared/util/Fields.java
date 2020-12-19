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

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author bingo 下午8:10:18
 *
 */
public class Fields {

  public static List<Field> getAllFields(final Class<?> cls) {
    Class<?> currentClass = shouldNotNull(cls);
    final List<Field> allFields = new ArrayList<>();
    while (currentClass != null) {
      final Field[] declaredFields = currentClass.getDeclaredFields();
      Collections.addAll(allFields, declaredFields);
      currentClass = currentClass.getSuperclass();
    }
    return allFields;
  }

  public static void traverseFields(Class<?> clazz, Consumer<Field> visitor) {
    traverseFields(clazz, field -> {
      visitor.accept(field);
      return true;
    });
  }

  public static void traverseFields(Class<?> clazz, Function<Field, Boolean> visitor) {
    if (clazz != null) {
      Class<?> current = clazz;
      stop: while (current != null) {
        for (Field field : current.getDeclaredFields()) {
          if (!visitor.apply(field)) {
            break stop;
          }
        }
        current = current.getSuperclass();
      }
    }
  }

  public static void traverseLocalFields(Class<?> clazz, Function<Field, Boolean> visitor) {
    for (Field field : clazz.getDeclaredFields()) {
      if (!visitor.apply(field)) {
        break;
      }
    }
  }

}
