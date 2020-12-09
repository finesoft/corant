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

import java.sql.Date;
import java.time.LocalDate;
import java.util.Map;

/**
 * corant-shared
 *
 * @author bingo 上午11:09:37
 *
 */
public class SqlDateLocalDateConverter extends AbstractConverter<Date, LocalDate> {

  public SqlDateLocalDateConverter() {
    super();
  }

  /**
   * @param throwException
   */
  public SqlDateLocalDateConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @param defaultValue
   */
  public SqlDateLocalDateConverter(LocalDate defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param throwException
   */
  public SqlDateLocalDateConverter(LocalDate defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  public boolean isPossibleDistortion() {
    return false;
  }

  @Override
  protected LocalDate convert(Date value, Map<String, ?> hints) throws Exception {
    if (value == null) {
      return getDefaultValue();
    }
    return value.toLocalDate();
  }

}
