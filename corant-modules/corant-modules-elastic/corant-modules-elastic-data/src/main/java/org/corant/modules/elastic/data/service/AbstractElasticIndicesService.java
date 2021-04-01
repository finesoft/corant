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
package org.corant.modules.elastic.data.service;

import java.util.HashMap;
import java.util.Map;
import org.corant.modules.elastic.data.Elastic6Constants;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import com.carrotsearch.hppc.cursors.ObjectObjectCursor;

/**
 * corant-modules-elastic-data
 *
 * @author bingo 下午6:36:42
 *
 */
public abstract class AbstractElasticIndicesService implements ElasticIndicesService {

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

  @Override
  public Map<String, Settings> getSetting(String... indexName) {
    GetSettingsResponse response =
        getTransportClient().admin().indices().prepareGetSettings(indexName).get();
    Map<String, Settings> setting = new HashMap<>();
    for (ObjectObjectCursor<String, Settings> cursor : response.getIndexToSettings()) {
      String index = cursor.key;
      setting.put(index, cursor.value);
    }
    return setting;
  }

  public abstract TransportClient getTransportClient();

  @Override
  public boolean isExist(String... indexName) {
    return getTransportClient().admin().indices().exists(new IndicesExistsRequest(indexName))
        .actionGet().isExists();
  }

  @Override
  public RefreshResponse refersh(String... indexName) {
    return getTransportClient().admin().indices().prepareRefresh(indexName).get();
  }
}
