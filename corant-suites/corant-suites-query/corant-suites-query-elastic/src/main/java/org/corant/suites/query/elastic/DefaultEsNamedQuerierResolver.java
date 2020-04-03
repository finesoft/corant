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
package org.corant.suites.query.elastic;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.suites.query.shared.AbstractNamedQuerierResolver;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.shared.mapping.Query;

/**
 * corant-suites-query
 *
 * @author bingo 下午3:16:56
 *
 */
@ApplicationScoped
public class DefaultEsNamedQuerierResolver extends AbstractNamedQuerierResolver<EsNamedQuerier> {

  final Map<String, FreemarkerEsQuerierBuilder> builders = new ConcurrentHashMap<>();

  @Inject
  Logger logger;

  @Override
  public EsNamedQuerier resolve(String key, Object param) {
    FreemarkerEsQuerierBuilder builder = builders.computeIfAbsent(key, this::createBuilder);
    return builder.build(param);
  }

  protected FreemarkerEsQuerierBuilder createBuilder(String key) {
    Query query = mappingService.getQuery(key);
    if (query == null) {
      throw new QueryRuntimeException("Can not find name query for name [%s]", key);
    }
    return new FreemarkerEsQuerierBuilder(query, queryResolver, fetchQueryResolver);
  }

  @PreDestroy
  synchronized void onPreDestroy() {
    builders.clear();
    logger.fine(() -> "Clear default elastic named querier resolver builders");
  }

}
