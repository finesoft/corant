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

import static org.corant.shared.util.CollectionUtils.asSet;
import static org.corant.shared.util.ObjectUtils.shouldBeTrue;
import static org.corant.shared.util.ObjectUtils.shouldNotNull;
import static org.corant.shared.util.StreamUtils.asStream;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.corant.shared.conversion.converter.DateInstantConverter;
import org.corant.shared.conversion.converter.NumberBigDecimalConverter;
import org.corant.shared.conversion.converter.NumberBigIntegerConverter;
import org.corant.shared.conversion.converter.NumberBooleanConverter;
import org.corant.shared.conversion.converter.NumberByteConverter;
import org.corant.shared.conversion.converter.NumberDoubleConverter;
import org.corant.shared.conversion.converter.NumberFloatConverter;
import org.corant.shared.conversion.converter.NumberInstantConverter;
import org.corant.shared.conversion.converter.NumberIntegerConverter;
import org.corant.shared.conversion.converter.NumberLocalDateConverter;
import org.corant.shared.conversion.converter.NumberLocalDateTimeConverter;
import org.corant.shared.conversion.converter.NumberLongConverter;
import org.corant.shared.conversion.converter.NumberShortConverter;
import org.corant.shared.conversion.converter.NumberZonedDateTimeConverter;
import org.corant.shared.conversion.converter.ObjectClassConveter;
import org.corant.shared.conversion.converter.SqlDateInstantConverter;
import org.corant.shared.conversion.converter.StringBigDecimalConverter;
import org.corant.shared.conversion.converter.StringBigIntegerConverter;
import org.corant.shared.conversion.converter.StringBooleanConverter;
import org.corant.shared.conversion.converter.StringByteConverter;
import org.corant.shared.conversion.converter.StringCharacterConveter;
import org.corant.shared.conversion.converter.StringCurrencyConverter;
import org.corant.shared.conversion.converter.StringDoubleConveter;
import org.corant.shared.conversion.converter.StringFloatConveter;
import org.corant.shared.conversion.converter.StringInstantConverter;
import org.corant.shared.conversion.converter.StringIntegerConverter;
import org.corant.shared.conversion.converter.StringLocalDateConverter;
import org.corant.shared.conversion.converter.StringLocalDateTimeConverter;
import org.corant.shared.conversion.converter.StringLocaleConverter;
import org.corant.shared.conversion.converter.StringLongConverter;
import org.corant.shared.conversion.converter.StringShortConverter;
import org.corant.shared.conversion.converter.StringTimeZoneConverter;
import org.corant.shared.conversion.converter.StringURLConverter;
import org.corant.shared.conversion.converter.StringZonedDateTimeConverter;
import org.corant.shared.conversion.converter.TemporalInstantConverter;
import org.corant.shared.conversion.converter.TemporalLocalDateConverter;
import org.corant.shared.conversion.converter.TemporalLocalDateTimeConverter;
import org.corant.shared.conversion.converter.TemporalZonedDateTimeConverter;
import org.corant.shared.conversion.converter.factory.ObjectEnumConverterFactory;
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
  static final Map<Class<?>, ConverterFactory<?, ?>> SUPPORT_CONVERTER_FACTORIES =
      new ConcurrentHashMap<>();
  static final Map<ConverterType<?, ?>, Set<ConverterType<?, ?>>> SUPPORT_CONVERTER_PIPE_TYPES =
      new ConcurrentHashMap<>();
  static final Set<ConverterType<?, ?>> NOT_SUPPORT_TYPES =
      Collections.newSetFromMap(new ConcurrentHashMap<>());

  static {
    register(new DateInstantConverter(null, false, false));
    register(new NumberBigDecimalConverter(null, false, false));
    register(new NumberBigIntegerConverter(null, false, false));
    register(new NumberBooleanConverter(null, false, false));
    register(new NumberByteConverter(null, false, false));
    register(new NumberDoubleConverter(null, false, false));
    register(new NumberFloatConverter(null, false, false));
    register(new NumberIntegerConverter(null, false, false));
    register(new NumberInstantConverter(null, false, false));
    register(new NumberLocalDateConverter(null, false, false));
    register(new NumberLocalDateTimeConverter(null, false, false));
    register(new NumberLongConverter(null, false, false));
    register(new NumberShortConverter(null, false, false));
    register(new NumberZonedDateTimeConverter(null, false, false));
    register(new ObjectClassConveter(null, false, false));
    register(new SqlDateInstantConverter(null, false, false));
    register(new StringBigDecimalConverter(null, false, false));
    register(new StringBigIntegerConverter(null, false, false));
    register(new StringBooleanConverter(null, false, false));
    register(new StringByteConverter(null, false, false));
    register(new StringCharacterConveter(null, false, false));
    register(new StringCurrencyConverter(null, false, false));
    register(new StringDoubleConveter(null, false, false));
    register(new StringFloatConveter(null, false, false));
    register(new StringInstantConverter(null, false, false));
    register(new StringIntegerConverter(null, false, false));
    register(new StringLocalDateConverter(null, false, false));
    register(new StringLocalDateTimeConverter(null, false, false));
    register(new StringLocaleConverter(null, false, false));
    register(new StringLongConverter(null, false, false));
    register(new StringShortConverter(null, false, false));
    register(new StringTimeZoneConverter(null, false, false));
    register(new StringURLConverter(null, false, false));
    register(new StringZonedDateTimeConverter(null, false, false));
    register(new TemporalInstantConverter(null, false, false));
    register(new TemporalLocalDateConverter(null, false, false));
    register(new TemporalLocalDateTimeConverter(null, false, false));
    register(new TemporalZonedDateTimeConverter(null, false, false));
    register(new ObjectEnumConverterFactory());
  }

  public synchronized static void deregister(ConverterType<?, ?> converterType) {
    if (SUPPORT_CONVERTERS.remove(converterType) != null) {
      removeConverterPipeTypes(converterType);
    }
  }

  // public static void main(String... pipeTypes) throws IOException {
  // ClassPaths
  // .from(ConverterRegistry.class.getClassLoader(), "org/corant/shared/conversion/converter")
  // .getClasses().map(x -> "register(new " + x.getSimpleName() + "(null, false, false));")
  // .forEach(System.out::println);
  // }

  public synchronized static <S, T> void register(Converter<S, T> converter) {
    Type[] types =
        TypeUtils.getParameterizedTypes(shouldNotNull(converter).getClass(), Converter.class);
    shouldBeTrue(types.length == 2 && types[0] instanceof Class && types[1] instanceof Class,
        "The converter %s parametered type must be actual type!", converter.toString());
    register((Class) types[0], (Class) types[1], converter);
  }

  public synchronized static <S, T> void register(ConverterFactory<S, T> converter) {
    Type[] types = TypeUtils.getParameterizedTypes(shouldNotNull(converter).getClass(),
        ConverterFactory.class);
    shouldBeTrue(types.length == 2 && types[0] instanceof Class,
        "The converter %s parametered type must be actual type!", converter.toString());
    SUPPORT_CONVERTER_FACTORIES.put((Class) types[0], converter);
  }

  public synchronized static void registerNotSupportType(Class<?> sourceClass,
      Class<?> targetClass) {
    registerNotSupportType(ConverterType.of(sourceClass, targetClass));
  }

  public synchronized static void registerNotSupportType(ConverterType converterType) {
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

  static Map<Class<?>, ConverterFactory<?, ?>> getConverterFactories() {
    return SUPPORT_CONVERTER_FACTORIES;
  }

  static Map<ConverterType<?, ?>, Converter<?, ?>> getConverters() {
    return SUPPORT_CONVERTERS;
  }

  static Set<ConverterType<?, ?>> getConverterTypes() {
    return SUPPORT_CONVERTERS.keySet();
  }

  static Set<ConverterType<?, ?>> getNotSyntheticConverterTypes() {
    Set<ConverterType<?, ?>> types = new HashSet<>(SUPPORT_CONVERTERS.keySet());
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

  synchronized static <S, T> void register(Class<S> sourceClass, Class<T> targetClass,
      Converter<S, T> converter, ConverterType<?, ?>... pipeTypes) {
    ConverterType<S, T> ct = ConverterType.<S, T>of(sourceClass, targetClass);
    if (SUPPORT_CONVERTERS.put(ct, converter) != null) {
      // has been register, check pipe or not
      removeNotSupportType(ct);
      removeConverterPipeTypes(ct);
    }
    if (pipeTypes.length > 0) {
      SUPPORT_CONVERTER_PIPE_TYPES.put(ct, asSet(pipeTypes));
    }
  }

  private static void removeConverterPipeTypes(ConverterType<?, ?> pipe) {
    Set<ConverterType<?, ?>> pipeKeys = asStream(SUPPORT_CONVERTER_PIPE_TYPES)
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

  /**
   * corant-shared
   *
   * @author bingo 下午4:36:23
   *
   */
  public static class ConverterType<S, T> {

    private final Class<S> sourceClass;

    private final Class<T> targetClass;

    /**
     * @param sourceClass
     * @param targetClass
     */
    public ConverterType(Class<S> sourceClass, Class<T> targetClass) {
      super();
      this.sourceClass = shouldNotNull(sourceClass);
      this.targetClass = shouldNotNull(targetClass);
    }

    public static <S, T> ConverterType<S, T> of(Class<S> sourceClass, Class<T> targetClass) {
      return new ConverterType<>(sourceClass, targetClass);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      ConverterType other = (ConverterType) obj;
      if (sourceClass == null) {
        if (other.sourceClass != null) {
          return false;
        }
      } else if (!sourceClass.equals(other.sourceClass)) {
        return false;
      }
      if (targetClass == null) {
        if (other.targetClass != null) {
          return false;
        }
      } else if (!targetClass.equals(other.targetClass)) {
        return false;
      }
      return true;
    }

    /**
     * @return the sourceClass
     */
    public Class<S> getSourceClass() {
      return sourceClass;
    }

    /**
     * @return the targetClass
     */
    public Class<T> getTargetClass() {
      return targetClass;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (sourceClass == null ? 0 : sourceClass.hashCode());
      result = prime * result + (targetClass == null ? 0 : targetClass.hashCode());
      return result;
    }

    @Override
    public String toString() {
      return "ConverterType [sourceClass=" + sourceClass + ", targetClass=" + targetClass + "]";
    }
  }
}
