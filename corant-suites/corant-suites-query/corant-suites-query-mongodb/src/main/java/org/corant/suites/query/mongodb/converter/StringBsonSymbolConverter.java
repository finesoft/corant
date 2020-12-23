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

import static org.corant.shared.util.Strings.isBlank;
import java.util.Map;
import org.bson.BsonSymbol;
import org.corant.shared.conversion.converter.AbstractConverter;

/**
 * corant-suites-query-mongodb
 *
 * @author bingo 下午4:02:44
 *
 */
public class StringBsonSymbolConverter extends AbstractConverter<String, BsonSymbol> {

  @Override
  protected BsonSymbol convert(String value, Map<String, ?> hints) throws Exception {
    if (isBlank(value)) {
      return null;
    }
    return new BsonSymbol(value);
  }

}
