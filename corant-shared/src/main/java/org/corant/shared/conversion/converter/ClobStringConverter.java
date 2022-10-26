/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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

import java.io.Reader;
import java.sql.Clob;
import java.util.Map;
import org.corant.shared.conversion.ConverterHints;

/**
 * corant-shared
 *
 * @author bingo 上午12:37:24
 *
 */
public class ClobStringConverter extends AbstractConverter<Clob, String> {

  /**
   * @see AbstractConverter#AbstractConverter()
   */
  public ClobStringConverter() {}

  /**
   * @see AbstractConverter#AbstractConverter(boolean)
   */
  public ClobStringConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object)
   */
  public ClobStringConverter(String defaultValue) {
    super(defaultValue);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object,boolean)
   */
  public ClobStringConverter(String defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  protected String doConvert(Clob value, Map<String, ?> hints) throws Exception {
    StringBuilder sb = new StringBuilder();
    try (Reader reader = value.getCharacterStream()) {
      int c;
      while ((c = reader.read()) != -1) {
        sb.append((char) c);
      }
      if (ConverterHints.getHint(hints, ConverterHints.CVT_FREE_AFTER_CONVERTED, Boolean.TRUE)) {
        value.free();
      }
    }
    return sb.toString();
  }

}
