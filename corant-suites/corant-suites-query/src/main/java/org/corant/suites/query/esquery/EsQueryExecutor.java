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
package org.corant.suites.query.esquery;

import static org.corant.shared.util.ObjectUtils.forceCast;
import static org.corant.shared.util.StringUtils.split;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.corant.shared.util.ObjectUtils.Pair;
import org.corant.suites.query.QueryUtils;
import org.elasticsearch.action.search.SearchResponse;

/**
 * corant-suites-query
 *
 * @author bingo 下午8:18:15
 *
 */
public interface EsQueryExecutor {

  String HIT_RS_ETR_PATH = "hits.hits._source";
  String[] HIT_RS_ETR_PATHS = split(HIT_RS_ETR_PATH, ".");
  String AGG_RS_ETR_PATH = "aggregations";
  String SUG_RS_ERT_PATH = "suggest";

  SearchResponse execute(String indexName, String script, Map<String, String> queryhints)
      throws Exception;

  default Map<String, Object> search(String indexName, String script,
      Map<String, String> queryhints) throws Exception {
    SearchResponse searchResponse = execute(indexName, script, queryhints);
    if (searchResponse != null) {
      Map<String, Object> result = QueryUtils.of(searchResponse);
      return result;
    } else {
      return new HashMap<>();
    }
  }

  default Map<String, Object> searchAggregation(String indexName, String script,
      Map<String, String> queryhints) throws Exception {
    SearchResponse searchResponse = execute(indexName, script, queryhints);
    if (searchResponse != null) {
      Map<String, Object> result = QueryUtils.of(searchResponse, AGG_RS_ETR_PATH);
      Object aggregation = result.get(AGG_RS_ETR_PATH);
      if (aggregation != null) {
        return forceCast(aggregation);
      }
    }
    return new HashMap<>();
  }

  default Pair<Long, List<Map<String, Object>>> searchHits(String indexName, String script,
      Map<String, String> queryhints) throws Exception {
    List<Map<String, Object>> list = new ArrayList<>();
    SearchResponse searchResponse = execute(indexName, script, queryhints);
    long total = 0;
    if (searchResponse != null) {
      total = searchResponse.getHits() != null ? searchResponse.getHits().getTotalHits() : 0;
      if (total > 0) {
        Map<String, Object> result = QueryUtils.of(searchResponse, HIT_RS_ETR_PATH);
        List<Object> extracted = new ArrayList<>();
        QueryUtils.extractResult(result, HIT_RS_ETR_PATHS, false, extracted);
        extracted.forEach(obj -> list.add(forceCast(obj)));
      }
    }
    return Pair.of(total, list);
  }

}
