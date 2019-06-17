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
import java.util.List;
import java.util.Map;
import org.bson.conversions.Bson;
import org.corant.suites.query.mongodb.MgInLineNamedQueryResolver.MgOperator;
import org.corant.suites.query.mongodb.MgInLineNamedQueryResolver.Querier;
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
public class DefaultMgNamedQuerier
    implements Querier<EnumMap<MgOperator, Bson>, FetchQuery, QueryHint> {

  public static ObjectMapper OM = new ObjectMapper();

  protected final EnumMap<MgOperator, Bson> script = new EnumMap<>(MgOperator.class);
  protected final Class<?> resultClass;
  protected final List<FetchQuery> fetchQueries;
  protected final List<QueryHint> hints = new ArrayList<>();

  /**
   * @param script
   * @param resultClass
   * @param hints
   * @param fetchQueries
   */
  public DefaultMgNamedQuerier(Map<?, ?> mgQuery, Class<?> resultClass, List<QueryHint> hints,
      List<FetchQuery> fetchQueries) {
    super();
    init(mgQuery);
    this.resultClass = resultClass;
    this.fetchQueries = fetchQueries;
    if (hints != null) {
      this.hints.addAll(hints);
    }
  }

  @Override
  public List<FetchQuery> getFetchQueries() {
    return fetchQueries;
  }

  /**
   * @return the resultClass
   */
  @SuppressWarnings("unchecked")
  @Override
  public <T> Class<T> getResultClass() {
    return (Class<T>) resultClass;
  }

  /**
   *
   * @return the script
   */
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
