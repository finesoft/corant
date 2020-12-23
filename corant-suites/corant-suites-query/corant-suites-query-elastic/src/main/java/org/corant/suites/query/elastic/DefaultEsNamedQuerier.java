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
package org.corant.suites.query.elastic;

import static org.corant.shared.util.Lists.append;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Strings.asDefaultString;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.corant.suites.query.shared.FetchQueryResolver;
import org.corant.suites.query.shared.QueryParameter;
import org.corant.suites.query.shared.QueryResolver;
import org.corant.suites.query.shared.dynamic.AbstractDynamicQuerier;
import org.corant.suites.query.shared.mapping.Query;

/**
 * corant-suites-query
 *
 * @author bingo 下午4:35:55
 *
 */
public class DefaultEsNamedQuerier extends
    AbstractDynamicQuerier<Map<String, Object>, Map<Object, Object>> implements EsNamedQuerier {

  protected final Map<Object, Object> script;
  protected final String name;
  protected final String indexName;
  protected final String[] hintKeys;

  /**
   * @param query
   * @param queryParameter
   * @param queryResolver
   * @param fetchQueryResolver
   * @param scriptMap
   */
  @SuppressWarnings("rawtypes")
  protected DefaultEsNamedQuerier(Query query, QueryParameter queryParameter,
      QueryResolver queryResolver, FetchQueryResolver fetchQueryResolver,
      Map<Object, Object> scriptMap) {
    super(query, queryParameter, queryResolver, fetchQueryResolver);
    name = query.getName();
    String[] useHintKeys = new String[] {EsQueryExecutor.HIT_RS_KEY};
    if (isNotEmpty(scriptMap)) {
      Entry<?, ?> entry = scriptMap.entrySet().iterator().next();
      indexName = asDefaultString(entry.getKey());
      script = forceCast(entry.getValue());
      if (entry.getValue() instanceof Map && ((Map) entry.getValue()).containsKey("highlight")) {
        useHintKeys = append(useHintKeys, EsQueryExecutor.HIT_HL_KEY);
      }
    } else {
      indexName = null;
      script = new HashMap<>();
    }
    hintKeys = useHintKeys;
  }

  @Override
  public String[] getHintKeys() {
    return hintKeys;
  }

  @Override
  public String getIndexName() {
    return indexName;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Map<Object, Object> getScript(Map<?, ?> additionals) {
    return script;
  }

  @Override
  public Map<String, Object> getScriptParameter() {
    return null;
  }

}
