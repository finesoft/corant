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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.modules.query.FetchQueryHandler;
import org.corant.modules.query.QueryHandler;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.shared.QueryMappingService.BeforeQueryMappingInitializeHandler;
import org.corant.modules.query.shared.dynamic.DynamicQuerier;
import net.jcip.annotations.GuardedBy;

/**
 * corant-modules-query-shared
 *
 * @author bingo 下午4:44:33
 *
 */
@ApplicationScoped
public abstract class AbstractNamedQuerierResolver<Q extends DynamicQuerier<?, ?>>
    implements NamedQuerierResolver<String, Object, Q>, BeforeQueryMappingInitializeHandler {

  @Inject
  protected QueryMappingService mappingService;

  @Inject
  protected QueryHandler queryHandler;

  @Inject
  protected FetchQueryHandler fetchQueryHandler;

  @Override
  public FetchQueryHandler getFetchQueryHandler() {
    return fetchQueryHandler;
  }

  @Override
  public QueryMappingService getMappingService() {
    return mappingService;
  }

  @Override
  public QueryHandler getQueryHandler() {
    return queryHandler;
  }

  @GuardedBy("QueryMappingService.rwl.readLock")
  protected Query resolveQuery(String name) {
    Query query = getMappingService().getQuery(name);
    if (query == null) {
      throw new QueryRuntimeException("Can not find name query for name [%s]", name);
    }
    return query;
  }

}
