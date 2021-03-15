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
package org.corant.suites.jpa.hibernate.orm;

import static org.corant.shared.util.Classes.defaultClassLoader;
import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.corant.config.Configs;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.util.Identifiers.GeneralSnowflakeUUIDGenerator;
import org.corant.shared.util.Identifiers.SnowflakeD5W5S12UUIDGenerator;
import org.corant.shared.util.Identifiers.SnowflakeIpv4HostUUIDGenerator;
import org.corant.shared.util.Identifiers.SnowflakeW10S12UUIDGenerator;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

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
  public static final String IG_SF_DL_TM = "identifier.generator.snowflake.delayed-timing";
  public static final String IG_SF_UP_TM = "identifier.generator.snowflake.use-persistence-timer";

  static Logger logger = Logger.getLogger(HibernateSnowflakeIdGenerator.class.getName());
  static final GeneralSnowflakeUUIDGenerator generator;
  static final boolean enabled;
  static final int dataCenterId;
  static final int workerId;
  static final String ip = Configs.getValue(IG_SF_WK_IP, String.class);
  static final boolean usePst = Configs.getValue(IG_SF_UP_TM, Boolean.class, Boolean.TRUE);
  static final long delayedTiming = Configs.getValue(IG_SF_DL_TM, Long.class, 16000L);
  static final boolean useSec;
  static final HibernateSnowflakeIdTimeService specTimeGenerator;
  static Map<Class<?>, HibernateSessionTimeService> timeResolvers = new ConcurrentHashMap<>();
  static final List<HibernateSessionTimeService> sessionTimeServices;

  static {
    dataCenterId = getConfig().getOptionalValue(IG_SF_DC_ID, Integer.class).orElse(-1);
    workerId = getConfig().getOptionalValue(IG_SF_WK_ID, Integer.class).orElse(-1);
    if (workerId >= 0) {
      if (dataCenterId >= 0) {
        generator = new SnowflakeD5W5S12UUIDGenerator(dataCenterId, workerId, delayedTiming);
      } else {
        generator = new SnowflakeW10S12UUIDGenerator(workerId, delayedTiming);
      }
      useSec = false;
    } else if (isNotBlank(ip)) {
      generator = new SnowflakeIpv4HostUUIDGenerator(ip, delayedTiming);
      useSec = true;
    } else {
      generator = new SnowflakeIpv4HostUUIDGenerator(delayedTiming);
      useSec = true;
    }

    specTimeGenerator =
        ServiceLoader.load(HibernateSnowflakeIdTimeService.class, defaultClassLoader()).stream()
            .map(Provider::get).sorted(Sortable::compare).findFirst().orElse(
                (u, s, o) -> (u ? Instant.now().getEpochSecond() : Instant.now().toEpochMilli()));
    sessionTimeServices =
        ServiceLoader.load(HibernateSessionTimeService.class, defaultClassLoader()).stream()
            .map(Provider::get).sorted(Sortable::compare).collect(Collectors.toList());

    logger.info(() -> String.format("Use %s.", generator.description()));

    enabled = true;
  }

  @Override
  public Serializable generate(SharedSessionContractImplementor session, Object object)
      throws HibernateException {
    return generator.generate(() -> getTimeSeq(session, object));
  }

  long getTimeSeq(SharedSessionContractImplementor session, Object object) {
    if (!usePst) {
      return specTimeGenerator.fromEpoch(useSec, session, object);
    }
    return resolveTime(session, object);
  }

  HibernateSessionTimeService resolveSessionTimeService(
      final SharedSessionContractImplementor session, final Object object) {
    for (HibernateSessionTimeService s : sessionTimeServices) {
      if (s.accept(session, object)) {
        return s;
      }
    }
    return specTimeGenerator::fromEpoch;
  }

  long resolveTime(final SharedSessionContractImplementor session, final Object object) {
    return timeResolvers.computeIfAbsent(getUserClass(object.getClass()),
        cls -> resolveSessionTimeService(session, object)).resolve(useSec, session, object);
  }

}
