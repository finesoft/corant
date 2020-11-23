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

import static org.corant.shared.util.Classes.defaultClassLoader;
import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Maps.getMapInstant;
import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bson.Document;
import org.corant.config.Configs;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Identifiers;
import org.corant.shared.util.Identifiers.SnowflakeD5W5S12UUIDGenerator;
import org.corant.shared.util.Identifiers.SnowflakeIpv4HostUUIDGenerator;
import org.corant.shared.util.Identifiers.SnowflakeW10S12UUIDGenerator;
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

  public static final String IG_SF_WK_IP = "identifier.generator.snowflake.worker-ip";
  public static final String IG_SF_WK_ID = "identifier.generator.snowflake.worker-id";
  public static final String IG_SF_DC_ID = "identifier.generator.snowflake.datacenter-id";
  public static final String IG_SF_TIME = "identifier.generator.snowflake.use-persistence-timer";

  static Logger logger = Logger.getLogger(HibernateSnowflakeIdGenerator.class.getName());
  static final Identifiers.IdentifierGenerator generator;
  static final boolean enabled;
  static final int dataCenterId;
  static final int workerId;
  static final String ip = Configs.getValue(IG_SF_WK_IP, String.class);
  static final boolean usePst = Configs.getValue(IG_SF_TIME, Boolean.class, Boolean.TRUE);
  static final boolean useSec;
  static final HibernateSnowflakeIdTimeGenerator specTimeGenerator;
  static Map<Class<?>, TimerResolver> timeResolvers = new ConcurrentHashMap<>();

  static {
    dataCenterId = getConfig().getOptionalValue(IG_SF_DC_ID, Integer.class).orElse(-1);
    workerId = getConfig().getOptionalValue(IG_SF_WK_ID, Integer.class).orElse(-1);
    if (workerId >= 0) {
      if (dataCenterId >= 0) {
        generator = new SnowflakeD5W5S12UUIDGenerator(dataCenterId, workerId);
        logger.info(() -> String.format(
            "Use SnowflakeD5W5S12UUIDGenerator data center id is %s, worker id is %s.",
            dataCenterId, workerId));
      } else {
        generator = new SnowflakeW10S12UUIDGenerator(workerId, true);
        logger.info(
            () -> String.format("Use SnowflakeW10S12UUIDGenerator worker id is %s.", workerId));
      }
      useSec = false;
    } else if (isNotBlank(ip)) {
      generator = new SnowflakeIpv4HostUUIDGenerator(ip, 16L);
      useSec = true;
      logger.info(() -> String.format("Use SnowflakeIpv4HostUUIDGenerator ip is %s.", ip));
    } else {
      generator = new SnowflakeIpv4HostUUIDGenerator(16L);
      useSec = true;
      logger.info(() -> "Use SnowflakeIpv4HostUUIDGenerator and localhost.");
    }

    specTimeGenerator =
        ServiceLoader.load(HibernateSnowflakeIdTimeGenerator.class, defaultClassLoader())
            .findFirst().orElse(s -> {
              long currentMills = System.currentTimeMillis();
              return s ? currentMills / 1000L + 1 : currentMills;
            });

    enabled = true;
  }

  @Override
  public Serializable generate(SharedSessionContractImplementor session, Object object)
      throws HibernateException {
    return generator.generate(() -> getTimeSeq(session, object));
  }

  long getTimeSeq(SharedSessionContractImplementor session, Object object) {
    if (!usePst) {
      return specTimeGenerator.fromEpoch(useSec);
    }
    return resolveTime(session, object);
  }

  long resolveTime(final SharedSessionContractImplementor session, final Object object) {
    return timeResolvers.computeIfAbsent(getUserClass(object.getClass()), cls -> (s, o) -> {
      MongoDBDatastoreProvider mp = resolveMongoDBDatastoreProvider(s);
      if (mp != null) {
        final Document timeBson =
            new Document(mapOf("serverStatus", 1, "repl", 0, "metrics", 0, "locks", 0));
        final long epochMillis =
            getMapInstant(mp.getDatabase().runCommand(timeBson), "localTime").toEpochMilli();
        return useSec ? epochMillis / 1000L + 1 : epochMillis;
      } else {
        final String timeSql = s.getFactory().getServiceRegistry().getService(JdbcServices.class)
            .getDialect().getCurrentTimestampSelectString();
        final FlushMode hfm = s.getHibernateFlushMode();
        try {
          s.setHibernateFlushMode(FlushMode.MANUAL);
          final long epochMillis =
              toObject(s.createNativeQuery(timeSql).getSingleResult(), Timestamp.class).getTime();
          return useSec ? epochMillis / 1000L + 1 : epochMillis;
        } catch (Exception e) {
          throw new CorantRuntimeException(e);
        } finally {
          s.setHibernateFlushMode(hfm);
        }
      }
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
