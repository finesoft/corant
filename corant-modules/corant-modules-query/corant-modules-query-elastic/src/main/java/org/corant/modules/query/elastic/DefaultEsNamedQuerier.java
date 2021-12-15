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
package org.corant.modules.query.elastic;

import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Lists.append;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Objects.min;
import static org.corant.shared.util.Strings.asDefaultString;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.corant.modules.query.FetchQueryHandler;
import org.corant.modules.query.QueryHandler;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.shared.dynamic.AbstractDynamicQuerier;

/**
 * corant-modules-query-elastic
 *
 * @author bingo 下午4:35:55
 *
 */
public class DefaultEsNamedQuerier extends
    AbstractDynamicQuerier<Map<String, Object>, Map<Object, Object>> implements EsNamedQuerier {

  protected final Map<Object, Object> script;
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
      QueryHandler queryResolver, FetchQueryHandler fetchQueryResolver,
      Map<Object, Object> scriptMap) {
    super(query, queryParameter, queryResolver, fetchQueryResolver);
    String[] useHintKeys = {EsQueryExecutor.HIT_RS_KEY};
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
  public Map<Object, Object> getScript(Map<?, ?> additionals) {
    return script;
  }

  @Override
  public Map<String, Object> getScriptParameter() {
    return null;
  }

  /**
   * Result window is too large, from + size must be less than or equal to: [10000]
   */
  @Override
  public int resolveMaxSelectSize() {
    return min(super.resolveMaxSelectSize(), getUnLimitSize());
  }

  @Override
  protected int getUnLimitSize() {
    return 10000;
  }

}
