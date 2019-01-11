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

import static org.corant.shared.util.ObjectUtils.asString;
import java.util.Map;
import java.util.logging.Logger;
import org.corant.shared.conversion.ConversionException;
import org.corant.shared.conversion.Converter;

/**
 * corant-shared
 *
 * @author bingo 下午4:59:59
 *
 */
public abstract class AbstractConverter<S, T> implements Converter<S, T> {

  private T defaultValue = null;
  private boolean throwException = true;
  protected Logger logger = Logger.getLogger(this.getClass().getName());

  public AbstractConverter() {
    super();
  }

  /**
   * @param throwException
   */
  public AbstractConverter(boolean throwException) {
    super();
    this.setThrowException(throwException);
  }

  /**
   * @param defaultValue
   */
  public AbstractConverter(T defaultValue) {
    this.setDefaultValue(defaultValue);
  }

  /**
   * @param defaultValue
   * @param throwException
   */
  public AbstractConverter(T defaultValue, boolean throwException) {
    super();
    this.setDefaultValue(defaultValue);
    this.setThrowException(throwException);
  }

  @Override
  public T apply(S value, Map<String, ?> hints) {
    T result = null;
    try {
      result = value == null ? null : convert(value, hints);
    } catch (Exception e) {
      if (isThrowException()) {
        throw new ConversionException(e);
      } else {
        logger.warning(() -> String.format("Can not convert %s", asString(value)));
      }
    }
    return result == null ? getDefaultValue() : result;
  }

  /**
   *
   * @return the defaultValue
   */
  public T getDefaultValue() {
    return defaultValue;
  }

  /**
   *
   * @return the throwException
   */
  @Override
  public boolean isThrowException() {
    return throwException;
  }

  protected abstract T convert(S value, Map<String, ?> hints) throws Exception;

  /**
   *
   * @param defaultValue the defaultValue to set
   */
  protected void setDefaultValue(T defaultValue) {
    this.defaultValue = defaultValue;
  }

  /**
   *
   * @param throwException the throwException to set
   */
  protected void setThrowException(boolean throwException) {
    this.throwException = throwException;
  }

}
