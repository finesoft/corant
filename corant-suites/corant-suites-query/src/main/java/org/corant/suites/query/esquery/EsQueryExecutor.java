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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.corant.shared.util.ObjectUtils.Pair;
import org.corant.suites.query.QueryRuntimeException;
import org.corant.suites.query.QueryUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.search.builder.SearchSourceBuilder;

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

  default SearchRequest buildSearchRequest(String script, String... indexNames) {
    try (XContentParser parser = XContentUtils.createParser(JsonXContent.jsonXContent, script)) {
      return new SearchRequest(indexNames).source(SearchSourceBuilder.fromXContent(parser));
    } catch (IOException e) {
      throw new QueryRuntimeException(e);
    }
  }

  SearchResponse execute(SearchRequest searchRequest) throws Exception;

  default SearchResponse execute(String indexName, String script) throws Exception {
    return execute(buildSearchRequest(script, indexName));
  }

  default Map<String, Object> search(String indexName, String script) throws Exception {
    SearchResponse searchResponse = execute(indexName, script);
    if (searchResponse != null) {
      Map<String, Object> result = XContentUtils.searchResponseToMap(searchResponse);
      return result;
    } else {
      return new HashMap<>();
    }
  }

  default Map<String, Object> searchAggregation(String indexName, String script) throws Exception {
    SearchResponse searchResponse = execute(indexName, script);
    if (searchResponse != null) {
      Map<String, Object> result =
          XContentUtils.searchResponseToMap(searchResponse, AGG_RS_ETR_PATH);
      Object aggregation = result.get(AGG_RS_ETR_PATH);
      if (aggregation != null) {
        return forceCast(aggregation);
      }
    }
    return new HashMap<>();
  }

  default Pair<Long, List<Map<String, Object>>> searchHits(String indexName, String script)
      throws Exception {
    List<Map<String, Object>> list = new ArrayList<>();
    SearchResponse searchResponse = execute(indexName, script);
    long total = 0;
    if (searchResponse != null) {
      total = searchResponse.getHits() != null ? searchResponse.getHits().getTotalHits() : 0;
      if (total > 0) {
        Map<String, Object> result =
            XContentUtils.searchResponseToMap(searchResponse, HIT_RS_ETR_PATH);
        List<Object> extracted = new ArrayList<>();
        QueryUtils.extractResult(result, HIT_RS_ETR_PATHS, false, extracted);
        extracted.forEach(obj -> list.add(forceCast(obj)));
      }
    }
    return Pair.of(total, list);
  }

}
