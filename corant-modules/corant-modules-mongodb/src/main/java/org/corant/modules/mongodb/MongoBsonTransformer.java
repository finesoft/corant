/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.mongodb;

import org.bson.BsonBinarySubType;
import org.bson.Document;
import org.bson.Transformer;
import org.bson.types.Binary;
import org.bson.types.Decimal128;
import com.mongodb.DBRef;

/**
 * corant-modules-mongodb
 *
 * @author bingo 18:17:38
 */
public class MongoBsonTransformer implements Transformer {

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    return o != null && getClass() == o.getClass();
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public Object transform(final Object value) {
    if (value instanceof Document document) {
      // the built-in document DBRef conversion
      if (document.containsKey("$id") && document.containsKey("$ref")) {
        return new DBRef((String) document.get("$db"), (String) document.get("$ref"),
            document.get("$id"));
      }
    } else if (value instanceof Binary binary
        && binary.getType() == BsonBinarySubType.BINARY.getValue()) {
      // use to convert all binary to bytes[]
      return binary.getData();
    } else if (value instanceof Decimal128 d) {
      // use to convert all decimal 128 to big decimal
      return d.bigDecimalValue();
    }
    return value;
  }

}
