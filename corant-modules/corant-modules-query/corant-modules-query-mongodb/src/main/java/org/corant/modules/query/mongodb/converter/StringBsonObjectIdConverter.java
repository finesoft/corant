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
import org.bson.BsonObjectId;
import org.bson.types.ObjectId;
import org.corant.shared.conversion.converter.AbstractConverter;

/**
 * corant-modules-query-mongodb
 *
 * @author bingo 上午10:04:31
 *
 */
public class StringBsonObjectIdConverter extends AbstractConverter<String, BsonObjectId> {

  /**
   *
   */
  public StringBsonObjectIdConverter() {}

  /**
   * @param throwException
   */
  public StringBsonObjectIdConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @param defaultValue
   */
  public StringBsonObjectIdConverter(BsonObjectId defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param throwException
   */
  public StringBsonObjectIdConverter(BsonObjectId defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  protected BsonObjectId convert(String value, Map<String, ?> hints) throws Exception {
    if (isEmpty(value)) {
      return getDefaultValue();
    }
    return new BsonObjectId(new ObjectId(value));
  }

}
