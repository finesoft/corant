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
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Sets.setOf;
import static org.corant.shared.util.Streams.streamOf;
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

  static final Map<ConverterType<?, ?>, ConverterFactory<?, ?>> SUPPORT_CONVERTER_FACTORIES =
      new ConcurrentHashMap<>();

  static final Map<ConverterType<?, ?>, Set<ConverterType<?, ?>>> SUPPORT_CONVERTER_PIPE_TYPES =
      new ConcurrentHashMap<>();

  static final Set<ConverterType<?, ?>> NOT_SUPPORT_TYPES =
      Collections.newSetFromMap(new ConcurrentHashMap<>());

  static {
    load();
  }

  /**
   * Remove the registered converter, the type supported by the converter will no longer be
   * supported. Note that if the type conversion({@link ConverterType}) involved is referenced by
   * the conversion pipeline, the conversion pipeline will also be removed, but the converter
   * pipeline may be reorganized when necessary.
   *
   * @param <S> the source type
   * @param <T> the target type
   * @param converter the converter to be deregistered
   */
  public static synchronized <S, T> void deregister(Converter<S, T> converter) {
    Class[] type = resolveTypes(converter);
    deregister(ConverterType.of(type[0], type[1])); // FIXME consider other ways
  }

  /**
   * Remove the registered converter factory, all types supported by converters that created by the
   * converter factory will no longer be supported. Note that if the type
   * conversion({@link ConverterType}) involved is referenced by the conversion pipeline, the
   * conversion pipeline will also be removed, but the converter pipeline may be reorganized when
   * necessary.
   *
   * @param <S> the source type
   * @param <T> the target type
   * @param converterFactory the converter factory to be deregistered
   */
  public static synchronized <S, T> void deregister(ConverterFactory<S, T> converterFactory) {
    CONVERTER_FACTORIES.remove(converterFactory);
    SUPPORT_CONVERTER_FACTORIES.entrySet().stream()
        .filter(e -> areEqual(e.getValue(), converterFactory)).map(Entry::getKey)
        .forEach(ConverterRegistry::deregister);
  }

  /**
   * Return converters and their supported type conversions, except for converters created by
   * converter factories.
   *
   * @return getSupportConverters
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
   * Register a converter
   *
   * @param <S> the source type
   * @param <T> the target type
   * @param converter the converter to be register
   */
  public static synchronized <S, T> void register(Converter<S, T> converter) {
    Class[] types = resolveTypes(converter);
    register(types[0], types[1], converter);
  }

  /**
   * Register a converter factory
   *
   * @param <S> the source type
   * @param <T> the target type
   * @param converterFactory the converter factory to be registered
   */
  public static synchronized <S, T> void register(ConverterFactory<S, T> converterFactory) {
    if (converterFactory != null && !CONVERTER_FACTORIES.contains(converterFactory)) {
      CONVERTER_FACTORIES.add(converterFactory);
      CONVERTER_FACTORIES.sort(Sortable::compare);
    }
  }

  /**
   * Register not support type conversion for fast interrupt lookup.
   *
   * @param sourceClass the source class
   * @param targetClass the target class
   */
  public static synchronized void registerNotSupportType(Class<?> sourceClass,
      Class<?> targetClass) {
    registerNotSupportType(ConverterType.of(sourceClass, targetClass));
  }

  /**
   * Register not support type conversion for fast interrupt lookup.
   *
   * @param converterType the converter that we don't support
   */
  public static synchronized void registerNotSupportType(ConverterType converterType) {
    if (!NOT_SUPPORT_TYPES.contains(converterType) && NOT_SUPPORT_TYPES.add(converterType)
        && NOT_SUPPORT_TYPES.size() > 128) {
      ConverterType first = NOT_SUPPORT_TYPES.iterator().next();
      NOT_SUPPORT_TYPES.remove(first);
      deregister(converterType);
    }
  }

  /**
   * Reset all conversions, clear all cached converters and converter factories and not support
   * types, reload converters and converter factories.
   */
  public static synchronized void reset() {
    NOT_SUPPORT_TYPES.clear();
    SUPPORT_CONVERTER_FACTORIES.clear();
    CONVERTER_FACTORIES.clear();
    SUPPORT_CONVERTER_PIPE_TYPES.clear();
    NOT_SUPPORT_TYPES.clear();
    load();
  }

  static synchronized void deregister(ConverterType<?, ?> converterType) {
    if (SUPPORT_CONVERTERS.remove(converterType) != null) {
      removeConverterPipeTypes(converterType); // FIXME consider other ways
    }
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
    Set<ConverterType<?, ?>> types = SUPPORT_CONVERTERS.entrySet().stream()
        .filter(e -> e.getValue().isComposable()).map(Entry::getKey).collect(Collectors.toSet());
    types.removeAll(SUPPORT_CONVERTER_PIPE_TYPES.keySet());
    return types;
  }

  static boolean isNotSupportType(Class<?> sourceClass, Class<?> targetClass) {
    return NOT_SUPPORT_TYPES.stream()
        .anyMatch(ct -> Converters.match(ct, sourceClass, targetClass));
  }

  static boolean isSupportType(Class<?> sourceClass, Class<?> targetClass) {
    return SUPPORT_CONVERTERS.containsKey(ConverterType.of(sourceClass, targetClass));
  }

  static synchronized void load() {
    ObjectPrimitiveArrayConverterFactories.FACTORIES.forEach(ConverterRegistry::register);
    Services.selectRequired(Converter.class).sorted(Sortable::reverseCompare)
        .forEach(ConverterRegistry::register);
    Services.selectRequired(ConverterFactory.class).sorted(Sortable::reverseCompare)
        .forEach(ConverterRegistry::register);
  }

  static synchronized <S, T> void register(Class<S> sourceClass, Class<T> targetClass,
      Converter<S, T> converter, ConverterFactory<?, ?> converterFactory) {
    ConverterType<S, T> ct = ConverterType.of(sourceClass, targetClass);
    if (SUPPORT_CONVERTERS.put(ct, converter) != null) {
      SUPPORT_CONVERTER_FACTORIES.put(ct, converterFactory);
      // has been register, check pipe or not
      removeNotSupportType(ct);
      removeConverterPipeTypes(ct);
    }
  }

  static synchronized <S, T> void register(Class<S> sourceClass, Class<T> targetClass,
      Converter<S, T> converter, ConverterType<?, ?>... pipeTypes) {
    ConverterType<S, T> ct = ConverterType.of(sourceClass, targetClass);
    if (SUPPORT_CONVERTERS.put(ct, converter) != null) {
      // has been register, check pipe or not
      removeNotSupportType(ct);
      removeConverterPipeTypes(ct);
    }
    if (isNotEmpty(pipeTypes)) {
      SUPPORT_CONVERTER_PIPE_TYPES.put(ct, setOf(pipeTypes));
    }
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

  private static void removeConverterPipeTypes(ConverterType<?, ?> pipe) {
    Set<ConverterType<?, ?>> pipeKeys = streamOf(SUPPORT_CONVERTER_PIPE_TYPES)
        .filter(e -> e.getValue().contains(pipe)).map(Entry::getKey).collect(Collectors.toSet());
    pipeKeys.forEach(r -> {
      deregister(r);
      SUPPORT_CONVERTER_PIPE_TYPES.remove(r);
    });
  }

  private static void removeNotSupportType(ConverterType<?, ?> converterType) {
    NOT_SUPPORT_TYPES.removeIf(
        p -> Converters.match(p, converterType.getSourceClass(), converterType.getTargetClass()));
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
