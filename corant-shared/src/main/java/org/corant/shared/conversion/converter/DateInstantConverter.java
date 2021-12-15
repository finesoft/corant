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

import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * corant-shared
 *
 * @author bingo 上午10:49:23
 *
 */
public class DateInstantConverter extends AbstractConverter<Date, Instant> {

  /**
   * @see AbstractConverter#AbstractConverter()
   */
  public DateInstantConverter() {}

  /**
   * @see AbstractConverter#AbstractConverter(boolean)
   */
  public DateInstantConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object)
   */
  public DateInstantConverter(Instant defaultValue) {
    super(defaultValue);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object,boolean)
   */
  public DateInstantConverter(Instant defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  public boolean isPossibleDistortion() {
    return true;
  }

  @Override
  protected Instant convert(Date value, Map<String, ?> hints) throws Exception {
    return value.toInstant();
  }

}
