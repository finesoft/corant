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
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import org.corant.shared.conversion.ConversionException;

/**
 * corant-shared
 *
 * @author bingo 上午11:09:37
 *
 */
public class SqlDateInstantConverter extends AbstractTemporalConverter<Date, Instant> {

  private static final long serialVersionUID = 5714969977056601989L;

  public SqlDateInstantConverter() {
    super();
  }

  /**
   * @param throwException
   */
  public SqlDateInstantConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @param defaultValue
   */
  public SqlDateInstantConverter(Instant defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param throwException
   */
  public SqlDateInstantConverter(Instant defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  public boolean isPossibleDistortion() {
    return true;
  }

  @Override
  protected Instant convert(Date value, Map<String, ?> hints) throws Exception {
    if (value == null) {
      return getDefaultValue();
    }
    // return Instant.ofEpochMilli(((java.util.Date) value).getTime());
    Optional<ZoneId> zoneId = resolveHintZoneId(hints);
    if (zoneId.isPresent()) {
      return value.toLocalDate().atStartOfDay(zoneId.get()).toInstant();
    } else if (!isStrict(hints)) {
      return value.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
    } else {
      throw new ConversionException("Can't not convert java.sql.Date %s to Instant", value);
    }
  }

}
