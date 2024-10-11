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

import java.util.Map;
import org.corant.shared.normal.Priorities;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-shared
 * <p>
 * Object converter factory, used to build the converter for conversion between objects, allows
 * support for multiple source types and target types. In actual applications, the converter created
 * by the converter factory may be cached or combined into a combined converter.
 *
 * <p>
 * the generic parameter S is the source type that converter accepts and the generic parameter T is
 * the target type that converter will convert source to.
 *
 * <p>
 * All converter factories are loaded by ServiceLoader, which means that they need to be declared in
 * META-INF/service.
 *
 * @author bingo 下午12:08:59
 */
public interface ConverterFactory<S, T> extends Sortable {

  /**
   * Returns a converter for given target type
   *
   * @param targetClass the target class
   * @param defaultValue the default value when the source object is null
   * @param throwException whether to throw an exception when it cannot be converted
   * @return the converter
   */
  Converter<S, T> create(Class<T> targetClass, T defaultValue, boolean throwException);

  /**
   * Returns the converter priority, used to identify the priority of converter factories of the
   * same type, the smaller the value, the more preferred. Default is
   * {@link Priorities#FRAMEWORK_LOWER}
   *
   * @return the priority
   */
  @Override
  default int getPriority() {
    return Priorities.FRAMEWORK_LOWER;
  }

  /**
   * Returns whether the converter factory supports the conversion of the given source class and
   * target class
   *
   * @param sourceClass the source object class that will be converted
   * @param targetClass the target class that will be converted to
   */
  default boolean isSupports(Class<?> sourceClass, Class<?> targetClass) {
    return isSupportSourceClass(sourceClass) && isSupportTargetClass(targetClass);
  }

  /**
   * Returns whether the converter factory supports the conversion of the given source class
   *
   * @param sourceClass the source object class that will be converted
   */
  default boolean isSupportSourceClass(Class<?> sourceClass) {
    return false;
  }

  /**
   * Returns whether the converter factory supports the conversion of the given target class
   *
   * @param targetClass the target class that will be converted to
   */
  boolean isSupportTargetClass(Class<?> targetClass);

  /**
   * corant-shared
   *
   * @author bingo 17:55:45
   */
  class FactoryConverter<X, Y> implements Converter<X, Y> {

    protected final ConverterFactory<?, ?> factory;

    protected final Converter<X, Y> syntheticConverter;

    public FactoryConverter(ConverterFactory<?, ?> factory, Converter<X, Y> syntheticConverter) {
      this.factory = factory;
      this.syntheticConverter = syntheticConverter;
    }

    @Override
    public Y convert(X t, Map<String, ?> hints) {
      return syntheticConverter.convert(t, hints);
    }

    public ConverterFactory<?, ?> getFactory() {
      return factory;
    }

  }
}
