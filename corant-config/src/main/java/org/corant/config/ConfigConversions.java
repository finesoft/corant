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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
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
public class ConfigConversions {

  static final Set<Class<?>> SUPPORT_TYPES =
      new HashSet<>(ClassUtils.WRAPPER_PRIMITIVE_MAP.keySet());
  static final List<OrdinalConverter> BUILT_IN_CONVERTERS = new LinkedList<>();
  static final int BUILT_IN_ORDINAL = 1;

  static {
    SUPPORT_TYPES.add(String.class);
    ConverterRegistry.getSupportConverters().keySet().stream()
        .filter(ct -> ct.getSourceClass().isAssignableFrom(String.class)).forEach(ct -> {
          SUPPORT_TYPES.add(ct.getTargetClass());
        });
    SUPPORT_TYPES.forEach(t -> {
      BUILT_IN_CONVERTERS
          .add(new OrdinalConverter(t, s -> ConversionUtils.toObject(s, t), BUILT_IN_ORDINAL));
    });
  }

  public static int findPriority(Class<?> clazz) {
    Priority priorityAnnot = clazz.getAnnotation(Priority.class);
    return null != priorityAnnot ? priorityAnnot.value() : 100;
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

  public static boolean isSupport(Class<?> cls) {
    return SUPPORT_TYPES.contains(cls);
  }

  static class ArrayConverter {

  }

  static class CollectionConverter<T, C extends Collection<T>> implements Converter<C> {

    @Override
    public C convert(String value) {
      return null;
    }

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

    int getOrdinal() {
      return ordinal;
    }
  }

}
