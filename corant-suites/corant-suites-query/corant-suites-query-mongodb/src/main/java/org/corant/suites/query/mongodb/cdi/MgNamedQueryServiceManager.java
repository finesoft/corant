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
package org.corant.suites.query.mongodb.cdi;

import static org.corant.kernel.util.Instances.resolveNamed;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.StringUtils.defaultString;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import org.corant.suites.query.mongodb.AbstractMgNamedQueryService;
import org.corant.suites.query.mongodb.MgInLineNamedQueryResolver;
import org.corant.suites.query.shared.NamedQueryService;
import org.corant.suites.query.shared.Querier;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import com.mongodb.client.MongoDatabase;

/**
 * corant-suites-query-sql
 *
 * @author bingo 下午6:04:28
 *
 */
@Priority(1)
@ApplicationScoped
@Alternative
public class MgNamedQueryServiceManager {

  static final Map<String, NamedQueryService> services = new ConcurrentHashMap<>(); // FIXME scope

  @Inject
  protected MgInLineNamedQueryResolver<String, Object> resolver;

  @Inject
  @ConfigProperty(name = "query.mongodb.max-select-size", defaultValue = "128")
  protected Integer maxSelectSize;

  @Produces
  @MgQuery
  NamedQueryService produce(InjectionPoint ip) {
    final Annotated annotated = ip.getAnnotated();
    final MgQuery sc = shouldNotNull(annotated.getAnnotation(MgQuery.class));
    final String dataBase = defaultString(sc.value());
    return services.computeIfAbsent(dataBase, (db) -> {
      return new DefaultMgNamedQueryService(db, resolver, maxSelectSize);
    });
  }

  /**
   * corant-suites-query-mongodb
   *
   * @author bingo 下午3:41:34
   *
   */
  public static final class DefaultMgNamedQueryService extends AbstractMgNamedQueryService {

    final MongoDatabase dataBase;

    final int defaultMaxSelectSize;

    /**
     * @param dataBase
     */
    public DefaultMgNamedQueryService(String dataBase,
        MgInLineNamedQueryResolver<String, Object> resolver, Integer maxSelectSize) {
      this.dataBase = resolveNamed(MongoDatabase.class, dataBase).get();
      logger = Logger.getLogger(this.getClass().getName());
      this.resolver = resolver;
      defaultMaxSelectSize = maxSelectSize;
    }

    @Override
    protected MongoDatabase getDataBase() {
      return dataBase;
    }

    @Override
    protected int getMaxSelectSize(Querier querier) {
      return querier.getQuery().getProperty(PRO_KEY_MAX_SELECT_SIZE, Integer.class,
          defaultMaxSelectSize);
    }
  }
}
