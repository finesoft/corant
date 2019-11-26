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
import static org.corant.shared.util.ClassUtils.defaultClassLoader;
import static org.corant.shared.util.ClassUtils.getUserClass;
import static org.corant.shared.util.CollectionUtils.setOf;
import static org.corant.shared.util.StreamUtils.streamOf;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.corant.shared.util.TypeUtils;

/**
 * corant-shared
 *
 * @author bingo 上午9:12:49
 *
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ConverterRegistry {

  static final Map<ConverterType<?, ?>, Converter<?, ?>> SUPPORT_CONVERTERS =
      new ConcurrentHashMap<>();
  static final Map<Type, ConverterFactory<?, ?>> SUPPORT_CONVERTER_FACTORIES =
      new ConcurrentHashMap<>();
  static final Map<ConverterType<?, ?>, Set<ConverterType<?, ?>>> SUPPORT_CONVERTER_PIPE_TYPES =
      new ConcurrentHashMap<>();
  static final Set<ConverterType<?, ?>> NOT_SUPPORT_TYPES =
      Collections.newSetFromMap(new ConcurrentHashMap<>());

  static {
    streamOf(ServiceLoader.load(Converter.class, defaultClassLoader()))
        .sorted((c1, c2) -> Integer.compare(c1.getPriority(), c2.getPriority()))
        .forEach(ConverterRegistry::register);
    streamOf(ServiceLoader.load(ConverterFactory.class, defaultClassLoader()))
        .sorted((c1, c2) -> Integer.compare(c1.getPriority(), c2.getPriority()))
        .forEach(ConverterRegistry::register);
  }

  public static synchronized void deregister(ConverterType<?, ?> converterType) {
    if (SUPPORT_CONVERTERS.remove(converterType) != null) {
      removeConverterPipeTypes(converterType);
    }
  }

  public static Map<ConverterType<?, ?>, Converter<?, ?>> getSupportConverters() {
    return Collections.unmodifiableMap(SUPPORT_CONVERTERS);
  }

  public static synchronized <S, T> void register(Converter<S, T> converter) {
    Type[] types =
        TypeUtils.getParameterizedTypes(getUserClass(shouldNotNull(converter)), Converter.class);
    shouldBeTrue(types.length == 2 && types[0] instanceof Class && types[1] instanceof Class,
        "The converter %s parametered type must be actual type!", converter.toString());
    register((Class) types[0], (Class) types[1], converter);
  }

  public static synchronized <S, T> void register(ConverterFactory<S, T> converter) {
    Type[] types = TypeUtils.getParameterizedTypes(getUserClass(shouldNotNull(converter)),
        ConverterFactory.class);
    shouldBeTrue(types.length == 2 && types[0] instanceof Class,
        "The converter %s parametered type must be actual type!", converter.toString());
    SUPPORT_CONVERTER_FACTORIES.put(types[1], converter);
  }

  public static synchronized void registerNotSupportType(Class<?> sourceClass,
      Class<?> targetClass) {
    registerNotSupportType(ConverterType.of(sourceClass, targetClass));
  }

  public static synchronized void registerNotSupportType(ConverterType converterType) {
    if (!NOT_SUPPORT_TYPES.contains(converterType) && NOT_SUPPORT_TYPES.add(converterType)
        && NOT_SUPPORT_TYPES.size() > 128) {
      ConverterType first = NOT_SUPPORT_TYPES.iterator().next();
      NOT_SUPPORT_TYPES.remove(first);
    }
  }

  static Converter<?, ?> getConverter(Class<?> sourceClass, Class<?> targetClass) {
    return SUPPORT_CONVERTERS.get(ConverterType.of(sourceClass, targetClass));
  }

  static Converter<?, ?> getConverter(ConverterType converterType) {
    return SUPPORT_CONVERTERS.get(converterType);
  }

  static Map<Type, ConverterFactory<?, ?>> getConverterFactories() {
    return SUPPORT_CONVERTER_FACTORIES;
  }

  static Map<ConverterType<?, ?>, Converter<?, ?>> getConverters() {
    return SUPPORT_CONVERTERS;
  }

  static Set<ConverterType<?, ?>> getConverterTypes() {
    return SUPPORT_CONVERTERS.keySet();
  }

  static Set<ConverterType<?, ?>> getNotSyntheticConverterTypes() {
    Set<ConverterType<?, ?>> types = SUPPORT_CONVERTERS.entrySet().stream()
        .filter(e -> e.getValue().isComposable()).map(e -> e.getKey()).collect(Collectors.toSet());
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

  static synchronized <S, T> void register(Class<S> sourceClass, Class<T> targetClass,
      Converter<S, T> converter, ConverterType<?, ?>... pipeTypes) {
    ConverterType<S, T> ct = ConverterType.<S, T>of(sourceClass, targetClass);
    if (SUPPORT_CONVERTERS.put(ct, converter) != null) {
      // has been register, check pipe or not
      removeNotSupportType(ct);
      removeConverterPipeTypes(ct);
    }
    if (pipeTypes.length > 0) {
      SUPPORT_CONVERTER_PIPE_TYPES.put(ct, setOf(pipeTypes));
    }
  }

  private static void removeConverterPipeTypes(ConverterType<?, ?> pipe) {
    Set<ConverterType<?, ?>> pipeKeys = streamOf(SUPPORT_CONVERTER_PIPE_TYPES)
        .filter(e -> e.getValue().contains(pipe)).map(Entry::getKey).collect(Collectors.toSet());
    pipeKeys.stream().forEach(r -> {
      deregister(r);
      SUPPORT_CONVERTER_PIPE_TYPES.remove(r);
    });
  }

  private static void removeNotSupportType(ConverterType<?, ?> converterType) {
    NOT_SUPPORT_TYPES.removeIf(
        p -> Converters.match(p, converterType.getSourceClass(), converterType.getTargetClass()));
  }
}
