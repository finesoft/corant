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

import static org.corant.shared.util.StringUtils.isBlank;
import static org.corant.shared.util.StringUtils.split;
import java.util.Map;
import org.bson.types.ObjectId;
import org.corant.shared.conversion.ConversionException;
import org.corant.shared.conversion.ConverterHints;
import org.corant.shared.conversion.converter.AbstractConverter;
import com.mongodb.DBRef;

/**
 * corant-suites-query-mongodb
 *
 * @author bingo 下午3:56:35
 *
 */
public class StringDBRefConverter extends AbstractConverter<String, DBRef> {

  @Override
  protected DBRef convert(String value, Map<String, ?> hints) throws Exception {
    if (isBlank(value)) {
      return null;
    }
    String[] array = split(value,
        ConverterHints.getHint(hints, StringDBPointerConverter.SEPARATOR_KEY, ":"), true, true);
    if (array.length < 2) {
      throw new ConversionException(
          "Can't convert string %s to BsonDbPointer, the source string must represent like 'database:collection:id'",
          value);
    }
    if (array.length == 2) {
      return new DBRef(array[0], new ObjectId(array[1]));
    } else {
      return new DBRef(array[0], array[1], new ObjectId(array[1]));
    }
  }

}
