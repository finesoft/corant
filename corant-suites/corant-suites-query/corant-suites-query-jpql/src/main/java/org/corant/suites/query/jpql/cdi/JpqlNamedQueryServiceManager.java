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
package org.corant.suites.query.jpql.cdi;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.ObjectUtils.forceCast;
import static org.corant.shared.util.StringUtils.isBlank;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import org.corant.suites.jpa.shared.PersistenceService;
import org.corant.suites.query.jpql.AbstractJpqlNamedQueryService;
import org.corant.suites.query.jpql.JpqlNamedQuerier;
import org.corant.suites.query.shared.NamedQuerierResolver;
import org.corant.suites.query.shared.NamedQueryService;
import org.corant.suites.query.shared.NamedQueryServiceManager;
import org.corant.suites.query.shared.Querier;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-suites-query-sql
 *
 * @author bingo 下午6:04:28
 *
 */
@Priority(1)
@ApplicationScoped
@Alternative
public class JpqlNamedQueryServiceManager implements NamedQueryServiceManager {

  static final Map<String, NamedQueryService> services = new ConcurrentHashMap<>();// FIXME scope

  @Inject
  protected Logger logger;

  @Inject
  protected PersistenceService persistenceService;

  @Inject
  protected NamedQuerierResolver<String, Object, JpqlNamedQuerier> resolver;

  @Inject
  @ConfigProperty(name = "query.jpql.max-select-size", defaultValue = "128")
  protected Integer maxSelectSize;

  @Inject
  @ConfigProperty(name = "query.jpql.limit", defaultValue = "16")
  protected Integer limit;

  @Inject
  @ConfigProperty(name = "query.jpql.default-qualifier-value")
  protected Optional<String> defaultQualifierValue;

  @Override
  public NamedQueryService get(Object qualifier) {
    final JpqlQuery sc = forceCast(qualifier);
    String pun = sc == null ? null : sc.value();
    if (isBlank(pun) && defaultQualifierValue.isPresent()) {
      pun = defaultQualifierValue.get();
    }
    final String pu = pun;
    return services.computeIfAbsent(pu, n -> {
      logger.info(() -> String
          .format("Create default jpql named query service, the persistence unit is [%s].", n));
      return new DefaultJpqlNamedQueryService(persistenceService.getEntityManagerFactory(n), this);
    });
  }

  @Produces
  @JpqlQuery
  NamedQueryService produce(InjectionPoint ip) {
    final Annotated annotated = ip.getAnnotated();
    final JpqlQuery sc = shouldNotNull(annotated.getAnnotation(JpqlQuery.class));
    return get(sc);
  }

  /**
   * corant-suites-query-jpql
   *
   * @author bingo 下午12:02:33
   *
   */
  public static class DefaultJpqlNamedQueryService extends AbstractJpqlNamedQueryService {

    protected final EntityManagerFactory emf;
    protected final int defaultMaxSelectSize;
    protected final int defaultLimit;
    protected final NamedQuerierResolver<String, Object, JpqlNamedQuerier> resolver;

    /**
     * @param emf
     * @param resolver
     * @param maxSelectSize
     */
    public DefaultJpqlNamedQueryService(EntityManagerFactory emf,
        JpqlNamedQueryServiceManager manager) {
      this.emf = emf;
      resolver = manager.resolver;
      defaultMaxSelectSize = manager.maxSelectSize;
      defaultLimit = manager.limit < 1 ? DEFAULT_LIMIT : manager.limit;
    }

    /**
     * @param emf
     * @param defaultMaxSelectSize
     * @param defaultLimit
     * @param resolver
     */
    protected DefaultJpqlNamedQueryService(EntityManagerFactory emf, int defaultMaxSelectSize,
        int defaultLimit, NamedQuerierResolver<String, Object, JpqlNamedQuerier> resolver) {
      super();
      this.emf = emf;
      this.defaultMaxSelectSize = defaultMaxSelectSize;
      this.defaultLimit = defaultLimit;
      this.resolver = resolver;
    }

    @Override
    protected EntityManagerFactory getEntityManagerFactory() {
      return emf;
    }

    @Override
    protected NamedQuerierResolver<String, Object, JpqlNamedQuerier> getResolver() {
      return resolver;
    }

    @Override
    protected int resolveDefaultLimit(Querier querier) {
      return querier.getQuery().getProperty(PRO_KEY_DEFAULT_LIMIT, Integer.class, defaultLimit);
    }

    @Override
    protected int resolveMaxSelectSize(Querier querier) {
      return querier.getQuery().getProperty(PRO_KEY_MAX_SELECT_SIZE, Integer.class,
          defaultMaxSelectSize);
    }
  }

}
