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
import static org.corant.shared.util.ConversionUtils.toInteger;
import static org.corant.shared.util.ConversionUtils.toObject;
import static org.corant.shared.util.MapUtils.getMapInstant;
import static org.corant.shared.util.MapUtils.mapOf;
import static org.corant.shared.util.StringUtils.isNotBlank;
import static org.corant.shared.util.StringUtils.split;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bson.Document;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Identifiers;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;

/**
 * corant-suites-jpa-hibernate
 *
 * @author bingo 下午2:06:44
 *
 */
public class HibernateSnowflakeIdGenerator implements IdentifierGenerator {
  static Logger logger = Logger.getLogger(HibernateSnowflakeIdGenerator.class.getName());
  static final String SYS_IP_KEY = "corant.system.ip";
  static final String IDGEN_SF_WK_ID = "identifier.generator.snowflake.worker-id";
  static final String IDGEN_SF_DC_ID = "identifier.generator.snowflake.datacenter-id";
  static final String IDGEN_SF_TIME = "identifier.generator.snowflake.use-persistence-timer";
  static Identifiers.IdentifierGenerator GENERATOR;
  static volatile boolean ENABLED = false;
  static volatile int DATA_CENTER_ID;
  static volatile int WORKER_ID;
  static volatile boolean usePersistenceTime =
      getConfig().getOptionalValue(IDGEN_SF_TIME, Boolean.class).orElse(true);
  static Map<Class<?>, TimerResolver> timeResolvers = new ConcurrentHashMap<>();// static?
  static String ipAddress = getConfig().getOptionalValue(SYS_IP_KEY, String.class).orElse(null);

  static {
    DATA_CENTER_ID = getConfig().getOptionalValue(IDGEN_SF_DC_ID, Integer.class).orElse(-1);
    WORKER_ID = getConfig().getOptionalValue(IDGEN_SF_WK_ID, Integer.class).orElseGet(() -> {
      if (isNotBlank(ipAddress)) {
        String[] segs = split(ipAddress, ".", true, true);
        if (segs.length > 0) {
          return toInteger(segs[segs.length - 1]);
        }
      }
      return 0;
    });
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
    return resolveTime(session, object);
  }

  long resolveTime(final SharedSessionContractImplementor session, final Object object) {
    return timeResolvers.computeIfAbsent(getUserClass(object.getClass()), (cls) -> {
      return (s, o) -> {
        MongoDBDatastoreProvider mp = resolveMongoDBDatastoreProvider(s);
        if (mp != null) {
          final Document timeBson =
              new Document(mapOf("serverStatus", 1, "repl", 0, "metrics", 0, "locks", 0));
          return getMapInstant(mp.getDatabase().runCommand(timeBson), "localTime").toEpochMilli();
        } else {
          final String timeSql = s.getFactory().getServiceRegistry().getService(JdbcServices.class)
              .getDialect().getCurrentTimestampSelectString();
          // return toObject(s.createNativeQuery(timeSql).getSingleResult(), Timestamp.class)
          // .getTime();
          // FIXME whether it is necessary? 2020-01-01
          final FlushMode hfm = s.getHibernateFlushMode();
          try {
            s.setHibernateFlushMode(FlushMode.MANUAL);
            return toObject(s.createNativeQuery(timeSql).getSingleResult(), Timestamp.class)
                .getTime();
          } catch (Exception e) {
            throw new CorantRuntimeException(e);
          } finally {
            s.setHibernateFlushMode(hfm);
          }
        }
      };
    }).resolve(session, object);
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

  @FunctionalInterface
  public interface TimerResolver {
    long resolve(SharedSessionContractImplementor session, Object object);
  }
}
