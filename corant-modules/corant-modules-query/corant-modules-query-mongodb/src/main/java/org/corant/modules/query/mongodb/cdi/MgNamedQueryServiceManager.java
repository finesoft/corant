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
package org.corant.modules.query.mongodb.cdi;

import static java.lang.String.format;
import static org.corant.shared.util.Assertions.shouldNotNull;
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
import org.bson.Document;
import org.corant.context.qualifier.Qualifiers;
import org.corant.modules.query.mapping.Query.QueryType;
import org.corant.modules.query.mongodb.AbstractMgNamedQueryService;
import org.corant.modules.query.mongodb.Decimal128Utils;
import org.corant.modules.query.mongodb.MgNamedQuerier;
import org.corant.modules.query.mongodb.MongoDatabases;
import org.corant.modules.query.shared.AbstractNamedQuerierResolver;
import org.corant.modules.query.shared.FetchableNamedQueryService;
import org.corant.modules.query.shared.NamedQueryServiceManager;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import com.mongodb.client.MongoDatabase;

/**
 * corant-modules-query-sql
 *
 * @author bingo 下午6:04:28
 *
 */
// @Priority(1)
@ApplicationScoped
// @Alternative
public class MgNamedQueryServiceManager implements NamedQueryServiceManager {

  protected final Map<String, FetchableNamedQueryService> services = new ConcurrentHashMap<>();

  @Inject
  protected Logger logger;

  @Inject
  protected AbstractNamedQuerierResolver<MgNamedQuerier> resolver;

  @Inject
  @ConfigProperty(name = "corant.query.mongodb.default-qualifier-value")
  protected Optional<String> defaultQualifierValue;

  @Inject
  @ConfigProperty(name = "corant.query.mongodb.convert-decimal", defaultValue = "true")
  protected boolean convertDecimal;

  @Override
  public FetchableNamedQueryService get(Object qualifier) {
    String key = resolveQualifier(qualifier);
    return services.computeIfAbsent(key, k -> {
      final String databaseName =
          isBlank(k) ? defaultQualifierValue.orElse(Qualifiers.EMPTY_NAME) : k;
      logger.fine(() -> format("Create default mongodb named query service, the data base is [%s].",
          databaseName));
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
  protected FetchableNamedQueryService produce(InjectionPoint ip) {
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
      return getAssembledConfigValue(((MgQuery) qualifier).value());
    } else {
      return getAssembledConfigValue(asDefaultString(qualifier));
    }
  }

  /**
   * corant-modules-query-mongodb
   *
   * @author bingo 下午3:41:34
   *
   */
  public static class DefaultMgNamedQueryService extends AbstractMgNamedQueryService {

    protected final MongoDatabase dataBase;
    protected final AbstractNamedQuerierResolver<MgNamedQuerier> resolver;
    protected final boolean convertDecimal;

    /**
     * @param dataBase
     * @param manager
     */
    public DefaultMgNamedQueryService(String dataBase, MgNamedQueryServiceManager manager) {
      this.dataBase = shouldNotNull(MongoDatabases.resolveDatabase(dataBase),
          "Can't build default mongo named query, the data base named %s not found.", dataBase);
      resolver = manager.resolver;
      convertDecimal = manager.convertDecimal;
    }

    /**
     * @param dataBase
     * @param convertDecimal
     * @param resolver
     */
    protected DefaultMgNamedQueryService(MongoDatabase dataBase, boolean convertDecimal,
        AbstractNamedQuerierResolver<MgNamedQuerier> resolver) {
      this.dataBase = dataBase;
      this.resolver = resolver;
      this.convertDecimal = convertDecimal;
    }

    @Override
    protected Map<String, Object> convertDocument(Document doc, MgNamedQuerier querier,
        boolean autoSetIdField) {
      if (doc == null) {
        return doc;
      } else if (autoSetIdField && !doc.containsKey("id") && doc.containsKey("_id")) {
        doc.put("id", doc.get("_id"));
      }
      return convertDecimal ? Decimal128Utils.convert(doc) : doc;
    }

    @Override
    protected MongoDatabase getDataBase() {
      return dataBase;
    }

    @Override
    protected AbstractNamedQuerierResolver<MgNamedQuerier> getQuerierResolver() {
      return resolver;
    }
  }
}
