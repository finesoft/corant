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
package org.corant.shared.conversion;

import static org.corant.shared.conversion.ConverterHints.CVT_MAX_NEST_DEPT;
import static org.corant.shared.conversion.ConverterHints.CVT_NEST_DEPT_KEY;
import static org.corant.shared.util.ClassUtils.getComponentClass;
import static org.corant.shared.util.IterableUtils.asIterable;
import static org.corant.shared.util.ObjectUtils.tryCast;
import static org.corant.shared.util.StreamUtils.asStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.corant.shared.util.ClassUtils;
import org.corant.shared.util.ObjectUtils;

/**
 * corant-shared
 *
 * @author bingo 上午11:32:01
 *
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class Conversions {

  private static final Logger LOGGER = Logger.getLogger(Conversions.class.getName());

  public static <T> Iterable<T> convert(Iterable<?> value, Class<?> sourceClass,
      Class<T> targetClass, Map<String, ?> hints) {
    Converter converter = resolveConverter(sourceClass, targetClass, hints);
    if (converter != null) {
      return converter.iterable(value, hints);
    } else {
      Converter stringConverter = resolveConverter(String.class, targetClass, hints);
      if (stringConverter != null) {
        LOGGER.fine(() -> String.format(
            "Can not find proper convert for %s -> %s, use String -> %s converter!", sourceClass,
            targetClass, targetClass));
        return stringConverter.iterable(asIterable(value, ObjectUtils::asString), hints);
      }
    }
    throw new ConversionException("Can not find converter for type pair s% -> %s", sourceClass,
        targetClass);
  }

  public static <C extends Collection<?>, T> C convert(Object value, Class<C> collectionClass,
      Class<T> targetClass, Map<String, ?> hints) {
    Class<?> sourceClass = getComponentClass(value);
    Iterable<T> it = null;
    if (value instanceof Iterable) {
      it = convert(tryCast(value, Iterable.class), sourceClass, targetClass, hints);
    } else if (value instanceof Object[]) {
      it = convert(asIterable((Object[]) value), sourceClass, targetClass, hints);
    } else if (value instanceof Iterator) {
      it = convert(() -> ((Iterator) value), sourceClass, targetClass, hints);
    } else if (value instanceof Enumeration) {
      it = convert(asIterable((Enumeration) value), sourceClass, targetClass, hints);
    } else if (value != null) {
      it = convert(asIterable(value), sourceClass, targetClass, hints);
    }
    if (it != null) {
      if (List.class.isAssignableFrom(collectionClass)) {
        return (C) asStream(it).collect(Collectors.toList());
      } else {
        return (C) asStream(it).collect(Collectors.toSet());
      }
    } else {
      if (List.class.isAssignableFrom(collectionClass)) {
        return (C) new ArrayList<>();
      } else {
        return (C) new HashSet<>();
      }
    }
  }

  public static <T> T convert(Object value, Class<T> targetClass) {
    return convert(value, targetClass, null);
  }

  public static <S, T> T convert(Object value, Class<T> targetClass, Map<String, ?> hints) {
    if (value == null) {
      return null;
    }
    Class<?> sourceClass = value.getClass();
    if (targetClass.isAssignableFrom(sourceClass)) {
      return (T) value;
    }
    Converter<S, T> converter = resolveConverter(sourceClass, targetClass, hints);
    if (converter != null) {
      return converter.apply((S) value, hints);
    } else {
      Converter<String, T> stringConverter = resolveConverter(String.class, targetClass, hints);
      if (stringConverter != null) {
        LOGGER.fine(() -> String.format(
            "Can not find proper convert for %s -> %s, use String -> %s converter!", sourceClass,
            targetClass, targetClass));
        return stringConverter.apply(value.toString(), hints);
      }
    }
    throw new ConversionException("Can not find converter for type pair s% -> %s", sourceClass,
        targetClass);
  }

  private static Converter resolveConverter(Class<?> sourceClass, Class<?> targetClass,
      Map<String, ?> hints) {
    return Converters.lookup(ClassUtils.primitiveToWrapper(sourceClass),
        ClassUtils.primitiveToWrapper(targetClass),
        ConverterHints.getHint(hints, CVT_NEST_DEPT_KEY, CVT_MAX_NEST_DEPT)).orElse(null);
  }

}
