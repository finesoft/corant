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

import static org.corant.context.Instances.resolve;
import static org.corant.shared.util.Classes.defaultClassLoader;
import static org.corant.shared.util.Classes.tryAsClass;
import static org.corant.shared.util.Conversions.toBoolean;
import static org.corant.shared.util.Conversions.toInteger;
import static org.corant.shared.util.Conversions.toLong;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Objects.asString;
import static org.corant.shared.util.Strings.isNotBlank;
import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.util.Identifiers.GeneralSnowflakeUUIDGenerator;
import org.corant.shared.util.Identifiers.SnowflakeD5W5S12UUIDGenerator;
import org.corant.shared.util.Identifiers.SnowflakeIpv4HostUUIDGenerator;
import org.corant.shared.util.Identifiers.SnowflakeW10S12UUIDGenerator;
import org.corant.suites.jpa.shared.JPAExtension;
import org.corant.suites.jpa.shared.metadata.PersistenceUnitInfoMetaData;
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

  static final HibernateSnowflakeIdTimeService specTimeGenerator =
      ServiceLoader.load(HibernateSnowflakeIdTimeService.class, defaultClassLoader()).stream()
          .map(Provider::get).sorted(Sortable::compare).findFirst()
          .orElse((u, s, o) -> (u ? Instant.now().getEpochSecond() : Instant.now().toEpochMilli()));
  static final List<HibernateSessionTimeService> sessionTimeServices =
      ServiceLoader.load(HibernateSessionTimeService.class, defaultClassLoader()).stream()
          .map(Provider::get).sorted(Sortable::compare).collect(Collectors.toList());

  static Map<String, Generator> generators = new ConcurrentHashMap<>();

  @Override
  public Serializable generate(SharedSessionContractImplementor session, Object object)
      throws HibernateException {
    String ptu = asString(session.getFactory().getProperties()
        .get(org.hibernate.jpa.AvailableSettings.ENTITY_MANAGER_FACTORY_NAME));
    return generators.computeIfAbsent(ptu, this::createGenerator).generate(session, object);
  }

  protected Generator createGenerator(String ptu) {
    PersistenceUnitInfoMetaData metaData =
        resolve(JPAExtension.class).getPersistenceUnitInfoMetaDatas().values().stream()
            .filter(p -> areEqual(p.getPersistenceUnitName(), ptu)).findFirst().get();
    final GeneralSnowflakeUUIDGenerator generator;
    int dataCenterId = toInteger(metaData.getProperties().getOrDefault(IG_SF_DC_ID, -1));
    int workerId = toInteger(metaData.getProperties().getOrDefault(IG_SF_WK_ID, -1));
    String ip = asString(metaData.getProperties().get(IG_SF_WK_IP), null);
    boolean usePst = toBoolean(metaData.getProperties().getOrDefault(IG_SF_DC_ID, "true"));
    long delayedTiming = toLong(metaData.getProperties().getOrDefault(IG_SF_DL_TM, 16000L));
    if (workerId >= 0) {
      if (dataCenterId >= 0) {
        generator = new SnowflakeD5W5S12UUIDGenerator(dataCenterId, workerId, delayedTiming);
      } else {
        generator = new SnowflakeW10S12UUIDGenerator(workerId, delayedTiming);
      }
    } else if (isNotBlank(ip)) {
      generator = new SnowflakeIpv4HostUUIDGenerator(ip, delayedTiming);
    } else {
      generator = new SnowflakeIpv4HostUUIDGenerator(delayedTiming);
    }
    logger.info(() -> String.format(
        "Create identifier generator for persistence unit[%s], the generator is %s.", ptu,
        generator.description()));
    return new Generator(tryAsClass(metaData.getPersistenceProviderClassName()), generator, usePst);
  }

  /**
   * corant-suites-jpa-hibernate-orm
   *
   * @author bingo 下午4:14:47
   *
   */
  public static class Generator {
    final GeneralSnowflakeUUIDGenerator snowflakeGenerator;
    final boolean usePst;
    final Class<?> providerClass;
    final boolean useSecond;
    final HibernateSessionTimeService timeService;

    public Generator(final Class<?> providerClass, GeneralSnowflakeUUIDGenerator snowflakeGenerator,
        boolean usePst) {
      this.providerClass = providerClass;
      this.snowflakeGenerator = snowflakeGenerator;
      this.usePst = usePst;
      useSecond = snowflakeGenerator.getUnit() == ChronoUnit.SECONDS;
      if (!usePst) {
        timeService = (u, s, o) -> specTimeGenerator.fromEpoch(u, s, o);
      } else {
        timeService = sessionTimeServices.stream().filter(s -> s.accept(providerClass)).findFirst()
            .orElse((u, s, o) -> specTimeGenerator.fromEpoch(u, s, o));
      }
    }

    public long generate(SharedSessionContractImplementor session, Object object) {
      return snowflakeGenerator.generate(() -> timeService.resolve(useSecond, session, object));
    }
  }

}
