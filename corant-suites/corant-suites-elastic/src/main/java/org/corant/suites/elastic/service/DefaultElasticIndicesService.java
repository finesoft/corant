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
package org.corant.suites.elastic.service;

import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.suites.elastic.Elastic6Constants;
import org.corant.suites.elastic.metadata.resolver.DefaultElasticIndexingResolver;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.client.transport.TransportClient;

/**
 * corant-suites-elastic
 *
 * @author bingo 下午6:36:42
 *
 */
@ApplicationScoped
public class DefaultElasticIndicesService implements ElasticIndicesService {

  @Inject
  protected DefaultElasticIndexingResolver indexingResolver;

  @Inject
  protected ElasticTransportClientService transportClientService;

  @Override
  public boolean create(String indexName, Map<String, Object> setting, Map<String, Object> schema) {
    if (!isExist(indexName)) {
      return getTransportClient().admin().indices().prepareCreate(indexName)
          .addMapping(Elastic6Constants.TYP_NME, schema).setSettings(setting).get()
          .isAcknowledged();
    } else {
      return getTransportClient().admin().indices().preparePutMapping(indexName)
          .setType(Elastic6Constants.TYP_NME).setSource(schema).get().isAcknowledged();
    }
  }

  @Override
  public boolean delete(String indexName) {
    return getTransportClient().admin().indices().delete(new DeleteIndexRequest(indexName))
        .actionGet().isAcknowledged();
  }

  public TransportClient getTransportClient() {
    return getTransportClientService().getTransportClient();
  }

  /**
   *
   * @return the transportClientService
   */
  public ElasticTransportClientService getTransportClientService() {
    return transportClientService;
  }

  @Override
  public boolean isExist(String... indexName) {
    return getTransportClient().admin().indices().exists(new IndicesExistsRequest(indexName))
        .actionGet().isExists();
  }
}
