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
package org.corant.shared.conversion.converter;

import static org.corant.shared.util.Objects.asString;
import static org.corant.shared.util.Objects.defaultObject;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.corant.shared.conversion.ConversionException;
import org.corant.shared.conversion.Converter;
import org.corant.shared.conversion.ConverterHints;

/**
 * corant-shared
 *
 * @author bingo 下午4:59:59
 */
public abstract class AbstractConverter<S, T> implements Converter<S, T> {

  protected T defaultValue = null;
  protected boolean throwException = true;
  protected final Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * Constructs a converter, if the converted object is null, the conversion result is null; if it
   * cannot be converted or an exception occurs during the conversion, an exception will be thrown.
   */
  protected AbstractConverter() {}

  /**
   * Constructs a converter, if the converted object is null, the conversion result is null.
   *
   * @param throwException whether to throw an exception when a conversion error occurs
   */
  protected AbstractConverter(boolean throwException) {
    this.throwException = throwException;
  }

  /**
   * Constructs a converter, if the converted object is null, the conversion result is the given
   * {@code defaultValue}; if it cannot be converted or an exception occurs during the conversion,
   * an exception will be thrown.
   *
   * @param defaultValue if the object to be converted is null or the converted result is null then
   *        use this value as converted as result
   */
  protected AbstractConverter(T defaultValue) {
    this.defaultValue = defaultValue;
  }

  /**
   * Constructs a converter with the given default value and whether to throw exceptions.
   *
   * @param defaultValue if the object to be converted is null or the converted result is null then
   *        use this value as converted as result
   * @param throwException whether to throw an exception when a conversion error occurs
   */
  protected AbstractConverter(T defaultValue, boolean throwException) {
    this.defaultValue = defaultValue;
    this.throwException = throwException;
  }

  protected static boolean isStrict(Map<String, ?> hints) {
    if (ConverterHints.containsKey(hints, ConverterHints.CVT_TEMPORAL_STRICTLY_KEY)) {
      return ConverterHints.getHint(hints, ConverterHints.CVT_TEMPORAL_STRICTLY_KEY);
    }
    return false;
  }

  @Override
  public T apply(S value, Map<String, ?> hints) {
    T result = null;
    try {
      result = value == null ? null : convert(value, hints);
    } catch (Exception e) {
      if (isThrowException()) {
        throw new ConversionException(e, "Can not convert %s.", asString(value));
      } else {
        logger.log(Level.WARNING, e, () -> String.format("Can not convert %s.", asString(value)));
      }
    }
    return defaultObject(result, this::getDefaultValue);
  }

  /**
   * If the object to be converted is null or the converted result is null then use this value as
   * converted as result. The default value is null.
   *
   * @return the defaultValue
   */
  public T getDefaultValue() {
    return defaultValue;
  }

  /**
   * Returns whether to throw an exception when a conversion error occurs
   */
  @Override
  public boolean isThrowException() {
    return throwException;
  }

  /**
   * The actual conversion method to facilitate the implementation of subclasses.
   *
   * @param value the object to be converted, do not need to check for null.
   * @param hints the conversion hints use to intervene in the conversion process
   * @return the converted value
   * @throws Exception occurred during the conversion process
   */
  protected abstract T convert(S value, Map<String, ?> hints) throws Exception;

  protected void warn(Class<?> target, Object object) {
    if (object == null) {
      logger.warning(
          () -> String.format("The conversion of an object from %s to %s may cause distortion!",
              "Object", target.getName()));
    } else {
      logger
          .warning(() -> String.format("The conversion of [%s] from %s to %s may cause distortion!",
              asString(object), object.getClass().getName(), target.getName()));
    }
  }

}
