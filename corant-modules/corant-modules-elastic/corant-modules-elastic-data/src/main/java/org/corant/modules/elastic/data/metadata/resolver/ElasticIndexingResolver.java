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
package org.corant.modules.elastic.data.metadata.resolver;

import java.util.Map;
import org.corant.modules.elastic.data.metadata.ElasticIndexing;
import org.corant.modules.elastic.data.metadata.ElasticMapping;

/**
 * corant-modules-elastic-data
 *
 * <p>
 * This interface is used to parse classes that have some annotations and generate various metadata
 * objects related to the index, which will be used for subsequent data indexing.
 * </p>
 *
 * @author bingo 下午2:55:11
 */
public interface ElasticIndexingResolver {

  /**
   * Resolve document class elastic indexing metadata object. the document class must have some
   * metadata annotations.
   *
   * @param documentClass
   * @return the document elastic indexing metadata
   */
  ElasticIndexing getIndexing(Class<?> documentClass);

  /**
   * Get the all resolved elastic indexing metadata map.
   *
   * @return all resolved elastic indexing metadata, the key is index name, the value is the
   *         metadata object
   */
  Map<String, ElasticIndexing> getIndexings();

  /**
   * Get the document class elastic mapping metadata object. the document class must have some
   * metadata annotations.
   *
   * @param documentClass
   * @return the document elastic mapping metadata
   */
  ElasticMapping getMapping(Class<?> documentClass);

  /**
   * Get the all resolved elastic indexing metadata map.
   *
   * @return all resolved elastic indexing metadata, the key is document class, the value is the
   *         metadata object
   */
  Map<Class<?>, ElasticIndexing> getMappings();
}
