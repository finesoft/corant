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
package org.corant.modules.query.cassandra;

import static org.corant.shared.util.Objects.forceCast;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.shared.AbstractNamedQuerierResolver;
import org.corant.modules.query.shared.dynamic.DynamicQuerierBuilder;
import org.corant.modules.query.shared.dynamic.freemarker.DynamicTemplateMethodModelEx;
import org.corant.modules.query.shared.dynamic.freemarker.FreemarkerDynamicQuerierBuilder;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.ubiquity.Tuple.Triple;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.jcip.annotations.GuardedBy;

/**
 * corant-modules-query-cassandra
 *
 * @author bingo 下午3:16:56
 */
@ApplicationScoped
@SuppressWarnings({"rawtypes"})
public class DefaultCasNamedQuerierResolver extends AbstractNamedQuerierResolver<CasNamedQuerier> {

  protected final Map<String, DynamicQuerierBuilder> builders = new ConcurrentHashMap<>();

  @Inject
  protected Logger logger;

  @Inject
  @ConfigProperty(name = "corant.query.cassandra.mapping-file.paths")
  protected Optional<String> mappingFilePaths;

  @GuardedBy("QueryMappingService.rwl.writeLock")
  @Override
  public void beforeQueryMappingInitialize(Collection<Query> queries, long initializedVersion) {
    clearBuilders();
  }

  @Override
  public DefaultCasNamedQuerier resolve(String name, Object param) {
    DynamicQuerierBuilder builder = builders.get(name);
    if (builder == null) {
      /* Note: this.builders & QueryMappingService.queries may cause deadlock */
      Query query = resolveQuery(name);
      builder = builders.computeIfAbsent(name, k -> createBuilder(query));
    }
    return forceCast(builder.build(param));
  }

  protected DynamicQuerierBuilder createBuilder(Query query) {
    switch (query.getScript().getType()) {
      case CDI:
      case JSE:
      case JS:
      case KT:
        throw new NotSupportedException("The query script type %s not support!",
            query.getScript().getType());
      default:
        return createFmBuilder(query);
    }
  }

  protected DynamicQuerierBuilder createFmBuilder(Query query) {
    return new FreemarkerDynamicQuerierBuilder<Object[], String, DefaultCasNamedQuerier>(query,
        getQueryHandler(), getFetchQueryHandler()) {

      @Override
      protected DefaultCasNamedQuerier build(Triple<QueryParameter, Object[], String> processed) {
        return new DefaultCasNamedQuerier(getQuery(), processed.getLeft(), getQueryHandler(),
            getFetchQueryHandler(), processed.getMiddle(), processed.getRight());
      }

      @SuppressWarnings("unchecked")
      @Override
      protected DynamicTemplateMethodModelEx getTemplateMethodModelEx() {
        return new CasTemplateMethodModelEx();
      }

    };
  }

  @PreDestroy
  protected void onPreDestroy() {
    clearBuilders();
  }

  void clearBuilders() {
    if (!builders.isEmpty()) {
      builders.clear();
      logger.fine(() -> "Clear default cassandra named querier resolver builders");
    }
  }
}
