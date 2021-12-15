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
import static org.corant.shared.util.Maps.getMapKeyPathValues;
import static org.corant.shared.util.Maps.getOptMapObject;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Strings.split;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.Conversions;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.search.builder.SearchSourceBuilder;

/**
 * corant-modules-query-elastic
 *
 * @author bingo 下午8:18:15
 *
 */
public interface EsQueryExecutor {

  String HIT_HL_KEY = "hits.hits.highlight";
  String HIT_RS_KEY = "hits.hits._source";
  String[] HIT_KEYS = {"hits", "hits"};
  String AGG_RS_ETR_PATH = "aggregations";
  String SUG_RS_ERT_PATH = "suggest";

  String PRO_KEY_ROUTING = ".routing";
  String PRO_KEY_PREFERENCE = ".preference";
  String PRO_KEY_TYPES = ".types";
  String PRO_KEY_SEARCH_TYPE = ".search_type";
  String PRO_KEY_BATCHED_REDUCE_SIZE = ".batched_reduce_size";
  String PRO_KEY_PRE_FILTER_SHARD_SIZE = ".pre_filter_shard_size";
  String PRO_KEY_MAX_CONCURRENT_SHARD_REQS = ".max_concurrent_shard_requests";
  String PRO_KEY_ALLOW_PARTIAL_SEARCH_RESULTS = ".allow_partial_search_results";
  String PRO_KEY_REQ_CACHE = ".request_cache";
  String PRO_KEY_ACT_GET_TIMEOUT = ".action_get_timeout";

  static SearchSourceBuilder buildSearchSourceBuilder(String script) {
    try (XContentParser parser = XContentUtils.createParser(JsonXContent.jsonXContent, script)) {
      return SearchSourceBuilder.fromXContent(parser);
    } catch (IOException e) {
      throw new QueryRuntimeException(e, "Can't build SearchSourceBuilder from %s.", script);
    }
  }

  default SearchRequest buildSearchRequest(String script, Map<String, String> properties,
      String... indexNames) {
    final SearchRequest searchRequest =
        new SearchRequest(indexNames).source(buildSearchSourceBuilder(script));
    if (isNotEmpty(properties)) {
      getOptMapObject(properties, PRO_KEY_ROUTING, Conversions::toString)
          .ifPresent(s -> searchRequest.routing(split(s, ",", true, true)));
      getOptMapObject(properties, PRO_KEY_PREFERENCE, Conversions::toString)
          .ifPresent(searchRequest::preference);
      getOptMapObject(properties, PRO_KEY_TYPES, Conversions::toString)
          .ifPresent(s -> searchRequest.types(split(s, ",", true, true)));
      getOptMapObject(properties, PRO_KEY_SEARCH_TYPE, Conversions::toString)
          .ifPresent(searchRequest::searchType);
      getOptMapObject(properties, PRO_KEY_BATCHED_REDUCE_SIZE, Conversions::toInteger)
          .ifPresent(searchRequest::setBatchedReduceSize);
      getOptMapObject(properties, PRO_KEY_PRE_FILTER_SHARD_SIZE, Conversions::toInteger)
          .ifPresent(searchRequest::setPreFilterShardSize);
      getOptMapObject(properties, PRO_KEY_MAX_CONCURRENT_SHARD_REQS, Conversions::toInteger)
          .ifPresent(searchRequest::setMaxConcurrentShardRequests);
      getOptMapObject(properties, PRO_KEY_ALLOW_PARTIAL_SEARCH_RESULTS, Conversions::toBoolean)
          .ifPresent(searchRequest::allowPartialSearchResults);
      getOptMapObject(properties, PRO_KEY_REQ_CACHE, Conversions::toBoolean)
          .ifPresent(searchRequest::requestCache);
    }
    return searchRequest;
  }

  SearchResponse execute(SearchRequest searchRequest, Map<String, String> properties)
      throws Exception;

  default SearchResponse execute(String indexName, String script, Map<String, String> properties)
      throws Exception {
    return execute(buildSearchRequest(script, properties, indexName), properties);
  }

  Stream<Map<String, Object>> scrolledSearch(String indexName, String script,
      TimeValue scrollKeepAlive, int batchSize) throws Exception;

  default Map<String, Object> search(String indexName, String script,
      Map<String, String> properties) throws Exception {
    SearchResponse searchResponse = execute(indexName, script, properties);
    if (searchResponse != null) {
      return XContentUtils.searchResponseToMap(searchResponse);
    } else {
      return new HashMap<>();
    }
  }

  default Map<String, Object> searchAggregation(String indexName, String script,
      Map<String, String> properties) throws Exception {
    SearchResponse searchResponse = execute(indexName, script, properties);
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

  default Pair<Long, List<Map<String, Object>>> searchHits(String indexName, String script,
      Map<String, String> properties, String... hintKeys) throws Exception {
    List<Map<String, Object>> list = new ArrayList<>();
    SearchResponse searchResponse = execute(indexName, script, properties);
    long total = 0;
    if (searchResponse != null) {
      total = searchResponse.getHits() != null ? searchResponse.getHits().getTotalHits() : 0;
      if (total > 0) {
        Map<String, Object> result = XContentUtils.searchResponseToMap(searchResponse, hintKeys);
        if (hintKeys.length == 1) {
          List<Object> extracted = getMapKeyPathValues(result, split(hintKeys[0], ".", true, true));
          extracted.forEach(obj -> list.add(forceCast(obj)));
        } else {
          List<Object> extracted = getMapKeyPathValues(result, HIT_KEYS);
          extracted.forEach(obj -> list.add(forceCast(obj)));
        }
      }
    }
    return Pair.of(total, list);
  }

}
