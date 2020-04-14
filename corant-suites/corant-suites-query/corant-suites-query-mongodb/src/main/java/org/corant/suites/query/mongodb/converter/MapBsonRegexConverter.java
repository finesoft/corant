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
package org.corant.suites.query.mongodb.converter;

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.StringUtils.asDefaultString;
import static org.corant.shared.util.StringUtils.isBlank;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.util.Map;
import org.bson.BsonRegularExpression;
import org.corant.shared.conversion.converter.AbstractConverter;

/**
 * corant-suites-query-mongodb
 *
 * @author bingo 上午10:04:31
 *
 */
@SuppressWarnings("rawtypes")
public class MapBsonRegexConverter extends AbstractConverter<Map, BsonRegularExpression> {

  private static final long serialVersionUID = -324446604179431073L;

  /**
   *
   */
  public MapBsonRegexConverter() {
    super();
  }

  /**
   * @param throwException
   */
  public MapBsonRegexConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @param defaultValue
   */
  public MapBsonRegexConverter(BsonRegularExpression defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param throwException
   */
  public MapBsonRegexConverter(BsonRegularExpression defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  protected BsonRegularExpression convert(Map value, Map<String, ?> hints) throws Exception {
    String pattern = null;
    String option = null;
    if (isEmpty(value) || isBlank(pattern = asDefaultString(value.get("pattern")))) {
      return getDefaultValue();
    }
    option = asDefaultString(value.get("option"));
    if (isNotBlank(option)) {
      return new BsonRegularExpression(pattern);
    } else {
      return new BsonRegularExpression(pattern, option);
    }
  }

}
