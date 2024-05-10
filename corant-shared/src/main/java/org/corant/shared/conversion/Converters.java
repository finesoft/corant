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

import static org.corant.shared.util.Streams.streamOf;
import java.util.Map.Entry;
import org.corant.shared.conversion.converter.IdentityConverter;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.ubiquity.Tuple.Pair;

/**
 * corant-shared
 *
 * <pre>
 * Lookup condition:
 * 1.The parameter source class must be equals or extends the source class of converter supported.
 * 2.For converter the target class of converter supported must be equals or extends the parameter
 * target class.
 * 3.For converter factory the target class of parameter must be equals or extends the target class
 * of converter factory supported.
 * </pre>
 *
 * @author bingo 下午2:12:57
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class Converters {

  public static <S, T> Converter<S, T> lookup(Class<S> sourceClass, Class<T> targetClass) {
    if (targetClass.isAssignableFrom(sourceClass)) {
      return (Converter<S, T>) IdentityConverter.INSTANCE;
    } else {
      ConverterType<S, T> type = ConverterType.of(sourceClass, targetClass);
      Converter converter = ConverterRegistry.getConverter(type);
      if (converter != null) {
        return converter;
      } else if (ConverterRegistry.isNotSupportType(type)) {
        return null;
      } else {
        // find from registered converters
        converter = getMatchedConverter(type);
        if (converter != null) {
          ConverterRegistry.register(sourceClass, targetClass, converter);
          return converter;
        } else {
          // find from registered converter factories
          Pair<Converter, ConverterFactory> factoryConverter = getMatchedConverterFromFactory(type);
          if (factoryConverter != null) {
            converter = factoryConverter.getKey();
            ConverterFactory converterFactory = factoryConverter.getValue();
            ConverterRegistry.register(sourceClass, targetClass, converter, converterFactory);
            return converter;
          }
        }
        ConverterRegistry.registerNotSupportType(type);
        return null;
      }
    }
  }

  static synchronized Converter getMatchedConverter(ConverterType<?, ?> type) {
    return streamOf(ConverterRegistry.getConverters()).filter(e -> e.getKey().match(type))
        .map(Entry::getValue).findFirst().orElse(null);
  }

  static Pair<Converter, ConverterFactory> getMatchedConverterFromFactory(
      ConverterType<?, ?> type) {
    ConverterFactory factory = ConverterRegistry.getConverterFactories().stream()
        .filter(f -> f.isSupports(type.getSourceClass(), type.getTargetClass()))
        .min(Sortable::compare).orElse(null);
    if (factory != null) {
      // FIXME initialize parameter
      return Pair.of(factory.create(type.getTargetClass(), null, true), factory);
    } else {
      return null;
    }
  }

  static boolean match(Class<?> a, Class<?> b) {
    return a.isAssignableFrom(b);
  }

}
