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
package org.corant.suites.elastic;

import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.suites.elastic.metadata.ElasticMapping;
import org.corant.suites.elastic.metadata.resolver.ElasticMappingResolver;

/**
 * corant-suites-elastic
 *
 * @author bingo 下午6:36:42
 *
 */
@ApplicationScoped
public abstract class DefaultElasticIndicesService implements ElasticIndicesService {

  @Inject
  ElasticExtension extension;

  @Inject
  ElasticTransportClientService transportClientService;

  @Override
  public boolean checkIndicesExist(String... indexName) {
    return false;
  }

  @Override
  public boolean createIndex(String indexName, Map<String, Object> setting,
      Map<String, Map<String, Object>> schemas) {
    return false;
  }

  @Override
  public boolean deleteIndex(String indexName) {
    return false;
  }

  @Override
  public ElasticMapping getMapping(Class<?> cls) {
    return null;
  }

  @Override
  public ElasticMappingResolver getMappingResolver() {
    return null;
  }

}
