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

import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import org.corant.kernel.api.DataSourceService;
import org.corant.kernel.api.PersistenceService;
import org.corant.suites.query.jpql.AbstractJpqlNamedQueryService;
import org.corant.suites.query.shared.NamedQueryService;

/**
 * corant-suites-query-sql
 *
 * @author bingo 下午6:04:28
 *
 */
@Priority(1)
@ApplicationScoped
@Alternative
public class JpqlNamedQueryServiceManager {

  static final Map<String, NamedQueryService> services = new ConcurrentHashMap<>();

  @Inject
  DataSourceService dataSourceService;

  @Inject
  PersistenceService persistenceService;

  @Produces
  @ApplicationScoped
  @JpqlQuery
  NamedQueryService produce(InjectionPoint ip) {
    final Annotated annotated = ip.getAnnotated();
    final JpqlQuery sc = shouldNotNull(annotated.getAnnotation(JpqlQuery.class));
    final String pu = shouldNotBlank(sc.value());
    return services.computeIfAbsent(pu, (dc) -> {
      return new DefaultJpqlNamedQueryService(persistenceService.getEntityManagerFactory(pu));
    });
  }

  /**
   * corant-suites-query-jpql
   *
   * @author bingo 下午12:02:33
   *
   */
  public static final class DefaultJpqlNamedQueryService extends AbstractJpqlNamedQueryService {

    private final EntityManagerFactory emf;

    public DefaultJpqlNamedQueryService(EntityManagerFactory emf) {
      this.emf = emf;
    }

    @Override
    protected EntityManagerFactory getEntityManagerFactory() {
      return emf;
    }
  }
}
