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
package org.corant.suites.cdi;

import static org.corant.shared.util.MapUtils.mapOf;
import java.util.Collection;
import java.util.function.Supplier;
import javax.enterprise.context.ApplicationScoped;
import org.corant.shared.conversion.Conversions;
import org.corant.shared.conversion.Converter;
import org.corant.shared.conversion.ConverterRegistry;
import org.corant.shared.conversion.ConverterType;
import org.corant.shared.conversion.Converters;

/**
 *
 * corant-kernel
 *
 * @author bingo 上午12:16:42
 *
 */
public interface ConversionService {

  <C extends Collection<T>, T> C convert(final Object value, final Class<C> collectionClazz,
      final Class<T> clazz, Object... hints);

  <T> T convert(final Object value, final Class<T> clazz, Object... hints);

  <C extends Collection<T>, T> C convert(final Object value, final Class<T> clazz,
      final Supplier<C> collectionFactory, Object... hints);

  void deregister(ConverterType<?, ?> converterType);

  <S, T> Converter<S, T> getConverter(final Class<S> sourceType, final Class<T> targetType);

  void register(Converter<?, ?> converter);

  /**
   * corant-kernel
   *
   * @author bingo 下午6:47:49
   *
   */
  @ApplicationScoped
  public static class DefaultConversionService implements ConversionService {

    @Override
    public <C extends Collection<T>, T> C convert(Object value, Class<C> collectionClazz,
        Class<T> clazz, Object... hints) {
      return Conversions.convert(value, collectionClazz, clazz, mapOf(hints));
    }

    @Override
    public <T> T convert(Object value, Class<T> clazz, Object... hints) {
      return Conversions.convert(value, clazz, mapOf(hints));
    }

    @Override
    public <C extends Collection<T>, T> C convert(Object value, Class<T> clazz,
        Supplier<C> collectionFactory, Object... hints) {
      return Conversions.convert(value, clazz, collectionFactory, mapOf(hints));
    }

    @Override
    public void deregister(ConverterType<?, ?> converterType) {
      ConverterRegistry.deregister(converterType);
    }

    @Override
    public <S, T> Converter<S, T> getConverter(Class<S> sourceType, Class<T> targetType) {
      return Converters.lookup(sourceType, targetType, 3).orElse(null);
    }

    @Override
    public void register(Converter<?, ?> converter) {
      ConverterRegistry.register(converter);
    }

  }
}
