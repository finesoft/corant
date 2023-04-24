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

import static org.corant.shared.util.Assertions.shouldBeEquals;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Classes.getAllInterfaces;
import static org.corant.shared.util.Classes.getUserClass;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-shared
 *
 * @author bingo 下午8:10:18
 *
 */
public class Fields {

  public static List<Field> getAllFields(final Class<?> cls) {
    Class<?> currentClass =
        shouldNotNull(cls, "The given class to get all fields from can't null!");
    final List<Field> allFields = new ArrayList<>();
    while (currentClass != null) {
      final Field[] declaredFields = currentClass.getDeclaredFields();
      Collections.addAll(allFields, declaredFields);
      currentClass = currentClass.getSuperclass();
    }
    return allFields;
  }

  public static Field getField(Object target, String fieldName) {
    shouldBeTrue(target != null && fieldName != null,
        "The field name and the target object can't null!");
    final Class<?> targetClass = getUserClass(target);
    for (Class<?> currentClass = targetClass; currentClass != null; currentClass =
        currentClass.getSuperclass()) {
      try {
        return currentClass.getDeclaredField(fieldName);
      } catch (NoSuchFieldException | SecurityException e) {
      }
    }
    Field field = null;
    Class<?> klass = null;
    for (Class<?> currentInterface : getAllInterfaces(targetClass)) {
      try {
        Field match = currentInterface.getField(fieldName);
        if (field != null) {
          shouldBeEquals(field, match,
              "More than one fields named '%s' was found in the interfaces '%s' implemented by '%s'.",
              fieldName, Strings.join(", ", currentInterface.getCanonicalName(), klass.getName()),
              targetClass.getName());
        }
        field = match;
        klass = currentInterface;
      } catch (final NoSuchFieldException ex) {
      }
    }
    return field;
  }

  public static Object getFieldValue(Field field, Object target) {
    shouldBeTrue(target != null && field != null, "The field and the target object can't null!");
    return readFieldValue(field, target);
  }

  public static Object getFieldValue(String fieldName, Object target) {
    Field field = shouldNotNull(getField(target, fieldName), "Can't find any field named %s in %s.",
        fieldName, getUserClass(target));
    return Modifier.isStatic(field.getModifiers()) ? getStaticFieldValue(field)
        : getFieldValue(field, target);
  }

  public static Object getStaticFieldValue(Field field) {
    shouldBeTrue(field != null && Modifier.isStatic(field.getModifiers()),
        "The field can't null and must be a static field!");
    return readFieldValue(field, null);
  }

  public static void setFieldValue(Field field, Object target, Object value) {
    shouldBeTrue(field != null && target != null, "The field and the target object can't null!");
    writeFieldValue(field, target, value);
  }

  public static void setFieldValue(String fieldName, Object target, Object value) {
    shouldBeTrue(target != null && fieldName != null,
        "The field name and the target object can't null!");
    Field field = getField(target, fieldName);
    if (Modifier.isStatic(field.getModifiers())) {
      setStaticFieldValue(field, value);
    } else {
      setFieldValue(field, target, value);
    }
  }

  public static void setStaticFieldValue(Field field, Object value) {
    shouldBeTrue(field != null && Modifier.isStatic(field.getModifiers()),
        "The field can't null and must be a static field!");
    writeFieldValue(field, null, value);
  }

  public static void traverseFields(Class<?> clazz, Consumer<Field> visitor) {
    traverseFields(clazz, field -> {
      visitor.accept(field);
      return Boolean.TRUE;
    });
  }

  public static void traverseFields(Class<?> clazz, Function<Field, Boolean> visitor) {
    if (clazz != null) {
      Class<?> current = clazz;
      stop: while (current != null) {
        for (Field field : current.getDeclaredFields()) {
          if (!visitor.apply(field).booleanValue()) {
            break stop;
          }
        }
        current = current.getSuperclass();
      }
    }
  }

  public static void traverseLocalFields(Class<?> clazz, Function<Field, Boolean> visitor) {
    for (Field field : clazz.getDeclaredFields()) {
      if (!visitor.apply(field).booleanValue()) {
        break;
      }
    }
  }

  static Object readFieldValue(Field field, Object target) {
    try {
      if (!field.canAccess(target)) {
        field.setAccessible(true);
      }
      return field.get(target);
    } catch (IllegalArgumentException | IllegalAccessException e) {
      throw new CorantRuntimeException(e);
    }
  }

  static void writeFieldValue(Field field, Object target, Object value) {
    try {
      if (!field.canAccess(target)) {
        field.setAccessible(true);
      }
      field.set(target, value);
    } catch (IllegalArgumentException | IllegalAccessException e) {
      throw new CorantRuntimeException(e);
    }
  }
}
