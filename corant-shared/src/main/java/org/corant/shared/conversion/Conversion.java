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

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Iterables.iterableOf;
import static org.corant.shared.util.Iterables.transform;
import static org.corant.shared.util.Objects.tryCast;
import static org.corant.shared.util.Primitives.wrap;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.util.Objects;

/**
 * corant-shared
 *
 * @author bingo 上午11:32:01
 *
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class Conversion {

  private static final Logger LOGGER = Logger.getLogger(Conversion.class.getName());

  /**
   * Convert collection value to target collection value
   *
   * @param <T>
   * @param <C>
   * @param value the collection value to convert
   * @param collectionFactory the constructor of collection
   * @param targetItemClass the target item class of the converted collection
   * @param hints the converter hints use for intervening converters
   * @return A collection of items converted according to the target type
   */
  public static <T, C extends Collection<T>> C convert(Collection<?> value,
      IntFunction<C> collectionFactory, Class<T> targetItemClass, Map<String, ?> hints) {
    if (value == null) {
      return null;
    }
    C collection = collectionFactory.apply(value.size());
    Iterator<T> it = convert(value, targetItemClass, hints).iterator();
    while (it.hasNext()) {
      collection.add(it.next());
    }
    return collection;
  }

  /**
   * Convert iterable values with knowed source class and target class
   *
   * @param <T>
   * @param value the value to convert
   * @param sourceClass the knowed item source class
   * @param targetClass the item target class that will be convertted
   * @param hints the converter hints use for intervening converters
   * @return An iterable of items converted according to the target type
   */
  public static <T> Iterable<T> convert(Iterable<?> value, Class<?> sourceClass,
      Class<T> targetClass, Map<String, ?> hints) {
    Converter converter = resolveConverter(sourceClass, targetClass);
    if (converter != null) {
      return converter.iterable(value, hints);
    } else {
      Converter stringConverter = resolveConverter(String.class, targetClass);
      if (stringConverter != null) {
        LOGGER.fine(() -> String.format(
            "Can not find proper convert for %s -> %s, use String -> %s converter!", sourceClass,
            targetClass, targetClass));
        return stringConverter.iterable(transform(value, Objects::asString), hints);
      }
    }
    throw new ConversionException("Can not find converter for type pair s% -> %s.", sourceClass,
        targetClass);
  }

  /**
   * Convert iterable values with unknowed source class and target class
   *
   * @param <T>
   * @param value the value to convert
   * @param targetClass the item target class that will be convertted
   * @param hints the converter hints use for intervening converters
   * @return An iterable of items converted according to the target type
   */
  public static <T> Iterable<T> convert(Iterable<?> value, Class<T> targetClass,
      Map<String, ?> hints) {
    if (value == null) {
      return null;
    }
    return () -> new Iterator<>() {
      final Iterator<?> it = value.iterator();
      Converter converter = null;
      Class<?> sourceClass = null;
      boolean tryStringConverter = false;

      @Override
      public boolean hasNext() {
        return it.hasNext();
      }

      @Override
      public T next() {
        Object next = it.next();
        if (next == null) {
          return null;
        }
        final Class<?> nextClass = next.getClass();
        if (converter == null || sourceClass == null || !sourceClass.equals(nextClass)) {
          converter = resolveConverter(nextClass, targetClass);
          sourceClass = nextClass;
          if (converter == null) {
            tryStringConverter = true;
            converter = resolveConverter(String.class, targetClass);
          } else {
            tryStringConverter = false;
          }
        }
        return (T) shouldNotNull(converter, "Can not find converter for type pair s% -> %s.",
            sourceClass, targetClass).apply(tryStringConverter ? next.toString() : next, hints);
      }

      @Override
      public void remove() {
        it.remove();
      }
    };
  }

  /**
   * Convert objects to certain item types and collection types, use the default collection
   * constructor.
   *
   * @param <C>
   * @param <T>
   * @param value the value to convert
   * @param collectionClass the collection class
   * @param targetClass the item class
   * @param hints the converter hints use for intervening converters
   * @return A collection of items converted according to the target type
   */
  public static <C extends Collection<T>, T> C convert(Object value, Class<C> collectionClass,
      Class<T> targetClass, Map<String, ?> hints) {
    return convert(value, targetClass, () -> {
      try {
        return collectionClass.getDeclaredConstructor().newInstance();
      } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
          | InvocationTargetException | NoSuchMethodException | SecurityException e) {
        throw new NotSupportedException();
      }
    }, hints);
  }

  /**
   * Convert single object to target class object without hints
   *
   * @param <T> the target class
   * @param value the value to convert
   * @param targetClass the target class that convert to
   *
   * @return the converted object
   */
  public static <T> T convert(Object value, Class<T> targetClass) {
    return convert(value, targetClass, null);
  }

  /**
   * Convert single object to target class object with hints
   *
   * @param <S> the source value object class
   * @param <T> the target class
   * @param value the value to convert
   * @param targetClass the target class that convert to
   * @param hints the converter hints use for intervening converters
   * @return the converted object
   */
  public static <S, T> T convert(Object value, Class<T> targetClass, Map<String, ?> hints) {
    if (value == null) {
      return null;
    }
    Class<?> sourceClass = value.getClass();
    if (targetClass.isAssignableFrom(sourceClass)) {
      return (T) value;
    }
    Converter<S, T> converter = resolveConverter(sourceClass, targetClass);
    if (converter != null) {
      return converter.apply((S) value, hints);
    } else {
      Converter<String, T> stringConverter = resolveConverter(String.class, targetClass);
      if (stringConverter != null) {
        LOGGER.fine(() -> String.format(
            "Can not find proper convert for %s -> %s, use String -> %s converter!", sourceClass,
            targetClass, targetClass));
        return stringConverter.apply(value.toString(), hints);
      }
    }
    throw new ConversionException("Can not find converter for type pair s% -> %s.", sourceClass,
        targetClass);
  }

  /**
   * Convert an value object to a collection objects, use for converting the
   * iterable/array/iterator/enumeration objects to collection objects.
   *
   * if the value object not belong to above then regard the value object as the first item of
   * collection and convert it.
   *
   * @param <T> the target class of item of the collection
   * @param <C> the target collection class
   * @param value the value to convert, may be an iterable/array/iterator/enumeration typed object
   * @param targetItemClass the target class of item of the collection
   * @param collectionFactory the constructor of collection
   * @param hints the converter hints use for intervening converters
   * @return A collection of items converted according to the target type
   */
  public static <T, C extends Collection<T>> C convert(Object value, Class<T> targetItemClass,
      Supplier<C> collectionFactory, Map<String, ?> hints) {
    Iterable<T> it = null;
    if (value instanceof Iterable) {
      it = convert(tryCast(value, Iterable.class), targetItemClass, hints);
    } else if (value instanceof Object[]) {
      it = convert(iterableOf((Object[]) value), targetItemClass, hints);
    } else if (value instanceof Iterator) {
      it = convert(() -> ((Iterator) value), targetItemClass, hints);
    } else if (value instanceof Enumeration) {
      it = convert(iterableOf((Enumeration) value), targetItemClass, hints);
    } else if (value != null) {
      it = convert(iterableOf(value), targetItemClass, hints);
    }
    final C collection = collectionFactory.get();
    if (it != null) {
      for (T item : it) {
        collection.add(item);
      }
    }
    return collection;
  }

  /**
   * Convert an array value to target array
   *
   * @param <T>
   * @param value the array value to convert
   * @param targetItemClass the target element class of array
   * @param arrayFactory the constructor of array
   * @param hints the converter hints use for intervening converters
   * @return An array that elements were converted according to the target type
   */
  public static <T> T[] convert(Object[] value, Class<T> targetItemClass,
      IntFunction<T[]> arrayFactory, Map<String, ?> hints) {
    if (value == null) {
      return arrayFactory.apply(0);
    }
    T[] array = arrayFactory.apply(value.length);
    int i = 0;
    Iterator<T> it = convert(iterableOf(value), targetItemClass, hints).iterator();
    while (it.hasNext()) {
      array[i] = it.next();
      i++;
    }
    return array;
  }

  /**
   * Convert array value to target collection value
   *
   * @param <T>
   * @param <C>
   * @param value the array value to convert
   * @param collectionFactory the constructor of collection
   * @param targetItemClass the target class of item of the collection
   * @param hints the converter hints use for intervening converters
   * @return A collection of items converted according to the target type
   */
  public static <T, C extends Collection<T>> C convert(Object[] value,
      IntFunction<C> collectionFactory, Class<T> targetItemClass, Map<String, ?> hints) {
    if (value == null) {
      return null;
    }
    C collection = collectionFactory.apply(value.length);
    Iterator<T> it = convert(iterableOf(value), targetItemClass, hints).iterator();
    while (it.hasNext()) {
      collection.add(it.next());
    }
    return collection;
  }

  /**
   * Use source class and target class to find out the right converter
   *
   * @param sourceClass
   * @param targetClass
   */
  private static Converter resolveConverter(Class<?> sourceClass, Class<?> targetClass) {
    return Converters.lookup(wrap(sourceClass), wrap(targetClass)).orElse(null);
  }

}
