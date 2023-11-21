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
package org.corant.modules.query.elastic;

import static org.corant.shared.util.Objects.forceCast;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.shared.AbstractNamedQuerierResolver;
import org.corant.shared.exception.NotSupportedException;
import net.jcip.annotations.GuardedBy;

/**
 * corant-modules-query-elastic
 *
 * @author bingo 下午3:16:56
 *
 */
@ApplicationScoped
public class DefaultEsNamedQuerierResolver extends AbstractNamedQuerierResolver<EsNamedQuerier> {

  protected final Map<String, FreemarkerEsQuerierBuilder> builders = new ConcurrentHashMap<>();

  @Inject
  protected Logger logger;

  @GuardedBy("QueryMappingService.rwl.writeLock")
  @Override
  public void beforeQueryMappingInitialize(Collection<Query> queries, long initializedVersion) {
    clearBuilders();
  }

  @Override
  public EsNamedQuerier resolve(String name, Object param) {
    FreemarkerEsQuerierBuilder builder = builders.get(name);
    if (builder == null) {
      // Note: this.builders & QueryMappingService.queries may cause deadlock
      Query query = resolveQuery(name);
      builder = builders.computeIfAbsent(name, k -> createBuilder(query));
    }
    return forceCast(builder.build(param));
  }

  protected FreemarkerEsQuerierBuilder createBuilder(Query query) {
    switch (query.getScript().getType()) {
      case CDI:
      case JSE:
      case JS:
      case KT:
        throw new NotSupportedException("The query script type %s not support!",
            query.getScript().getType());
      default:
        return new FreemarkerEsQuerierBuilder(query, getQueryHandler(), getFetchQueryHandler());
    }
  }

  @PreDestroy
  protected void onPreDestroy() {
    clearBuilders();
  }

  void clearBuilders() {
    if (!builders.isEmpty()) {
      builders.clear();
      logger.fine(() -> "Clear default elastic named querier resolver builders");
    }
  }
}
