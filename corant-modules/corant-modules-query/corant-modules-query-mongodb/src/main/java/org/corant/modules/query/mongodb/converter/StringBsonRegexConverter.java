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
package org.corant.modules.query.mongodb.converter;

import static org.corant.shared.util.Empties.isEmpty;
import java.util.Map;
import org.bson.BsonRegularExpression;
import org.corant.shared.conversion.ConverterHints;
import org.corant.shared.conversion.converter.AbstractConverter;
import org.corant.shared.util.PathMatcher.RegexMatcher;

/**
 * corant-modules-query-mongodb
 *
 * @author bingo 上午10:04:31
 *
 */
public class StringBsonRegexConverter extends AbstractConverter<String, BsonRegularExpression> {

  public static final String REGEX_KEY = "regex.option";

  /**
   *
   */
  public StringBsonRegexConverter() {}

  /**
   * @param throwException
   */
  public StringBsonRegexConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @param defaultValue
   */
  public StringBsonRegexConverter(BsonRegularExpression defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param throwException
   */
  public StringBsonRegexConverter(BsonRegularExpression defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  protected BsonRegularExpression convert(String value, Map<String, ?> hints) throws Exception {
    if (isEmpty(value)) {
      return getDefaultValue();
    }
    String useValue = value;
    Object pattern = hints.get("like");
    if (pattern != null) {
      if (RegexMatcher.hasRegexChar(useValue)) {
        int len = useValue.length();
        StringBuilder sb = new StringBuilder(len << 1);
        for (int i = 0; i < len; i++) {
          char c = useValue.charAt(i);
          if (RegexMatcher.isRegexChar(c)) {
            sb.append("\\").append(c);
          } else {
            sb.append(c);
          }
        }
        useValue = sb.toString();
      }
      if ("%*%".equals(pattern)) {
        useValue = ".*" + useValue + ".*";// include
      } else if ("*%".equals(pattern)) {
        useValue = "^" + useValue + ".*"; // start with
      } else if ("%*".equals(pattern)) {
        useValue = ".*" + useValue + "$"; // end with
      } else if ("~%*%".equals(pattern)) {
        useValue = "^((?!" + value + ").)*$"; // exclude
      }
    }
    return new BsonRegularExpression(useValue, ConverterHints.getHint(hints, REGEX_KEY, "i"));
  }
}
