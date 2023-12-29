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
import org.corant.modules.query.shared.dynamic.DynamicQuerier;

/**
 * corant-modules-query-mongodb
 *
 * @author bingo 下午10:44:40
 */
public interface MgNamedQuerier
    extends DynamicQuerier<Map<String, Object>, EnumMap<MgOperator, Object>> {

  String getCollectionName(); // from 2020-03-18

  String getOriginalScript();

  MgOperator getRootOperator(); // from 2023-10-11

  enum MgOperator {

    AGGREGATE("aggregate", true),

    FILTER("filter", true),

    DISTINCT("distinct", true),

    COUNT("count", true),

    PROJECTION("projection", false),

    MIN("min", false),

    MAX("max", false),

    HINT("hint", false),

    SORT("sort", false),

    FIELD_NAME("fieldName", false),

    FIELD_TYPE("fieldType", false),

    // READ_PREFERENCE("readPreference", false),

    READ_CONCERN("readConcern", false);

    private final String ops;

    private final boolean independent;

    MgOperator(String ops, boolean independent) {
      this.ops = ops;
      this.independent = independent;
    }

    public String getOps() {
      return ops;
    }

    public boolean isIndependent() {
      return independent;
    }

  }
}
