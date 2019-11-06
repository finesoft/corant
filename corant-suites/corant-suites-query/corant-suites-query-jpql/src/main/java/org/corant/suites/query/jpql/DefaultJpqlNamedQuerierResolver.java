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
package org.corant.suites.query.jpql;

import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.ObjectUtils.forceCast;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.shared.exception.NotSupportedException;
import org.corant.suites.query.shared.FetchQueryResolver;
import org.corant.suites.query.shared.NamedQuerierResolver;
import org.corant.suites.query.shared.QueryResolver;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.shared.dynamic.DynamicQuerierBuilder;
import org.corant.suites.query.shared.mapping.Query;
import org.corant.suites.query.shared.mapping.QueryMappingService;
import org.corant.suites.query.shared.mapping.Script.ScriptType;

/**
 * corant-suites-query
 *
 * @author bingo 下午3:16:56
 *
 */
@SuppressWarnings("rawtypes")
@ApplicationScoped
public class DefaultJpqlNamedQuerierResolver
    implements NamedQuerierResolver<String, Object, JpqlNamedQuerier> {

  final Map<String, DynamicQuerierBuilder> builders = new ConcurrentHashMap<>();

  @Inject
  protected QueryMappingService mappingService;

  @Inject
  protected QueryResolver queryResolver;

  @Inject
  protected FetchQueryResolver fetchQueryResolver;

  @Override
  public DefaultJpqlNamedQuerier resolve(String key, Object param) {
    DynamicQuerierBuilder builder = builders.computeIfAbsent(key, this::createBuilder);
    return forceCast(builder.build(param));
  }

  protected DynamicQuerierBuilder createBuilder(String key) {
    Query query = mappingService.getQuery(key);
    if (query == null) {
      throw new QueryRuntimeException("Can not found QueryService for key %s", key);
    }
    if (isNotEmpty(query.getFetchQueries()) || isNotEmpty(query.getHints())) {
      throw new NotSupportedException();
    }
    // FIXME decide script engine
    if (query.getScript().getType() == ScriptType.JS) {
      return createJsProcessor(query);
    } else {
      return createFmProcessor(query);
    }
  }

  protected FreemarkerJpqlQuerierBuilder createFmProcessor(Query query) {
    return new FreemarkerJpqlQuerierBuilder(query, queryResolver, fetchQueryResolver);
  }

  protected JavascriptJpqlQuerierBuilder createJsProcessor(Query query) {
    return new JavascriptJpqlQuerierBuilder(query, queryResolver, fetchQueryResolver);
  }

}
