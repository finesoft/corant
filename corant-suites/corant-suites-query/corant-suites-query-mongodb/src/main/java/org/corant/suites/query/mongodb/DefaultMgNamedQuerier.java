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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.conversions.Bson;
import org.corant.suites.query.mongodb.MgInLineNamedQueryResolver.MgOperator;
import org.corant.suites.query.mongodb.MgInLineNamedQueryResolver.MgQuerier;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.shared.mapping.FetchQuery;
import org.corant.suites.query.shared.mapping.QueryHint;
import com.fasterxml.jackson.core.JsonpCharacterEscapes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;

/**
 * corant-suites-query
 *
 * @author bingo 下午4:35:55
 *
 */
public class DefaultMgNamedQuerier implements MgQuerier {

  public static final ObjectMapper OM = new ObjectMapper();

  protected final String name;
  protected final EnumMap<MgOperator, Bson> script = new EnumMap<>(MgOperator.class);
  protected final Class<?> resultClass;
  protected final List<FetchQuery> fetchQueries;
  protected final List<QueryHint> hints = new ArrayList<>();
  protected final Map<String, String> properties = new HashMap<>();
  protected final String originalScript;

  /**
   * @param name
   * @param script
   * @param originalScript
   * @param resultClass
   * @param hints
   * @param fetchQueries
   * @param properties
   */
  public DefaultMgNamedQuerier(String name, Map<?, ?> mgQuery, String originalScript,
      Class<?> resultClass, List<QueryHint> hints, List<FetchQuery> fetchQueries,
      Map<String, String> properties) {
    super();

    this.name = name;
    this.resultClass = resultClass;
    this.fetchQueries = fetchQueries;
    if (hints != null) {
      this.hints.addAll(hints);
    }
    if (properties != null) {
      this.properties.putAll(properties);
    }
    init(mgQuery);
    this.originalScript = originalScript;
  }

  @Override
  public List<FetchQuery> getFetchQueries() {
    return fetchQueries;
  }

  @Override
  public List<QueryHint> getHints() {
    return hints;
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
  public Map<String, String> getProperties() {
    return properties;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Class<T> getResultClass() {
    return (Class<T>) resultClass;
  }

  @Override
  public EnumMap<MgOperator, Bson> getScript() {
    return script;
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
