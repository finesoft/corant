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
package org.corant.modules.query.sql.cdi;

import static org.corant.context.Instances.findNamed;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Strings.asDefaultString;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.split;
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
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.corant.config.Configs;
import org.corant.context.qualifier.Qualifiers;
import org.corant.modules.query.NamedQueryService;
import org.corant.modules.query.mapping.Query.QueryType;
import org.corant.modules.query.shared.AbstractNamedQuerierResolver;
import org.corant.modules.query.shared.NamedQueryServiceManager;
import org.corant.modules.query.sql.AbstractSqlNamedQueryService;
import org.corant.modules.query.sql.DefaultSqlQueryExecutor;
import org.corant.modules.query.sql.SqlNamedQuerier;
import org.corant.modules.query.sql.SqlQueryConfiguration;
import org.corant.modules.query.sql.SqlQueryConfiguration.Builder;
import org.corant.modules.query.sql.SqlQueryExecutor;
import org.corant.modules.query.sql.dialect.Dialect.DBMS;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Names;
import org.corant.shared.normal.Names.JndiNames;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-modules-query-sql
 *
 * @author bingo 下午6:04:28
 *
 */
// @Priority(1)
@ApplicationScoped
// @Alternative
public class SqlNamedQueryServiceManager implements NamedQueryServiceManager {

  protected final Map<String, NamedQueryService> services = new ConcurrentHashMap<>();// FIXME scope

  @Inject
  protected Logger logger;

  @Inject
  protected AbstractNamedQuerierResolver<SqlNamedQuerier> resolver;

  @Inject
  @ConfigProperty(name = "corant.query.sql.fetch-size", defaultValue = "16")
  protected Integer fetchSize;

  @Inject
  @ConfigProperty(name = "corant.query.sql.fetch-direction")
  protected Optional<Integer> fetchDirection;

  @Inject
  @ConfigProperty(name = "corant.query.sql.max-field-size", defaultValue = "0")
  protected Integer maxFieldSize;

  @Inject
  @ConfigProperty(name = "corant.query.sql.default-qualifier-value")
  protected Optional<String> defaultQualifierValue;

  @Inject
  @ConfigProperty(name = "corant.query.sql.default-qualifier-dialect", defaultValue = "MYSQL")
  protected DBMS defaultQualifierDialect;

  @Override
  public NamedQueryService get(Object qualifier) {
    String key = resolveQualifer(qualifier);
    return services.computeIfAbsent(key, k -> {
      String dataSourceName = defaultQualifierValue.orElse(Qualifiers.EMPTY_NAME);
      DBMS dialect = defaultQualifierDialect;
      String useKey = k;
      boolean jndi = useKey.startsWith(JndiNames.JNDI_COMP_NME);
      if (jndi) {
        useKey = useKey.substring(JndiNames.JNDI_COMP_NME.length());
      }
      String[] qs = split(useKey, Names.DOMAIN_SPACE_SEPARATORS, true, true);
      if (qs.length > 0) {
        dataSourceName = jndi ? JndiNames.JNDI_COMP_NME.concat(qs[0]) : qs[0];
        if (qs.length > 1) {
          dialect = toObject(qs[1], DBMS.class);
        }
      }
      logger.fine(String.format(
          "Create default sql named query service, the data source is [%s] and dialect is [%s].",
          dataSourceName, dialect));
      return new DefaultSqlNamedQueryService(dataSourceName, dialect, this);
    });
  }

  @Override
  public QueryType getType() {
    return QueryType.SQL;
  }

  @PreDestroy
  protected synchronized void onPreDestroy() {
    services.clear();
    logger.fine(() -> "Clear cached named query services.");
  }

  @Produces
  @SqlQuery
  protected NamedQueryService produce(InjectionPoint ip) {
    Annotation qualifier = null;
    for (Annotation a : ip.getQualifiers()) {
      if (a.annotationType().equals(SqlQuery.class)) {
        qualifier = a;
        break;
      }
    }
    return get(qualifier);
  }

  protected String resolveQualifer(Object qualifier) {
    if (qualifier instanceof SqlQuery) {
      SqlQuery q = forceCast(qualifier);
      // try {
      // return new URI(q.dialect(), q.value(), null, null).toString();
      // } catch (URISyntaxException e) {
      // throw new CorantRuntimeException(e);
      // }
      return Configs.assemblyStringConfigProperty(
          q.value().concat(Names.DOMAIN_SPACE_SEPARATORS).concat(q.dialect()));
    } else {
      return Configs.assemblyStringConfigProperty(asDefaultString(qualifier));
    }
  }

  /**
   * corant-modules-query-sql
   *
   * @author bingo 下午6:54:23
   *
   */
  public static class DefaultSqlNamedQueryService extends AbstractSqlNamedQueryService {

    protected final SqlQueryExecutor executor;
    protected final AbstractNamedQuerierResolver<SqlNamedQuerier> resolver;

    /**
     * @param executor
     * @param defaultMaxSelectSize
     * @param defaultLimit
     * @param resolver
     */
    protected DefaultSqlNamedQueryService(SqlQueryExecutor executor,
        AbstractNamedQuerierResolver<SqlNamedQuerier> resolver) {
      this.executor = executor;
      this.resolver = resolver;
    }

    /**
     * @param dataSourceName
     * @param dbms
     * @param manager
     */
    protected DefaultSqlNamedQueryService(String dataSourceName, DBMS dbms,
        SqlNamedQueryServiceManager manager) {
      resolver = manager.resolver;
      Builder builder = SqlQueryConfiguration.defaultBuilder()
          .dataSource(shouldNotNull(resolveDataSource(dataSourceName),
              "Can't build default sql named query, the data source named %s not found.",
              dataSourceName))
          .dialect(dbms.instance()).fetchSize(manager.fetchSize).maxFieldSize(manager.maxFieldSize);
      //DON'T CONFIGURE MAX ROWS AND TIME OUT, USE QUERIER since 1.6.2
      /*
       * .maxRows(manager.maxRows).queryTimeout(manager.timeout.orElseGet(() -> { Duration d =
       * manager.resolver.getQueryHandler().getQuerierConfig().getTimeout(); if (d != null) { return
       * Long.valueOf(d.toSeconds()).intValue(); } return null; }));
       */
      manager.fetchDirection.ifPresent(builder::fetchDirection);
      executor = new DefaultSqlQueryExecutor(builder.build());
    }

    @Override
    protected SqlQueryExecutor getExecutor() {
      return executor;
    }

    @Override
    protected AbstractNamedQuerierResolver<SqlNamedQuerier> getQuerierResolver() {
      return resolver;
    }

    protected DataSource resolveDataSource(String dataSourceName) {
      if (isNotBlank(dataSourceName) && dataSourceName.startsWith(JndiNames.JNDI_COMP_NME)) {
        try {
          return forceCast(new InitialContext().lookup(dataSourceName));
        } catch (NamingException e) {
          throw new CorantRuntimeException(e);
        }
      } else {
        return findNamed(DataSource.class, dataSourceName).orElse(null);
      }
    }
  }
}
