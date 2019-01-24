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
import static org.corant.shared.util.StringUtils.defaultBlank;
import static org.corant.shared.util.StringUtils.split;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.corant.suites.query.QueryRuntimeException;
import org.corant.suites.query.QueryUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.DeprecationHandler;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.search.SearchModule;
import org.elasticsearch.search.builder.SearchSourceBuilder;
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
  public static final String HIT_RS_ETR_PATH = "hits.hits._source";
  public static final String AGG_RS_ETR_PATH = "aggregations";
  public static final String SUG_RS_ERT_PATH = "suggest";
  public static final String COU_RS_ERT_PATH = "hits.total";

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
    SearchResponse sr = transportClient.search(buildSearchRequest(script, indexName)).get();
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
    SearchResponse sr = transportClient.search(buildSearchRequest(script, indexName)).get();
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
    SearchResponse sr = transportClient.search(buildSearchRequest(script, indexName)).get();
    if (sr != null) {
      List<?> list = extractResult(sr, hints, true);
      return getObjMapper().convertValue(list, new TypeReference<List<T>>() {});
    }
    return new ArrayList<>();
  }

  @Override
  public List<Map<String, Object>> select(String indexName, String script,
      Map<String, String> hints) throws Exception {
    SearchResponse sr = transportClient.search(buildSearchRequest(script, indexName)).get();
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

  protected SearchRequest buildSearchRequest(String script, String... indexNames) {
    try {
      return new SearchRequest(indexNames).source(SearchSourceBuilder
          .fromXContent(JsonXContent.jsonXContent.createParser(new NamedXContentRegistry(
              new SearchModule(Settings.EMPTY, false, Collections.emptyList()).getNamedXContents()),
              DeprecationHandler.THROW_UNSUPPORTED_OPERATION, script)));
    } catch (IOException e) {
      throw new QueryRuntimeException(e);
    }
  }

  protected List<?> extractResult(SearchResponse sr, Map<String, String> hints, boolean flat)
      throws IOException {
    List<Object> list = new ArrayList<>();
    if (sr != null) {
      String extractPath = defaultBlank(getMapString(hints, HIT_RS_ETR_PATH_NME), HIT_RS_ETR_PATH);
      Map<String, Object> result = XContentHelper.convertToMap(
          BytesReference.bytes(sr.toXContent(new XContentBuilder(XContentType.JSON.xContent(),
              new ByteArrayOutputStream(), asSet(extractPath)), ToXContent.EMPTY_PARAMS)),
          false, XContentType.JSON).v2();
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
