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
package org.corant.modules.query.mongodb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.bson.Document;
import org.bson.types.Decimal128;

/**
 * corant-modules-query-mongodb
 *
 * @author bingo 下午4:14:22
 *
 */
// FIXME
@SuppressWarnings({"rawtypes", "unchecked"})
public class Decimal128Utils {

  static final int MAX_DEEP = 32;

  /**
   * Very simple and crude conversion Decimal128 to BigDecimal, fix me pls!!!
   *
   * @param doc
   * @return convert
   */
  public static Document convert(Document doc) {
    if (doc == null) {
      return null;
    }
    int dept = 0;
    for (Entry<String, Object> e : doc.entrySet()) {
      if (e.getValue() instanceof Decimal128) {
        e.setValue(((Decimal128) e.getValue()).bigDecimalValue());
      } else if (e.getValue() instanceof Map) {
        e.setValue(convert(new Document((Map<String, Object>) e.getValue()), dept));
      } else if (e.getValue() instanceof Document) {
        e.setValue(convert((Document) e.getValue(), dept));
      } else if (e.getValue() instanceof Collection) {
        e.setValue(convert((Collection) e.getValue(), dept));
      }
    }
    return doc;
  }

  private static Collection convert(Collection collection, int deep) {
    int dp = deep + 1;
    if (collection == null || dp > MAX_DEEP) {
      return collection;
    }
    List list = new ArrayList<>(collection.size());
    for (Object o : collection) {
      if (o instanceof Decimal128) {
        list.add(((Decimal128) o).bigDecimalValue());
      } else if (o instanceof Map) {
        list.add(convert(new Document((Map<String, Object>) o), dp));
      } else if (o instanceof Document) {
        list.add(convert((Document) o, dp));
      } else if (o instanceof Collection) {
        list.add(convert((Collection) o, dp));
      } else {
        list.add(o);
      }
    }
    return list;
  }

  private static Document convert(Document doc, int deep) {
    int dp = deep + 1;
    if (doc == null || dp > MAX_DEEP) {
      return null;
    }
    for (Entry<String, Object> e : doc.entrySet()) {
      if (e.getValue() instanceof Decimal128) {
        e.setValue(((Decimal128) e.getValue()).bigDecimalValue());
      } else if (e.getValue() instanceof Map) {
        e.setValue(convert(new Document((Map<String, Object>) e.getValue()), dp));
      } else if (e.getValue() instanceof Document) {
        e.setValue(convert((Document) e.getValue(), dp));
      } else if (e.getValue() instanceof Collection) {
        e.setValue(convert((Collection) e.getValue(), dp));
      }
    }
    return doc;
  }
}
