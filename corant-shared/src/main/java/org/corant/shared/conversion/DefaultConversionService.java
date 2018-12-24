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

import static org.corant.shared.util.MapUtils.asMap;
import java.util.Collection;
import javax.enterprise.context.ApplicationScoped;
import org.corant.shared.conversion.ConverterRegistry.ConverterType;

/**
 *
 * @author bingo 上午12:11:08
 *
 */
@ApplicationScoped
public class DefaultConversionService implements ConversionService {

  @Override
  public <C extends Collection<T>, T> C convert(Object value, Class<C> collectionClazz,
      Class<T> clazz, Object... hints) {
    return Conversions.convert(value, collectionClazz, clazz, asMap(hints));
  }

  @Override
  public <T> T convert(Object value, Class<T> clazz, Object... hints) {
    return Conversions.convert(value, clazz, asMap(hints));
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
  public <T> void register(Converter<?, ?> converter) {
    ConverterRegistry.register(converter);
  }

}
