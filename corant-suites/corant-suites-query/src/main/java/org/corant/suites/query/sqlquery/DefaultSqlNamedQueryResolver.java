/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.suites.query.sqlquery;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.shared.conversion.ConversionService;
import org.corant.suites.query.NamedQueryResolver;
import org.corant.suites.query.QueryRuntimeException;
import org.corant.suites.query.mapping.FetchQuery;
import org.corant.suites.query.mapping.Query;
import org.corant.suites.query.mapping.QueryMappingService;

/**
 * asosat-query
 *
 * @author bingo 下午3:16:56
 *
 */
@ApplicationScoped
public class DefaultSqlNamedQueryResolver
    implements NamedQueryResolver<String, Map<String, Object>, String, Object[], FetchQuery> {

  final Map<String, DefaultSqlNamedQueryTpl> cachedQueTpls = new ConcurrentHashMap<>();

  @Inject
  protected QueryMappingService mappingService;

  @Inject
  protected ConversionService conversionService;

  @Override
  public DefaultSqlNamedQuerier resolve(String key, Map<String, Object> param) {
    return cachedQueTpls.computeIfAbsent(key, this::buildQueryTemplate).process(param);
  }

  protected DefaultSqlNamedQueryTpl buildQueryTemplate(String key) {
    Query query = mappingService.getQuery(key);
    if (query == null) {
      throw new QueryRuntimeException("Can not found Query for key " + key);
    }
    return new DefaultSqlNamedQueryTpl(query, conversionService);
  }

}
