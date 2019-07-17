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
package org.corant.suites.jpa.hibernate;

import static org.corant.shared.util.ClassUtils.getUserClass;
import static org.corant.shared.util.ConversionUtils.toLong;
import static org.corant.shared.util.MapUtils.getMapInstant;
import static org.corant.shared.util.MapUtils.mapOf;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.bson.Document;
import org.corant.shared.util.Identifiers;
import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import com.mongodb.client.MongoDatabase;

/**
 * corant-suites-jpa-hibernate
 *
 * @author bingo 下午2:06:44
 *
 */
public class HibernateSnowflakeIdGenerator implements IdentifierGenerator {
  static Logger logger = Logger.getLogger(HibernateSnowflakeIdGenerator.class.getName());
  static final String IDGEN_SF_WK_ID = "identifier.generator.snowflake.worker-id";
  static final String IDGEN_SF_DC_ID = "identifier.generator.snowflake.datacenter-id";
  static final String IDGEN_SF_TIME = "identifier.generator.snowflake.use-persistence-timer";
  static Identifiers.IdentifierGenerator GENERATOR;
  static volatile boolean ENABLED = false;
  static volatile int DATA_CENTER_ID;
  static volatile int WORKER_ID;
  static volatile boolean usePersistenceTime =
      getConfig().getOptionalValue(IDGEN_SF_TIME, Boolean.class).orElse(true);
  static Map<Class<?>, Supplier<?>> timeSuppliers = new ConcurrentHashMap<>();

  static {
    DATA_CENTER_ID = getConfig().getOptionalValue(IDGEN_SF_DC_ID, Integer.class).orElse(-1);
    WORKER_ID = getConfig().getOptionalValue(IDGEN_SF_WK_ID, Integer.class).orElse(0);
    logger.info(() -> String.format(
        "Use Snowflake id generator for hibernate data center id is %s, worker id is %s.",
        DATA_CENTER_ID, WORKER_ID));
    if (DATA_CENTER_ID >= 0) {
      GENERATOR = Identifiers.snowflakeUUIDGenerator(DATA_CENTER_ID, WORKER_ID);
    } else {
      GENERATOR = Identifiers.snowflakeBufferUUIDGenerator(WORKER_ID, true);
    }
    ENABLED = true;
  }

  @Override
  public Serializable generate(SharedSessionContractImplementor session, Object object)
      throws HibernateException {
    return GENERATOR.generate(() -> getTimeSeq(session, object));
  }

  long getTimeSeq(SharedSessionContractImplementor session, Object object) {
    if (!usePersistenceTime) {
      return System.currentTimeMillis();
    }
    return toLong(resolveTimer(session, object).get());
  }

  Supplier<?> resolveTimer(final SharedSessionContractImplementor session, Object object) {
    return timeSuppliers.computeIfAbsent(getUserClass(object.getClass()), c -> {
      MongoDBDatastoreProvider mp = resolveMongoDBDatastoreProvider(session);
      if (mp != null) {
        final MongoDatabase md = mp.getDatabase();
        final Document timeBson =
            new Document(mapOf("serverStatus", 1, "repl", 0, "metrics", 0, "locks", 0));
        return () -> {
          return getMapInstant(md.runCommand(timeBson), "localTime").toEpochMilli();
        };
      } else {
        final String timeSql = session.getFactory().getServiceRegistry()
            .getService(JdbcServices.class).getDialect().getCurrentTimestampSelectString();
        return () -> {
          try {
            final PreparedStatement st =
                session.getJdbcCoordinator().getStatementPreparer().prepareStatement(timeSql);
            try {
              final ResultSet rs = session.getJdbcCoordinator().getResultSetReturn().extract(st);
              try {
                rs.next();
                return rs.getTimestamp(1).getTime();
              } finally {
                try {
                  session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry()
                      .release(rs, st);
                } catch (Throwable ignore) {
                }
              }
            } finally {
              session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release(st);
              session.getJdbcCoordinator().afterStatementExecution();
            }

          } catch (SQLException sqle) {
            throw session.getFactory().getServiceRegistry().getService(JdbcServices.class)
                .getSqlExceptionHelper()
                .convert(sqle, "could not get next sequence value", timeSql);
          }
        };
      }
    });
  }

  private MongoDBDatastoreProvider resolveMongoDBDatastoreProvider(
      SharedSessionContractImplementor session) {
    try {
      return session.getFactory().getServiceRegistry().getService(MongoDBDatastoreProvider.class);
    } catch (Exception e) {
      // Noop FIXME
    }
    return null;
  }
}
