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

import static org.corant.shared.util.Functions.optional;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Streams.streamOf;
import java.util.Map.Entry;
import java.util.Optional;
import org.corant.shared.conversion.converter.IdentityConverter;
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

  public static <S, T> Optional<Converter<S, T>> lookup(Class<S> sourceClass,
      Class<T> targetClass) {
    if (targetClass.isAssignableFrom(sourceClass)) {
      return optional((Converter<S, T>) IdentityConverter.INSTANCE);
    } else if (ConverterRegistry.isSupportType(sourceClass, targetClass)) {
      return optional(forceCast(ConverterRegistry.getConverter(sourceClass, targetClass)));
    } else if (ConverterRegistry.isNotSupportType(sourceClass, targetClass)) {
      return optional(null);
    } else {
      // find from registered converters
      Converter converter = getMatchedConverter(sourceClass, targetClass);
      if (converter == null) {
        // find from registered converter factories
        Pair<Converter, ConverterFactory> factoryConverter =
            getMatchedConverterFromFactory(sourceClass, targetClass);
        if (factoryConverter != null) {
          converter = factoryConverter.getKey();
          ConverterFactory converterFactory = factoryConverter.getValue();
          ConverterRegistry.register(sourceClass, targetClass, converter, converterFactory);
        }
        /*
         * if (converter == null) { // indirect way, try to traverse and connect the converters that
         * meet the // requirements from the available converters Set<ConverterType<?, ?>>
         * pipeConverterTypes = new LinkedHashSet<>(); converter =
         * ExperimentalConverters.getMatchedConverterx(sourceClass, targetClass,
         * pipeConverterTypes::addAll); if (converter != null) {
         * ConverterRegistry.register(sourceClass, targetClass, (Converter<S, T>) converter,
         * pipeConverterTypes.toArray(new ConverterType[pipeConverterTypes.size()])); } }
         */
      } else {
        ConverterRegistry.register(sourceClass, targetClass, converter);
      }
      if (converter != null) {
        return optional(forceCast(converter));
      } else {
        ConverterRegistry.registerNotSupportType(sourceClass, targetClass);
        return optional(null);
      }
    }
  }

  static synchronized Converter getMatchedConverter(Class<?> sourceClass, Class<?> targetClass) {
    return streamOf(ConverterRegistry.getConverters())
        .filter(e -> match(e.getKey(), sourceClass, targetClass)).map(Entry::getValue).findFirst()
        .orElse(null);
  }

  static Pair<Converter, ConverterFactory> getMatchedConverterFromFactory(Class<?> sourceClass,
      Class<?> targetClass) {
    ConverterFactory factory = ConverterRegistry.getConverterFactories().stream()
        .filter(f -> f.isSupports(sourceClass, targetClass))
        .max((c1, c2) -> Integer.compare(c1.getPriority(), c2.getPriority())).orElse(null);
    if (factory != null) {
      // FIXME initialize parameter
      return Pair.of(factory.create(targetClass, null, true), factory);
    } else {
      return null;
    }
  }

  static boolean match(Class<?> a, Class<?> b) {
    return a.isAssignableFrom(b);
  }

  static boolean match(ConverterType<?, ?> converterType, Class<?> sourceClass,
      Class<?> targetClass) {
    return targetClass.isAssignableFrom(converterType.getTargetClass())
        && converterType.getSourceClass().isAssignableFrom(sourceClass);
  }

}
