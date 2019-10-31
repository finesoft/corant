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
package org.corant.config;

import static org.corant.shared.util.ConversionUtils.toObject;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.ObjectUtils.forceCast;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;
import javax.annotation.Priority;
import org.corant.shared.conversion.ConverterRegistry;
import org.corant.shared.util.ClassUtils;
import org.corant.shared.util.ConversionUtils;
import org.eclipse.microprofile.config.spi.Converter;

/**
 * corant-config
 *
 * @author bingo 下午4:27:06
 *
 */
public class ConfigConversion {

  public static final int BUILT_IN_CONVERTER_ORDINAL = 1;
  public static final int CUSTOMER_CONVERTER_ORDINAL = 100;

  public static final Set<Class<?>> BUILT_IN_SUPPORT_TYPES;
  public static final List<OrdinalConverter> BUILT_IN_CONVERTERS;

  static {
    Set<Class<?>> builtInSupTyps = new HashSet<>();
    List<OrdinalConverter> builtInCvts = new LinkedList<>();
    builtInSupTyps.add(String.class);
    builtInSupTyps.addAll(ClassUtils.WRAPPER_PRIMITIVE_MAP.keySet());
    ConverterRegistry.getSupportConverters().keySet().stream()
        .filter(ct -> ct.getSourceClass().isAssignableFrom(String.class))
        .map(ct -> ct.getTargetClass()).forEach(builtInSupTyps::add);
    builtInSupTyps.stream().map(OrdinalConverter::builtIn).forEach(builtInCvts::add);
    BUILT_IN_SUPPORT_TYPES = Collections.unmodifiableSet(builtInSupTyps);
    BUILT_IN_CONVERTERS = Collections.unmodifiableList(builtInCvts);
  }

  private final AtomicReference<Map<Type, Converter<?>>> converters;

  public ConfigConversion(List<OrdinalConverter> discoveredConverters) {
    List<OrdinalConverter> converters = new LinkedList<>(BUILT_IN_CONVERTERS);
    if (isNotEmpty(discoveredConverters)) {
      converters.addAll(discoveredConverters);
    }
    Collections.sort(converters, Comparator.comparingInt(OrdinalConverter::getOrdinal).reversed());
    Map<Type, Converter<?>> useConverters = new HashMap<>();
    for (OrdinalConverter oc : converters) {
      useConverters.computeIfAbsent(oc.type, (t) -> oc.converter);
    }
    this.converters = new AtomicReference<>(Collections.unmodifiableMap(useConverters));
  }

  public static int findPriority(Class<?> clazz) {
    Priority priorityAnnot = clazz.getAnnotation(Priority.class);
    return null != priorityAnnot ? priorityAnnot.value() : CUSTOMER_CONVERTER_ORDINAL;
  }

  public static Type getTypeOfConverter(Class<?> clazz) {
    if (clazz.equals(Object.class)) {
      return null;
    }
    Type[] genericInterfaces = clazz.getGenericInterfaces();
    for (Type genericInterface : genericInterfaces) {
      if (genericInterface instanceof ParameterizedType) {
        ParameterizedType pt = (ParameterizedType) genericInterface;
        if (pt.getRawType().equals(Converter.class)) {
          Type[] typeArguments = pt.getActualTypeArguments();
          if (typeArguments.length != 1) {
            throw new IllegalStateException("Converter " + clazz + " must be a ParameterizedType.");
          }
          return typeArguments[0];
        }
      }
    }
    return getTypeOfConverter(clazz.getSuperclass());
  }

  public boolean isSupport(Class<?> cls) {
    return converters.get().containsKey(cls);
  }

  <T> T convert(String value, Class<T> propertyType) {
    if (propertyType == String.class) {
      return forceCast(value);
    } else {
      final Converter<?> converter = converters.get().get(propertyType);
      if (null != converter) {
        return forceCast(converter.convert(value));
      } else if (propertyType.isArray()) {
        final Class<?> propertyComponentType = propertyType.getComponentType();
        return forceCast(convert(ConfigUtils.splitValue(value), propertyComponentType));
      } else if (List.class.isAssignableFrom(propertyType)) {
        return forceCast(convert(ConfigUtils.splitValue(value), String.class, ArrayList::new));
      } else if (Set.class.isAssignableFrom(propertyType)) {
        return forceCast(convert(ConfigUtils.splitValue(value), String.class, ArrayList::new));
      } else {
        return toObject(value, propertyType);
      }
    }
  }

  /**
   * convert array
   *
   * @param <T>
   * @param values
   * @param propertyComponentType
   * @return convert
   */
  <T> T[] convert(String[] values, Class<T> propertyComponentType) {
    Object array = Array.newInstance(propertyComponentType, values.length);
    for (int i = 0; i < values.length; i++) {
      Array.set(array, i, convert(values[i], propertyComponentType));
    }
    return forceCast(array);
  }

  /**
   * convert collection
   *
   * @param <T>
   * @param <C>
   * @param values
   * @param propertyItemType
   * @param collectionFactory
   * @return convert
   */
  <T, C extends Collection<T>> C convert(String[] values, Class<T> propertyItemType,
      IntFunction<C> collectionFactory) {
    int length = values.length;
    final C collection = collectionFactory.apply(length);
    for (String value : values) {
      collection.add(convert(value, propertyItemType));
    }
    return collection;
  }

  static class OrdinalConverter {
    final Class<?> type;
    final Converter<?> converter;
    final int ordinal;

    OrdinalConverter(Class<?> type, Converter<?> converter, int ordinal) {
      this.type = type;
      this.converter = converter;
      this.ordinal = ordinal;
    }

    static OrdinalConverter builtIn(Class<?> type) {
      return new OrdinalConverter(type, s -> ConversionUtils.toObject(s, type),
          BUILT_IN_CONVERTER_ORDINAL);
    }

    int getOrdinal() {
      return ordinal;
    }
  }

}
