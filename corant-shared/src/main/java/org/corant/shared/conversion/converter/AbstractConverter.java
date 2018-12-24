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

import java.util.Map;
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
  private boolean useNullValueIfErr = false;
  private boolean useDefaultValueIfErr = true;

  /**
   * @param defaultValue
   */
  public AbstractConverter(T defaultValue) {
    super();
    this.setDefaultValue(defaultValue);
  }

  /**
   * @param defaultValue
   * @param useNullValueIfErr
   * @param useDefaultValueIfErr
   */
  public AbstractConverter(T defaultValue, boolean useNullValueIfErr,
      boolean useDefaultValueIfErr) {
    this(defaultValue);
    this.setUseNullValueIfErr(useNullValueIfErr);
    this.setUseDefaultValueIfErr(useDefaultValueIfErr);
  }

  @Override
  public T apply(S value, Map<String, ?> hints) {
    try {
      return value == null ? null : convert(value, hints);
    } catch (Exception e) {
      if (isUseDefaultValueIfErr()) {
        return getDefaultValue();
      } else if (isUseNullValueIfErr()) {
        return null;
      } else {
        throw new ConversionException(e);
      }
    }
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
   * @return the useDefaultValueIfErr
   */
  @Override
  public boolean isUseDefaultValueIfErr() {
    return useDefaultValueIfErr;
  }

  /**
   *
   * @return the useDefaultValueIfNull
   */
  @Override
  public boolean isUseNullValueIfErr() {
    return useNullValueIfErr;
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
   * @param useDefaultValueIfErr the useDefaultValueIfErr to set
   */
  protected void setUseDefaultValueIfErr(boolean useDefaultValueIfErr) {
    this.useDefaultValueIfErr = useDefaultValueIfErr;
  }

  /**
   *
   * @param useDefaultValueIfNull the useDefaultValueIfNull to set
   */
  protected void setUseNullValueIfErr(boolean useDefaultValueIfNull) {
    this.useNullValueIfErr = useDefaultValueIfNull;
  }
}
