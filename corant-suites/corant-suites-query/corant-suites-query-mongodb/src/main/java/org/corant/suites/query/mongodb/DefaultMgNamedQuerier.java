package org.corant.suites.query.mongodb;
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
import static org.corant.shared.util.Strings.asDefaultString;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import org.corant.suites.query.mongodb.MgNamedQuerier.MgOperator;
import org.corant.suites.query.mongodb.converter.Bsons;
import org.corant.suites.query.shared.FetchQueryHandler;
import org.corant.suites.query.shared.QueryParameter;
import org.corant.suites.query.shared.QueryHandler;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.shared.dynamic.AbstractDynamicQuerier;
import org.corant.suites.query.shared.mapping.Query;

/**
 * corant-suites-query
 *
 * @author bingo 下午4:35:55
 *
 */
public class DefaultMgNamedQuerier
    extends AbstractDynamicQuerier<Map<String, Object>, EnumMap<MgOperator, Object>>
    implements MgNamedQuerier {

  protected String collectionName;
  protected final String name;
  protected final EnumMap<MgOperator, Object> script = new EnumMap<>(MgOperator.class);
  protected final String originalScript;

  /**
   * @param query
   * @param queryParameter
   * @param queryResolver
   * @param fetchQueryResolver
   * @param mgQuery
   * @param originalScript
   */
  protected DefaultMgNamedQuerier(Query query, QueryParameter queryParameter,
      QueryHandler queryResolver, FetchQueryHandler fetchQueryResolver, Map<?, ?> mgQuery,
      String originalScript) {
    super(query, queryParameter, queryResolver, fetchQueryResolver);
    name = query.getName();
    this.originalScript = originalScript;
    init(mgQuery);
  }

  @Override
  public String getCollectionName() {
    return collectionName;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getOriginalScript() {
    return originalScript;
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
  @SuppressWarnings("rawtypes")
  protected void init(Map<?, ?> mgQuery) {
    // resolve collection name and query script
    if (isNotEmpty(mgQuery)) {
      Entry<?, ?> entry = mgQuery.entrySet().iterator().next();
      collectionName = asDefaultString(entry.getKey());
      Map queryScript = forceCast(entry.getValue());
      if (isNotEmpty(queryScript)) {
        for (MgOperator mgo : MgOperator.values()) {
          Object x = queryScript.get(mgo.getOps());
          if (x != null) {
            try {
              if (x instanceof Collection) {
                script.put(mgo, Bsons.toBsons((Collection<?>) x));
              } else if (x instanceof Object[]) {
                script.put(mgo, Bsons.toBsons((Object[]) x));
              } else {
                script.put(mgo, Bsons.toBson(x));
              }
            } catch (Exception e) {
              throw new QueryRuntimeException(e);
            }
          }
        }
      }
    }
  }
}
