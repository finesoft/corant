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

import static org.corant.shared.util.CollectionUtils.asList;
import static org.corant.shared.util.StringUtils.split;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.corant.suites.query.QueryUtils;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;

/**
 * corant-suites-query
 *
 * @author bingo 下午8:18:15
 *
 */
public interface SimpleEsQueryExecutor {

  String RS_ETR_PATH_NME = "extract-path";
  String RS_ETR_PATH_FLA = "extract-flat";
  String HIT_RS_ETR_PATH = "hits.hits._source";
  String AGG_RS_ETR_PATH = "aggregations";
  String SUG_RS_ERT_PATH = "suggest";

  EsResult search(String indexName, String script, Map<String, String> hints) throws Exception;

  public static class EsResult {
    long total;
    List<Object> hits = new ArrayList<>();
    List<Object> aggregations = new ArrayList<>();
    List<Object> suggest = new ArrayList<>();
    String scrollId;

    EsResult(SearchResponse sr, Map<String, String> hints)
        throws ElasticsearchParseException, IOException {
      total = sr.getHits() != null ? sr.getHits().totalHits : 0L;
      scrollId = sr.getScrollId();
      String[] extractPaths = split(hints.get(RS_ETR_PATH_NME), ";");
      Set<String> useExtrPaths = new LinkedHashSet<>();
      if (extractPaths.length == 0) {
        useExtrPaths.add(HIT_RS_ETR_PATH);
      } else {
        useExtrPaths.addAll(asList(extractPaths));
      }
      Map<String, Object> result = XContentHelper.convertToMap(
          BytesReference.bytes(sr.toXContent(new XContentBuilder(XContentType.JSON.xContent(),
              new ByteArrayOutputStream(), useExtrPaths), ToXContent.EMPTY_PARAMS)),
          false, XContentType.JSON).v2();
      doExtract(result, useExtrPaths);
    }

    void doExtract(Map<String, Object> result, Set<String> extractPaths) {
      for (String path : extractPaths) {
        if (path.startsWith(HIT_RS_ETR_PATH)) {
          QueryUtils.extractResult(result, path, false, hits);
        } else if (path.startsWith(AGG_RS_ETR_PATH)) {
          QueryUtils.extractResult(result, path, false, aggregations);
        } else if (path.startsWith(SUG_RS_ERT_PATH)) {
          QueryUtils.extractResult(result, path, false, suggest);
        }
      }
    }

    List<Object> getAggregations() {
      return aggregations;
    }

    List<Object> getHits() {
      return hits;
    }

    String getScrollId() {
      return scrollId;
    }

    List<Object> getSuggest() {
      return suggest;
    }

    long getTotal() {
      return total;
    }

  }

}
