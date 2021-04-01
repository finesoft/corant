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

import java.util.EnumMap;
import java.util.Map;
import org.corant.modules.query.mongodb.MgNamedQuerier.MgOperator;
import org.corant.modules.query.shared.NamedQuerier;
import org.corant.modules.query.shared.dynamic.DynamicQuerier;

/**
 * corant-modules-query-mongodb
 *
 * @author bingo 下午10:44:40
 *
 */
public interface MgNamedQuerier
    extends DynamicQuerier<Map<String, Object>, EnumMap<MgOperator, Object>>, NamedQuerier {

  String getCollectionName(); // from 2020-03-18

  String getOriginalScript();

  enum MgOperator {
    AGGREGATE("aggregate"), FILTER("filter"), PROJECTION("projection"), MIN("min"), MAX(
        "max"), HINT("hint"), SORT("sort");

    private String ops;

    MgOperator(String ops) {
      this.ops = ops;
    }

    public String getOps() {
      return ops;
    }
  }
}
