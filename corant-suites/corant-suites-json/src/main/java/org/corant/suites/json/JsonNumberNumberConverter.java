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
package org.corant.suites.json;

import java.util.Map;
import javax.json.JsonNumber;
import org.corant.shared.conversion.converter.AbstractConverter;

/**
 * corant-suites-json
 *
 * @author bingo 下午6:37:13
 *
 */
public class JsonNumberNumberConverter extends AbstractConverter<JsonNumber, Number> {

  private static final long serialVersionUID = 7201796053881772463L;

  public JsonNumberNumberConverter() {
    super();
  }

  /**
   * @param throwException
   */
  public JsonNumberNumberConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @param defaultValue
   */
  public JsonNumberNumberConverter(Number defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param throwException
   */
  public JsonNumberNumberConverter(Number defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  protected Number convert(JsonNumber value, Map<String, ?> hints) throws Exception {
    return value.numberValue();
  }

}
