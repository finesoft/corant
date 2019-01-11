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
import org.corant.suites.elastic.metadata.ElasticMapping;
import org.corant.suites.elastic.metadata.resolver.ElasticMappingResolver;

/**
 * corant-suites-elastic
 *
 * @author bingo 下午4:08:34
 *
 */
public interface ElasticIndicesService {

  boolean checkIndicesExist(String... indexName);

  boolean checkMappingExist(String indexName, String typeName);

  boolean createIndex(String indexName, Map<String, Object> setting,
      Map<String, Map<String, Object>> schemas);

  boolean deleteIndex(String indexName);

  ElasticMapping getMapping(Class<?> cls);

  ElasticMappingResolver getMappingResolver();

}
