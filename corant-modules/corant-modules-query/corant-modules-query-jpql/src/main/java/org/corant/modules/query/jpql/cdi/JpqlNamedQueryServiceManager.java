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
package org.corant.modules.query.jpql.cdi;

import static org.corant.shared.util.Configurations.getAssembledConfigValue;
import static org.corant.shared.util.Strings.asDefaultString;
import static org.corant.shared.util.Strings.isBlank;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;
import org.corant.context.qualifier.Qualifiers;
import org.corant.modules.jpa.shared.PersistenceService;
import org.corant.modules.query.jpql.AbstractJpqlNamedQueryService;
import org.corant.modules.query.jpql.JpqlNamedQuerier;
import org.corant.modules.query.mapping.Query.QueryType;
import org.corant.modules.query.shared.AbstractNamedQuerierResolver;
import org.corant.modules.query.shared.FetchableNamedQueryService;
import org.corant.modules.query.shared.NamedQueryServiceManager;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-modules-query-jpql
 *
 * @author bingo 下午6:04:28
 *
 */
// @Priority(1)
@ApplicationScoped
// @Alternative
public class JpqlNamedQueryServiceManager implements NamedQueryServiceManager {

  protected final Map<String, FetchableNamedQueryService> services = new ConcurrentHashMap<>();

  @Inject
  protected Logger logger;

  @Inject
  protected PersistenceService persistenceService;

  @Inject
  protected AbstractNamedQuerierResolver<JpqlNamedQuerier> resolver;

  @Inject
  @ConfigProperty(name = "corant.query.jpql.default-qualifier-value")
  protected Optional<String> defaultQualifierValue;

  @Override
  public FetchableNamedQueryService get(Object qualifier) {
    String key = resolveQualifier(qualifier);
    return services.computeIfAbsent(key, k -> {
      final String pu = isBlank(k) ? defaultQualifierValue.orElse(Qualifiers.EMPTY_NAME) : k;
      logger.fine(() -> String
          .format("Create default jpql named query service, the persistence unit is [%s].", pu));
      return new DefaultJpqlNamedQueryService(persistenceService.getEntityManagerFactory(pu), this);
    });
  }

  @Override
  public QueryType getType() {
    return QueryType.JPQL;
  }

  @PreDestroy
  protected synchronized void onPreDestroy() {
    services.clear();
    logger.fine(() -> "Clear cached named query services.");
  }

  @Produces
  @JpqlQuery
  protected FetchableNamedQueryService produce(InjectionPoint ip) {
    Annotation qualifier = null;
    for (Annotation a : ip.getQualifiers()) {
      if (a.annotationType().equals(JpqlQuery.class)) {
        qualifier = a;
        break;
      }
    }
    return get(qualifier);
  }

  protected String resolveQualifier(Object qualifier) {
    return getAssembledConfigValue(qualifier instanceof JpqlQuery ? ((JpqlQuery) qualifier).value()
        : asDefaultString(qualifier));
  }

  /**
   * corant-modules-query-jpql
   *
   * @author bingo 下午12:02:33
   *
   */
  public static class DefaultJpqlNamedQueryService extends AbstractJpqlNamedQueryService {

    protected final EntityManagerFactory emf;
    protected final AbstractNamedQuerierResolver<JpqlNamedQuerier> resolver;

    /**
     * @param emf
     * @param manager
     */
    public DefaultJpqlNamedQueryService(EntityManagerFactory emf,
        JpqlNamedQueryServiceManager manager) {
      this.emf = emf;
      resolver = manager.resolver;
    }

    /**
     * @param emf
     * @param resolver
     */
    protected DefaultJpqlNamedQueryService(EntityManagerFactory emf,
        AbstractNamedQuerierResolver<JpqlNamedQuerier> resolver) {
      this.emf = emf;
      this.resolver = resolver;
    }

    @Override
    protected EntityManagerFactory getEntityManagerFactory() {
      return emf;
    }

    @Override
    protected AbstractNamedQuerierResolver<JpqlNamedQuerier> getQuerierResolver() {
      return resolver;
    }

  }

}
