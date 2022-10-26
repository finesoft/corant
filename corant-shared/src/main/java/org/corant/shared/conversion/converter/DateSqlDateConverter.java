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

import java.util.Date;
import java.util.Map;

/**
 * corant-shared
 *
 * <p>
 * FIXME to be removed, since we have date temporal converter factory
 *
 * @author bingo 上午10:49:23
 *
 */
public class DateSqlDateConverter extends AbstractConverter<Date, java.sql.Date> {

  /**
   * @see AbstractConverter#AbstractConverter()
   */
  public DateSqlDateConverter() {}

  /**
   * @see AbstractConverter#AbstractConverter(boolean)
   */
  public DateSqlDateConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object)
   */
  public DateSqlDateConverter(java.sql.Date defaultValue) {
    super(defaultValue);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object,boolean)
   */
  public DateSqlDateConverter(java.sql.Date defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  public boolean isPossibleDistortion() {
    return true;
  }

  @Override
  protected java.sql.Date doConvert(Date value, Map<String, ?> hints) throws Exception {
    return value == null ? null : new java.sql.Date(value.getTime());
  }

}
