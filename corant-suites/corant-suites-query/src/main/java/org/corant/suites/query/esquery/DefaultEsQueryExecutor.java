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

import static org.corant.shared.util.CollectionUtils.asSet;
import static org.corant.shared.util.CollectionUtils.isEmpty;
import static org.corant.shared.util.MapUtils.getMapString;
import static org.corant.shared.util.MapUtils.isEmpty;
import static org.corant.shared.util.ObjectUtils.shouldNotNull;
import static org.corant.shared.util.StringUtils.ifBlank;
import static org.corant.shared.util.StringUtils.split;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.corant.suites.query.QueryUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * corant-suites-query
 *
 * @author bingo 下午8:29:48
 *
 */
public class DefaultEsQueryExecutor implements EsQueryExecutor {

  public static final String HIT_RS_ETR_PATH_NME = "extract-path";
  public static final String DFLT_RS_ETR_PATH = "hits.hits._source";

  final TransportClient transportClient;

  /**
   * @param transportClient
   */
  public DefaultEsQueryExecutor(TransportClient transportClient) {
    super();
    this.transportClient = shouldNotNull(transportClient, "The transport client can not null!");
  }

  @Override
  public <T> T get(String indexName, String script, Class<T> resultClass, Map<String, String> hints)
      throws Exception {
    QueryBuilder qb = QueryBuilders.wrapperQuery(script);
    SearchResponse sr = transportClient.prepareSearch(indexName).setQuery(qb).setSize(1).get();
    if (sr != null) {
      List<?> list = extractResult(sr, hints, true);
      if (!isEmpty(list)) {
        return getObjMapper().convertValue(list.get(0), resultClass);
      }
    }
    return null;
  }

  @Override
  public Map<String, Object> get(String indexName, String script, Map<String, String> hints)
      throws Exception {
    QueryBuilder qb = QueryBuilders.wrapperQuery(script);
    SearchResponse sr = transportClient.prepareSearch(indexName).setQuery(qb).setSize(1).get();
    if (sr != null) {
      List<?> list = extractResult(sr, hints, true);
      if (!isEmpty(list)) {
        return getObjMapper().convertValue(list.get(0),
            new TypeReference<Map<String, Object>>() {});
      }
    }
    return null;
  }

  @Override
  public <T> List<T> select(String indexName, String script, Class<T> resultClass,
      Map<String, String> hints) throws Exception {
    QueryBuilder qb = QueryBuilders.wrapperQuery(script);
    SearchResponse sr = transportClient.prepareSearch(indexName).setQuery(qb).setSize(128).get();
    if (sr != null) {
      List<?> list = extractResult(sr, hints, true);
      return getObjMapper().convertValue(list, new TypeReference<List<T>>() {});
    }
    return new ArrayList<>();
  }

  @Override
  public List<Map<String, Object>> select(String indexName, String script,
      Map<String, String> hints) throws Exception {
    QueryBuilder qb = QueryBuilders.wrapperQuery(script);
    SearchResponse sr = transportClient.prepareSearch(indexName).setQuery(qb).setSize(128).get();
    if (sr != null) {
      List<?> list = extractResult(sr, hints, true);
      return getObjMapper().convertValue(list, new TypeReference<List<Map<String, Object>>>() {});
    }
    return new ArrayList<>();
  }

  @Override
  public <T> Stream<T> stream(String indexName, String script, Map<String, String> hints) {
    return Stream.empty();
  }

  protected List<?> extractResult(SearchResponse sr, Map<String, String> hints, boolean flat)
      throws IOException {
    List<Object> list = new ArrayList<>();
    if (sr != null) {
      String extractPath = ifBlank(getMapString(hints, HIT_RS_ETR_PATH_NME), DFLT_RS_ETR_PATH);
      XContentBuilder xbuilder = new XContentBuilder(XContentType.JSON.xContent(),
          new ByteArrayOutputStream(), asSet(extractPath));
      XContentBuilder builder = sr.toXContent(xbuilder, ToXContent.EMPTY_PARAMS);
      BytesReference bytes = BytesReference.bytes(builder);
      Map<String, Object> result =
          XContentHelper.convertToMap(bytes, false, XContentType.JSON).v2();
      // now we have structured result map;
      if (!isEmpty(result)) {
        QueryUtils.extractResult(result, split(extractPath, ".", true, false), flat, list);
      }
    }
    return list;
  }

  protected ObjectMapper getObjMapper() {
    return DefaultEsNamedQueryTpl.OM;
  }
}
