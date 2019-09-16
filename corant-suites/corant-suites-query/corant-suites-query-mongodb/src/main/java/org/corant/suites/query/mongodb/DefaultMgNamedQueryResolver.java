package org.corant.suites.query.mongodb;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.suites.query.shared.QueryParameterResolver;
import org.corant.suites.query.shared.QueryResultResolver;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.shared.mapping.Query;
import org.corant.suites.query.shared.mapping.QueryMappingService;

/**
 * corant-suites-query
 *
 * @author bingo 下午3:16:56
 *
 */
@ApplicationScoped
public class DefaultMgNamedQueryResolver implements MgInLineNamedQueryResolver<String, Object> {

  final Map<String, DefaultMgNamedQuerierBuilder> builders = new ConcurrentHashMap<>();

  @Inject
  protected QueryMappingService mappingService;

  @Inject
  protected QueryParameterResolver parameterResolver;

  @Inject
  protected QueryResultResolver resultResolver;

  @Override
  public DefaultMgNamedQuerier resolve(String key, Object param) {
    DefaultMgNamedQuerierBuilder builder = builders.computeIfAbsent(key, this::createBuilder);
    return builder.build(param);
  }

  protected DefaultMgNamedQuerierBuilder createBuilder(String key) {
    Query query = mappingService.getQuery(key);
    if (query == null) {
      throw new QueryRuntimeException("Can not found QueryService for key %s", key);
    }
    return new DefaultMgNamedQuerierBuilder(query, parameterResolver, resultResolver);
  }

}
