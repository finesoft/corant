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

import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;

/**
 * corant-suites-query
 *
 * @author bingo 下午7:58:37
 *
 */
public class DefaultEsQueryExecutor implements EsQueryExecutor {

  final TransportClient transportClient;

  public DefaultEsQueryExecutor(TransportClient transportClient) {
    super();
    this.transportClient = transportClient;
  }

  @Override
  public SearchResponse execute(SearchRequest searchRequest) throws Exception {
    return transportClient.search(searchRequest).get();
  }

  @Override
  public Stream<Map<String, Object>> stream(String indexName, String script) throws Exception {
    return StreamSupport.stream(new EsScrollableSpliterator(transportClient, indexName, script),
        false);
  }

}
