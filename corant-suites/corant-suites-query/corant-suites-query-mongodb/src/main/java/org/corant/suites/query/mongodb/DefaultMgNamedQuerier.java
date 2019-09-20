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

import java.util.EnumMap;
import java.util.Map;
import org.bson.conversions.Bson;
import org.corant.suites.query.mongodb.MgInLineNamedQueryResolver.MgOperator;
import org.corant.suites.query.mongodb.MgInLineNamedQueryResolver.MgQuerier;
import org.corant.suites.query.shared.QueryParameter;
import org.corant.suites.query.shared.QueryParameterResolver;
import org.corant.suites.query.shared.QueryResultResolver;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.shared.dynamic.AbstractDynamicQuerier;
import org.corant.suites.query.shared.mapping.Query;
import com.fasterxml.jackson.core.JsonpCharacterEscapes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;

/**
 * corant-suites-query
 *
 * @author bingo 下午4:35:55
 *
 */
public class DefaultMgNamedQuerier extends
    AbstractDynamicQuerier<Map<String, Object>, EnumMap<MgOperator, Bson>> implements MgQuerier {

  public static final ObjectMapper OM = new ObjectMapper();

  protected final String name;
  protected final EnumMap<MgOperator, Bson> script = new EnumMap<>(MgOperator.class);
  protected final String originalScript;

  /**
   * @param query
   * @param queryParameter
   * @param parameterResolver
   * @param resultResolver
   * @param mgQuery
   * @param originalScript
   */
  protected DefaultMgNamedQuerier(Query query, QueryParameter queryParameter,
      QueryParameterResolver parameterResolver, QueryResultResolver resultResolver,
      Map<?, ?> mgQuery, String originalScript) {
    super(query, queryParameter, parameterResolver, resultResolver);
    name = query.getName();
    this.originalScript = originalScript;
    init(mgQuery);
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
  public EnumMap<MgOperator, Bson> getScript(Map<?, ?> additionals) {
    return script;
  }

  @Override
  public Map<String, Object> getScriptParameter() {
    return null;
  }

  protected void init(Map<?, ?> mgQuery) {
    for (MgOperator mgo : MgOperator.values()) {
      Object x = mgQuery.get(mgo.getOps());
      if (x != null) {
        try {
          script.put(mgo, BasicDBObject
              .parse(OM.writer(JsonpCharacterEscapes.instance()).writeValueAsString(x)));
        } catch (Exception e) {
          throw new QueryRuntimeException(e);
        }
      } else {
        script.put(mgo, null);
      }
    }
  }
}
