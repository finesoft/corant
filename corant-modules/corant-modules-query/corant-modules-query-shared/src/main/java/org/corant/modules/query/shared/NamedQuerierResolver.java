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
package org.corant.modules.query.shared;

import java.util.Set;
import org.corant.modules.query.FetchQueryHandler;
import org.corant.modules.query.QueryHandler;
import org.corant.modules.query.shared.QueryMappingService.QueryMappingClient;
import org.corant.modules.query.shared.dynamic.DynamicQuerier;

/**
 * corant-modules-query-shared
 *
 * TODO Unfinished yet
 *
 * @author bingo 下午12:07:37
 */
public interface NamedQuerierResolver<K, P, Q extends DynamicQuerier<?, ?>>
    extends QueryMappingClient {

  FetchQueryHandler getFetchQueryHandler();

  @Override
  default Set<String> getMappingFilePaths() {
    return null;
  }

  QueryMappingService getMappingService();

  QueryHandler getQueryHandler();

  Q resolve(K key, P param);

}
