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
import org.corant.modules.query.jaxrs.JaxrsNamedQuerier.WebTargetConfig;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.shared.AbstractNamedQuerierResolver;

/**
 * corant-modules-query-jaxrs
 *
 * @author bingo 17:20:45
 */
@ApplicationScoped
public class DefaultJaxrsNamedQuerierResolver
    extends AbstractNamedQuerierResolver<JaxrsNamedQuerier> {

  protected final Map<String, DefaultJaxrsNamedQuerierBuilder> builders = new ConcurrentHashMap<>();

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
  public DefaultJaxrsNamedQuerier resolve(Query query, QueryParameter param) {
    final String queryName = query.getVersionedName();
    DefaultJaxrsNamedQuerierBuilder builder = builders.get(queryName);
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

  protected DefaultJaxrsNamedQuerierBuilder createBuilder(Query query) {
    String queryName = query.getVersionedName();
    JaxrsNamedQueryClientConfig clientConfig = clientResolver.getClientConfig(query);
    Client client = shouldNotNull(clientResolver.apply(query),
        () -> new QueryRuntimeException("Can't find Jaxrs client for %s", queryName));
    WebTargetConfig config = shouldNotNull(
        getQueryHandler().getObjectMapper().fromJsonString(query.getScript().getCode(),
            WebTargetConfig.class),
        () -> new QueryRuntimeException("Can't find Jaxrs client for %s", queryName));
    config.postConstruct();
    return new DefaultJaxrsNamedQuerierBuilder(query, getQueryHandler(), getFetchQueryHandler(),
        client, clientConfig, config);
  }

  @PreDestroy
  protected void onPreDestroy() {
    clearBuilders();
  }

}
