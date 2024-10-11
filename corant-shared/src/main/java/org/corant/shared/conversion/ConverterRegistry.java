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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Maps.removeIfValue;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.corant.shared.conversion.ConverterFactory.FactoryConverter;
import org.corant.shared.conversion.converter.AbstractConverter;
import org.corant.shared.conversion.converter.factory.ObjectPrimitiveArrayConverterFactories;
import org.corant.shared.resource.ClassResource;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.util.Classes;
import org.corant.shared.util.Resources;
import org.corant.shared.util.Services;
import org.corant.shared.util.Types;

/**
 * corant-shared
 *
 * @author bingo 上午9:12:49
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ConverterRegistry {

  static final List<ConverterFactory<?, ?>> CONVERTER_FACTORIES = new CopyOnWriteArrayList<>();

  static final Map<ConverterType<?, ?>, Converter<?, ?>> SUPPORT_CONVERTERS =
      new ConcurrentHashMap<>();

  static final Set<ConverterType<?, ?>> NOT_SUPPORT_TYPES =
      Collections.newSetFromMap(new ConcurrentHashMap<>());

  static {
    load();
  }

  /**
   * Remove the registered converter by the given converter or converter factory class. After
   * removed, if the given class is a converter class, all conversions supported by converters of
   * that class are no longer supported, if the class is a converter factory class, all conversions
   * supported by converters created by the converter factory are no longer supported.
   *
   * @param clazz the converter class or converter factory class
   */
  public static synchronized boolean deregister(Class<?> clazz) {
    if (clazz != null) {
      Class<?> usedClass = getUserClass(clazz);
      if (Converter.class.isAssignableFrom(usedClass)) {
        return isNotEmpty(
            removeIfValue(SUPPORT_CONVERTERS, v -> getUserClass(v.getClass()).equals(usedClass)));
      } else if (ConverterFactory.class.isAssignableFrom(usedClass)) {
        CONVERTER_FACTORIES.removeIf(f -> getUserClass(f.getClass()).equals(usedClass));
        return isNotEmpty(
            removeIfValue(SUPPORT_CONVERTERS, v -> (v instanceof FactoryConverter<?, ?> fc)
                && getUserClass(fc.getFactory().getClass()).equals(usedClass)));
      }
    }
    return false;
  }

  /**
   * Remove the registered converter with the given source type and target class. After removed the
   * conversions from the given sourceClass to the given targetClass will no longer supported.
   *
   * @param sourceClass the source class
   * @param targetClass the target class
   */
  public static synchronized boolean deregister(Class<?> sourceClass, Class<?> targetClass) {
    if (sourceClass != null && targetClass != null) {
      return deregister(ConverterType.of(sourceClass, targetClass));
    }
    return false;
  }

  /**
   * Remove the registered converter. After removed the conversions supported by the converter are
   * no longer supported.
   *
   * @param <S> the source type
   * @param <T> the target type
   * @param converter the converter to be deregistered
   */
  public static synchronized <S, T> boolean deregister(Converter<S, T> converter) {
    return isNotEmpty(removeIfValue(SUPPORT_CONVERTERS, v -> v.equals(converter)));
  }

  /**
   * Remove the registered converter factory, all types supported by converters that created by the
   * converter factory will no longer be supported.
   *
   * @param <S> the source type
   * @param <T> the target type
   * @param converterFactory the converter factory to be deregistered
   */
  public static synchronized <S, T> boolean deregister(ConverterFactory<S, T> converterFactory) {
    boolean removed = CONVERTER_FACTORIES.remove(converterFactory);
    removed |= isNotEmpty(removeIfValue(SUPPORT_CONVERTERS,
        v -> (v instanceof FactoryConverter<?, ?> fc) && fc.getFactory().equals(converterFactory)));
    return removed;
  }

  /**
   * Remove the registered converter with the given converter type.
   *
   * @param converterType the converter type to be deregistered
   */
  public static synchronized boolean deregister(ConverterType<?, ?> converterType) {
    if (converterType != null) {
      return SUPPORT_CONVERTERS.remove(converterType) != null;
    }
    return false;
  }

  /**
   * Remove the not support type conversion.
   *
   * @param sourceClass the source type
   * @param targetClass the target type
   */
  public static synchronized boolean deregisterNotSupportType(Class<?> sourceClass,
      Class<?> targetClass) {
    return NOT_SUPPORT_TYPES.remove(ConverterType.of(sourceClass, targetClass));
  }

  /**
   * Return converters and their supported type conversions, except for converters created by
   * converter factories.
   *
   * @return current all supports converter type and converters
   */
  public static Map<ConverterType<?, ?>, Converter<?, ?>> getSupportConverters() {
    return Collections.unmodifiableMap(SUPPORT_CONVERTERS);
  }

  public static void main(String... strings) throws IOException {
    String path = "org/corant/shared/conversion/converter/";
    Resources.fromClassPath(path).forEach(x -> {
      if (x instanceof ClassResource) {
        Class<?> cls = ((ClassResource) x).load();
        if (!Modifier.isAbstract(cls.getModifiers())
            && AbstractConverter.class.isAssignableFrom(cls)) {
          System.out.println(((ClassResource) x).getClassName());
        }
      }
    });
  }

  /**
   * Register a converter to supports conversion.
   *
   * @param <S> the source type
   * @param <T> the target type
   * @param converter the converter to be register
   */
  public static synchronized <S, T> boolean register(Converter<S, T> converter) {
    if (converter != null) {
      Class[] types = resolveTypes(converter);
      return register(types[0], types[1], converter);
    }
    return false;
  }

  /**
   * Register a converter factory to supports conversion.
   *
   * @param <S> the source type
   * @param <T> the target type
   * @param converterFactory the converter factory to be registered
   */
  public static synchronized <S, T> boolean register(ConverterFactory<S, T> converterFactory) {
    if (converterFactory != null && !CONVERTER_FACTORIES.contains(converterFactory)) {
      CONVERTER_FACTORIES.add(converterFactory);
      CONVERTER_FACTORIES.sort(Sortable::compare);
      return true;
    }
    return false;
  }

  /**
   * Register not support type conversion for fast interrupt lookup.
   *
   * @param sourceClass the source class
   * @param targetClass the target class
   */
  public static synchronized boolean registerNotSupportType(Class<?> sourceClass,
      Class<?> targetClass) {
    return registerNotSupportType(ConverterType.of(sourceClass, targetClass));
  }

  /**
   * Register not support type conversion for fast interrupt lookup.
   *
   * @param converterType the converter that we don't support
   */
  public static synchronized boolean registerNotSupportType(ConverterType converterType) {
    if (!NOT_SUPPORT_TYPES.contains(converterType) && NOT_SUPPORT_TYPES.add(converterType)) {
      if (NOT_SUPPORT_TYPES.size() > 512) {
        ConverterType first = NOT_SUPPORT_TYPES.iterator().next();
        NOT_SUPPORT_TYPES.remove(first);
      }
      deregister(converterType);
      return true;
    }
    return false;
  }

  /**
   * Reset all conversions, clear all cached converters and converter factories and not support
   * types, reload converters and converter factories.
   */
  public static synchronized void reset() {
    NOT_SUPPORT_TYPES.clear();
    CONVERTER_FACTORIES.clear();
    NOT_SUPPORT_TYPES.clear();
    load();
  }

  static Converter<?, ?> getConverter(Class<?> sourceClass, Class<?> targetClass) {
    return SUPPORT_CONVERTERS.get(ConverterType.of(sourceClass, targetClass));
  }

  static Converter<?, ?> getConverter(ConverterType converterType) {
    return SUPPORT_CONVERTERS.get(converterType);
  }

  static List<ConverterFactory<?, ?>> getConverterFactories() {
    return CONVERTER_FACTORIES;
  }

  static Map<ConverterType<?, ?>, Converter<?, ?>> getConverters() {
    return SUPPORT_CONVERTERS;
  }

  static Set<ConverterType<?, ?>> getConverterTypes() {
    return SUPPORT_CONVERTERS.keySet();
  }

  static Set<ConverterType<?, ?>> getNotSyntheticConverterTypes() {
    return SUPPORT_CONVERTERS.entrySet().stream().filter(e -> !e.getValue().isSynthetic())
        .map(Entry::getKey).collect(Collectors.toSet());
  }

  static boolean isNotSupportType(Class<?> sourceClass, Class<?> targetClass) {
    return isNotSupportType(ConverterType.of(sourceClass, targetClass));
  }

  static boolean isNotSupportType(ConverterType<?, ?> type) {
    return NOT_SUPPORT_TYPES.contains(type);
  }

  static boolean isSupportType(Class<?> sourceClass, Class<?> targetClass) {
    return SUPPORT_CONVERTERS.containsKey(ConverterType.of(sourceClass, targetClass));
  }

  static boolean isSupportType(ConverterType<?, ?> type) {
    return SUPPORT_CONVERTERS.containsKey(type);
  }

  static synchronized void load() {
    ObjectPrimitiveArrayConverterFactories.FACTORIES.forEach(ConverterRegistry::register);
    Services.selectRequired(Converter.class).sorted(Sortable::reverseCompare)
        .forEach(ConverterRegistry::register);
    Services.selectRequired(ConverterFactory.class).sorted(Sortable::reverseCompare)
        .forEach(ConverterRegistry::register);
  }

  static synchronized <S, T> boolean register(Class<S> sourceClass, Class<T> targetClass,
      Converter<S, T> converter) {
    ConverterType<S, T> ct = ConverterType.of(sourceClass, targetClass);
    if (SUPPORT_CONVERTERS.put(ct, converter) != null) {
      // has been register, check pipe or not
      removeNotSupportType(ct);
      return true;
    }
    return false;
  }

  static synchronized <S, T> boolean register(Class<S> sourceClass, Class<T> targetClass,
      Converter<S, T> converter, ConverterFactory<?, ?> converterFactory) {
    ConverterType<S, T> ct = ConverterType.of(sourceClass, targetClass);
    if (SUPPORT_CONVERTERS.put(ct, new FactoryConverter(converterFactory, converter)) != null) {
      // has been register, check pipe or not
      removeNotSupportType(ct);
      return true;
    }
    return false;
  }

  static Class[] resolveTypes(Converter<?, ?> converter) {
    Class[] classes = resolveClasses(
        Types.getParameterizedTypes(getUserClass(shouldNotNull(converter)), Converter.class));
    shouldBeTrue(classes.length == 2, "The converter %s parameterized type must be actual type!",
        converter.toString());
    return classes;
  }

  static Type[] resolveTypes(ConverterFactory<?, ?> converterFactory) {
    Type[] types = Types.getParameterizedTypes(getUserClass(shouldNotNull(converterFactory)),
        ConverterFactory.class);
    shouldBeTrue(types.length == 2 && types[0] instanceof Class,
        "The converter %s parameterized type must be actual type!", converterFactory.toString());
    return types;
  }

  private static void removeNotSupportType(ConverterType<?, ?> converterType) {
    NOT_SUPPORT_TYPES.removeIf(c -> c.match(converterType));
  }

  private static Class[] resolveClasses(Type[] types) {
    Class[] classes = new Class<?>[types.length];
    int i = 0;
    for (Type type : types) {
      Class<?> typeClass = Classes.getClass(type);
      if (typeClass != null) {
        classes[i] = typeClass;
        i++;
      }
    }
    return Arrays.copyOf(classes, i);
  }
}
