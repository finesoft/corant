package org.corant.modules.query.mongodb;
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

import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Primitives.wrapArray;
import static org.corant.shared.util.Strings.asDefaultString;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import org.corant.modules.mongodb.MongoExtendedJsons;
import org.corant.modules.query.FetchQueryHandler;
import org.corant.modules.query.QueryHandler;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.mongodb.MgNamedQuerier.MgOperator;
import org.corant.modules.query.shared.dynamic.AbstractDynamicQuerier;

/**
 * corant-modules-query-mongodb
 *
 * @author bingo 下午4:35:55
 */
public class DefaultMgNamedQuerier
    extends AbstractDynamicQuerier<Map<String, Object>, EnumMap<MgOperator, Object>>
    implements MgNamedQuerier {

  protected String collectionName;
  protected final EnumMap<MgOperator, Object> script = new EnumMap<>(MgOperator.class);
  protected final String originalScript;
  protected MgOperator rootOperator;

  protected DefaultMgNamedQuerier(Query query, QueryParameter queryParameter,
      QueryHandler queryResolver, FetchQueryHandler fetchQueryResolver, Map<?, ?> mgQuery,
      String originalScript) {
    super(query, queryParameter, queryResolver, fetchQueryResolver);
    this.originalScript = originalScript;
    init(mgQuery);
  }

  @Override
  public String getCollectionName() {
    return collectionName;
  }

  @Override
  public String getOriginalScript() {
    return originalScript;
  }

  @Override
  public MgOperator getRootOperator() {
    return rootOperator;
  }

  @Override
  public EnumMap<MgOperator, Object> getScript(Map<?, ?> additionals) {
    return script;
  }

  @Override
  public Map<String, Object> getScriptParameter() {
    return null;
  }

  /**
   * Resolve the collection name and query script
   *
   *
   * @param mgQuery init
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  protected void init(Map<?, ?> mgQuery) {
    // resolve collection name and query script
    if (isNotEmpty(mgQuery)) {
      Entry<?, ?> entry = mgQuery.entrySet().iterator().next();
      collectionName = asDefaultString(entry.getKey());
      Map queryScript = forceCast(entry.getValue());
      if (isNotEmpty(queryScript)) {
        queryScript.forEach((k, x) -> {
          MgOperator mgo = MgOperator.FILTER;
          for (MgOperator mo : MgOperator.values()) {
            if (mo.getOps().equals(k)) {
              mgo = mo;
              break;
            }
          }
          if (mgo.isIndependent()) {
            if (rootOperator == null) {
              rootOperator = mgo;
            } else {
              throw new QueryRuntimeException("For now we only support one root mongodb operator");
            }
          }
          try {
            // We assume that all extended JSON have been processed in the given script
            // FIXME use another way to convert value to BSON
            if (x instanceof Collection) {
              script.put(mgo, MongoExtendedJsons.toBsons((Collection<?>) x, false));
            } else if (x != null && x.getClass().isArray()) {
              script.put(mgo, MongoExtendedJsons.toBsons(wrapArray(x), false));
            } else {
              script.put(mgo, MongoExtendedJsons.toBson(x, false));
            }
          } catch (Exception e) {
            throw new QueryRuntimeException(e);
          }
        });
      }
    }
    if (rootOperator == null) {
      rootOperator = MgOperator.FILTER;
    }
  }
}
