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
package org.corant.modules.query.jaxrs.cdi;

import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import org.corant.modules.query.FetchableNamedQueryService;
import org.corant.modules.query.QueryHandler;
import org.corant.modules.query.jaxrs.AbstractJaxrsNamedQueryService;
import org.corant.modules.query.jaxrs.JaxrsNamedQuerier;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.mapping.Query.QueryType;
import org.corant.modules.query.shared.AbstractNamedQuerierResolver;
import org.corant.modules.query.shared.NamedQueryServiceManager;
import org.corant.shared.ubiquity.Experimental;

/**
 * corant-modules-query-jaxrs
 *
 * @author bingo 下午6:04:28
 */
@Experimental
@ApplicationScoped
public class JaxrsNamedQueryServiceManager implements NamedQueryServiceManager {

  @Inject
  protected Logger logger;

  @Inject
  protected AbstractNamedQuerierResolver<JaxrsNamedQuerier> resolver;

  protected AbstractJaxrsNamedQueryService service;

  @Override
  public FetchableNamedQueryService get(Object qualifier) {
    // FIXME we only have one instance
    return service;
  }

  @Override
  public QueryType getType() {
    return QueryType.JAXRS;
  }

  @PostConstruct
  protected void onPostConstruct() {
    service = new DefaultJaxrsNamedQueryService(resolver);
  }

  @PreDestroy
  protected synchronized void onPreDestroy() {
    logger.fine(() -> "Clear cached named query services.");
  }

  @Produces
  @JaxrsQuery
  protected FetchableNamedQueryService produce(InjectionPoint ip) {
    return service;
  }

  /**
   * corant-modules-query-jaxrs
   *
   * @author bingo 下午3:41:34
   */
  public static class DefaultJaxrsNamedQueryService extends AbstractJaxrsNamedQueryService {

    protected final AbstractNamedQuerierResolver<JaxrsNamedQuerier> resolver;

    public DefaultJaxrsNamedQueryService(AbstractNamedQuerierResolver<JaxrsNamedQuerier> resolver) {
      this.resolver = resolver;
    }

    @Override
    protected AbstractNamedQuerierResolver<JaxrsNamedQuerier> getQuerierResolver() {
      return resolver;
    }

    @Override
    protected Query getQuery(String queryName) {
      return resolver.resolveQuery(queryName);
    }

    @Override
    protected QueryHandler getQueryHandler() {
      return resolver.getQueryHandler();
    }
  }
}
