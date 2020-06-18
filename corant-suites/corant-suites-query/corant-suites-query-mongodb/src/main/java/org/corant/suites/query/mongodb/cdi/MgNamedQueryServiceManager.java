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

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Strings.EMPTY;
import static org.corant.shared.util.Strings.asDefaultString;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.suites.cdi.Instances.findNamed;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import org.bson.Document;
import org.corant.suites.query.mongodb.AbstractMgNamedQueryService;
import org.corant.suites.query.mongodb.Decimal128Utils;
import org.corant.suites.query.mongodb.MgNamedQuerier;
import org.corant.suites.query.mongodb.MgNamedQueryService;
import org.corant.suites.query.shared.AbstractNamedQuerierResolver;
import org.corant.suites.query.shared.NamedQueryServiceManager;
import org.corant.suites.query.shared.mapping.Query.QueryType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;

/**
 * corant-suites-query-sql
 *
 * @author bingo 下午6:04:28
 *
 */
// @Priority(1)
@ApplicationScoped
// @Alternative
public class MgNamedQueryServiceManager implements NamedQueryServiceManager {

  protected final Map<String, MgNamedQueryService> services = new ConcurrentHashMap<>(); // FIXME
  // scope

  @Inject
  protected Logger logger;

  @Inject
  protected AbstractNamedQuerierResolver<MgNamedQuerier> resolver;

  @Inject
  @ConfigProperty(name = "query.mongodb.max-select-size", defaultValue = "128")
  protected Integer maxSelectSize;

  @Inject
  @ConfigProperty(name = "query.mongodb.limit", defaultValue = "16")
  protected Integer limit;

  @Inject
  @ConfigProperty(name = "query.mongodb.default-qualifier-value")
  protected Optional<String> defaultQualifierValue;

  @Inject
  @ConfigProperty(name = "query.mongodb.convert-decimal", defaultValue = "true")
  protected boolean convertDecimal;

  @Override
  public MgNamedQueryService get(Object qualifier) {
    String key = resolveQualifier(qualifier);
    return services.computeIfAbsent(key, k -> {
      final String databaseName = isBlank(k) ? defaultQualifierValue.orElse(EMPTY) : k;
      logger.fine(() -> String.format(
          "Create default mongodb named query service, the data base is [%s].", databaseName));
      return new DefaultMgNamedQueryService(databaseName, this);
    });
  }

  @Override
  public QueryType getType() {
    return QueryType.MG;
  }

  @PreDestroy
  protected synchronized void onPreDestroy() {
    services.clear();
    logger.fine(() -> "Clear cached named query services.");
  }

  @Produces
  @MgQuery
  protected MgNamedQueryService produce(InjectionPoint ip) {
    Annotation qualifier = null;
    for (Annotation a : ip.getQualifiers()) {
      if (a.annotationType().equals(MgQuery.class)) {
        qualifier = a;
        break;
      }
    }
    return get(qualifier);
  }

  protected String resolveQualifier(Object qualifier) {
    if (qualifier instanceof MgQuery) {
      return ((MgQuery) qualifier).value();
    } else {
      return asDefaultString(qualifier);
    }
  }

  /**
   * corant-suites-query-mongodb
   *
   * @author bingo 下午3:41:34
   *
   */
  public static class DefaultMgNamedQueryService extends AbstractMgNamedQueryService {

    protected final MongoDatabase dataBase;
    protected final int defaultMaxSelectSize;
    protected final int defaultLimit;
    protected final AbstractNamedQuerierResolver<MgNamedQuerier> resolver;
    protected final boolean convertDecimal;

    /**
     * @param dataBase
     * @param manager
     */
    public DefaultMgNamedQueryService(String dataBase, MgNamedQueryServiceManager manager) {
      this.dataBase = shouldNotNull(resolveDataBase(dataBase),
          "Can't build default mongo named query, the data base named %s not found.", dataBase);
      resolver = manager.resolver;
      defaultMaxSelectSize = manager.maxSelectSize;
      defaultLimit = manager.limit < 1 ? DEFAULT_LIMIT : manager.limit;
      convertDecimal = manager.convertDecimal;
    }

    /**
     * @param dataBase
     * @param defaultMaxSelectSize
     * @param defaultLimit
     * @param convertDecimal
     * @param resolver
     */
    protected DefaultMgNamedQueryService(MongoDatabase dataBase, int defaultMaxSelectSize,
        int defaultLimit, boolean convertDecimal,
        AbstractNamedQuerierResolver<MgNamedQuerier> resolver) {
      super();
      this.dataBase = dataBase;
      this.defaultMaxSelectSize = defaultMaxSelectSize;
      this.defaultLimit = defaultLimit;
      this.resolver = resolver;
      this.convertDecimal = convertDecimal;
    }

    @Override
    protected Map<String, Object> convertDocument(Document doc, MgNamedQuerier querier) {
      boolean autoSetIdField =
          resolveProperties(querier, PRO_KEY_AUTO_SET_ID_FIELD, Boolean.class, Boolean.TRUE);
      if (autoSetIdField && !doc.containsKey("id") && doc.containsKey("_id")) {
        doc.put("id", doc.get("_id"));
      }
      return convertDecimal ? Decimal128Utils.convert(doc) : doc;
    }

    @Override
    protected MongoDatabase getDataBase() {
      return dataBase;
    }

    @Override
    protected int getDefaultLimit() {
      return defaultLimit;
    }

    @Override
    protected int getDefaultMaxSelectSize() {
      return defaultMaxSelectSize;
    }

    @Override
    protected AbstractNamedQuerierResolver<MgNamedQuerier> getQuerierResolver() {
      return resolver;
    }

    @SuppressWarnings("resource")
    protected MongoDatabase resolveDataBase(String dataBase) {
      if (isNotBlank(dataBase)
          && (dataBase.startsWith("mongodb+srv://") || dataBase.startsWith("mongodb://"))) {
        String db = dataBase.substring(dataBase.lastIndexOf('/'));
        return new MongoClient(new MongoClientURI(dataBase)).getDatabase(db);
      } else {
        return findNamed(MongoDatabase.class, dataBase).orElse(null);
      }
    }
  }
}
