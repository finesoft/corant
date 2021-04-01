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

import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.split;
import java.util.Map;
import org.bson.BsonDbPointer;
import org.bson.types.ObjectId;
import org.corant.shared.conversion.ConversionException;
import org.corant.shared.conversion.ConverterHints;
import org.corant.shared.conversion.converter.AbstractConverter;
import org.corant.shared.normal.Names;

/**
 * corant-modules-query-mongodb
 *
 * @author bingo 下午3:47:12
 *
 */
public class StringBsonDbPointerConverter extends AbstractConverter<String, BsonDbPointer> {

  public static final String SEPARATOR_KEY = "namespace.separator";

  @Override
  protected BsonDbPointer convert(String value, Map<String, ?> hints) throws Exception {
    if (isBlank(value)) {
      return null;
    }
    String[] array = split(value,
        ConverterHints.getHint(hints, SEPARATOR_KEY, Names.DOMAIN_SPACE_SEPARATORS), true, true);
    if (array.length != 2) {
      throw new ConversionException(
          "Can't convert string %s to BsonDbPointer, the source string must be represented like 'namespace:id'",
          value);
    }
    return new BsonDbPointer(array[0], new ObjectId(array[1]));
  }

}
