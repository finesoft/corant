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
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.corant.shared.conversion.ConverterHints;

/**
 * corant-shared
 *
 * @author bingo 下午5:35:33
 *
 */
public class NumberInstantConverter extends AbstractTemporalConverter<Number, Instant> {

  public NumberInstantConverter() {
    super();
  }

  /**
   * @param throwException
   */
  public NumberInstantConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @param defaultValue
   */
  public NumberInstantConverter(Instant defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param throwException
   */
  public NumberInstantConverter(Instant defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  public boolean isPossibleDistortion() {
    return true;
  }

  @Override
  protected Instant convert(Number value, Map<String, ?> hints) throws Exception {
    if (ChronoUnit.SECONDS
        .equals(ConverterHints.getHint(hints, ConverterHints.CVT_TEMPORAL_EPOCH_KEY))) {
      return Instant.ofEpochSecond(value.longValue());
    } else {
      return Instant.ofEpochMilli(value.longValue());
    }
  }
}
