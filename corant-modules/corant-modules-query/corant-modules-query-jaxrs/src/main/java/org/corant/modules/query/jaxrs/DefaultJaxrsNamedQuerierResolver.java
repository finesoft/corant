/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.query.jaxrs;

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.shared.AbstractNamedQuerierResolver;
import org.corant.modules.query.shared.NamedQuerierBuilder;
import org.corant.shared.exception.NotSupportedException;

/**
 * corant-modules-query-jaxrs
 *
 * @author bingo 17:20:45
 */
@ApplicationScoped
public class DefaultJaxrsNamedQuerierResolver
    extends AbstractNamedQuerierResolver<JaxrsNamedQuerier> {

  protected final Map<String, NamedQuerierBuilder<JaxrsNamedQuerier>> builders =
      new ConcurrentHashMap<>();

  @Inject
  protected Logger logger;

  @Inject
  @Any
  protected JaxrsNamedQueryClientResolver clientResolver;

  @Override
  public void beforeQueryMappingInitialize(Collection<Query> queries, long initializedVersion) {
    clearBuilders();
  }

  @Override
  public JaxrsNamedQuerier resolve(Query query, QueryParameter param) {
    final String queryName = query.getVersionedName();
    NamedQuerierBuilder<JaxrsNamedQuerier> builder = builders.get(queryName);
    if (builder == null) {
      // Note: this.builders & QueryMappingService.queries may cause deadlock
      builder = builders.computeIfAbsent(queryName, k -> createBuilder(query));
    }
    return builder.build(param);
  }

  protected void clearBuilders() {
    if (!builders.isEmpty()) {
      builders.clear();
      logger.fine(() -> "Clear default jaxrs named querier resolver builders.");
    }
  }

  protected NamedQuerierBuilder<JaxrsNamedQuerier> createBuilder(Query query) {
    String queryName = query.getVersionedName();
    JaxrsNamedQueryClientConfig clientConfig = clientResolver.getClientConfig(query);
    Client client = shouldNotNull(clientResolver.apply(query),
        () -> new QueryRuntimeException("Can't find Jaxrs client for %s", queryName));
    return switch (query.getScript().getType()) {
      case JS -> new JavaScriptJaxrsQuerierBuilder(query, getQueryHandler(), getFetchQueryHandler(),
          client, clientConfig);
      case CDI -> new JavaJaxrsQuerierBuilder(query, getQueryHandler(), getFetchQueryHandler(),
          client, clientConfig);
      case JSE -> new JsonExpressionJaxrsQuerierBuilder(query, getQueryHandler(),
          getFetchQueryHandler(), client, clientConfig);
      case FM -> new FreemarkerJaxrsQuerierBuilder(query, getQueryHandler(), getFetchQueryHandler(),
          client, clientConfig);
      default -> throw new NotSupportedException("The query script type %s not support!",
          query.getScript().getType());
    };
  }

  @PreDestroy
  protected void onPreDestroy() {
    clearBuilders();
  }

}
