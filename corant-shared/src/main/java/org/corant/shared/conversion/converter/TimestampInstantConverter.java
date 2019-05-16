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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

/**
 * corant-shared
 *
 * @author bingo 上午11:09:37
 *
 */
public class TimestampInstantConverter extends AbstractConverter<Timestamp, Instant> {

  public TimestampInstantConverter() {
    super();
  }

  /**
   * @param throwException
   */
  public TimestampInstantConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @param defaultValue
   */
  public TimestampInstantConverter(Instant defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param throwException
   */
  public TimestampInstantConverter(Instant defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  public boolean isPossibleDistortion() {
    return false;
  }

  @Override
  protected Instant convert(Timestamp value, Map<String, ?> hints) throws Exception {
    return value.toInstant();
  }

}