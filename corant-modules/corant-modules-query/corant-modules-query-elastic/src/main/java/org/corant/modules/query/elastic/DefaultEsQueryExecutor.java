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

import static org.corant.shared.util.Maps.getMapDuration;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.corant.shared.util.Functions;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.unit.TimeValue;

/**
 * corant-modules-query-elastic
 *
 * @author bingo 下午7:58:37
 *
 */
public class DefaultEsQueryExecutor implements EsQueryExecutor {

  final TransportClient transportClient;

  public DefaultEsQueryExecutor(TransportClient transportClient) {
    this.transportClient = transportClient;
  }

  @Override
  public SearchResponse execute(SearchRequest searchRequest, Map<String, String> properties)
      throws Exception {
    Duration actGetTimeOut = getMapDuration(properties, EsQueryExecutor.PRO_KEY_ACT_GET_TIMEOUT);
    if (actGetTimeOut != null) {
      return transportClient.search(searchRequest).actionGet(actGetTimeOut.toMillis());
    } else {
      return transportClient.search(searchRequest).get();
    }
  }

  @Override
  public Stream<Map<String, Object>> scrolledSearch(String indexName, String script,
      TimeValue scrollKeepAlive, int batchSize) throws Exception {
    return StreamSupport.stream(new EsScrollableSpliterator(transportClient, indexName, script,
        scrollKeepAlive, batchSize, Functions.emptyConsumer()), false);
  }

}
