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

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import org.corant.suites.query.QueryRuntimeException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.DeprecationHandler;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.search.SearchModule;
import org.elasticsearch.search.builder.SearchSourceBuilder;

/**
 * corant-suites-query
 *
 * @author bingo 下午7:58:37
 *
 */
public class DefaultSimpleEsQueryExecutor implements SimpleEsQueryExecutor {

  final TransportClient transportClient;

  public DefaultSimpleEsQueryExecutor(TransportClient transportClient) {
    super();
    this.transportClient = transportClient;
  }

  @Override
  public EsResult search(String indexName, String script, Map<String, String> hints)
      throws Exception {
    SearchResponse sr = transportClient.search(buildSearchRequest(script, indexName)).get();
    return new EsResult(sr, hints);
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

}
