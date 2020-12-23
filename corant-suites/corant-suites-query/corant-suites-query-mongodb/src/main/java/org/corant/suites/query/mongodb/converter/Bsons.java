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
package org.corant.suites.query.mongodb.converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bson.conversions.Bson;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonpCharacterEscapes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;

/**
 * corant-suites-query-mongodb
 *
 * @author bingo 下午6:09:18
 *
 */
public class Bsons {

  public static final ObjectMapper OM = new ObjectMapper();// TODO FIXME

  public static Bson toBson(Object x) throws JsonProcessingException {
    return x == null ? null
        : BasicDBObject.parse(OM.writer(JsonpCharacterEscapes.instance()).writeValueAsString(x));
  }

  public static List<Bson> toBsons(Collection<?> x) throws JsonProcessingException {
    if (x == null) {
      return null;
    } else {
      List<Bson> list = new ArrayList<>(x.size());
      for (Object o : x) {
        list.add(toBson(o));
      }
      return list;
    }
  }

  public static List<Bson> toBsons(Object[] x) throws JsonProcessingException {
    if (x == null) {
      return null;
    } else {
      List<Bson> list = new ArrayList<>(x.length);
      for (Object o : x) {
        list.add(toBson(o));
      }
      return list;
    }
  }
}
