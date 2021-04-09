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
package org.corant.modules.jpa.hibernate.orm;

import static org.corant.context.Instances.resolve;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Classes.defaultClassLoader;
import static org.corant.shared.util.Classes.tryAsClass;
import static org.corant.shared.util.Conversions.toBoolean;
import static org.corant.shared.util.Conversions.toInteger;
import static org.corant.shared.util.Conversions.toLong;
import static org.corant.shared.util.Maps.getMapString;
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
import org.corant.config.Configs;
import org.corant.context.qualifier.Qualifiers;
import org.corant.modules.jpa.shared.JPAExtension;
import org.corant.modules.jpa.shared.PersistenceService;
import org.corant.modules.jpa.shared.metadata.PersistenceUnitInfoMetaData;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Names;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.util.Identifiers.GeneralSnowflakeUUIDGenerator;
import org.corant.shared.util.Identifiers.SnowflakeD5W5S12UUIDGenerator;
import org.corant.shared.util.Identifiers.SnowflakeIpv4HostUUIDGenerator;
import org.corant.shared.util.Identifiers.SnowflakeW10S12UUIDGenerator;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

/**
 * corant-modules-jpa-hibernate-orm
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
  public static final String GL_IG_SF_WK_IP = Names.CORANT_PREFIX + IG_SF_WK_IP;
  public static final String GL_IG_SF_WK_ID = Names.CORANT_PREFIX + IG_SF_WK_ID;
  public static final String GL_IG_SF_DC_ID = Names.CORANT_PREFIX + IG_SF_DC_ID;
  public static final String GL_IG_SF_DL_TM = Names.CORANT_PREFIX + IG_SF_DL_TM;
  public static final String GL_IG_SF_UP_TM = Names.CORANT_PREFIX + IG_SF_UP_TM;

  static Logger logger = Logger.getLogger(HibernateSnowflakeIdGenerator.class.getName());

  static final HibernateSnowflakeIdTimeService specTimeGenerator =
      ServiceLoader.load(HibernateSnowflakeIdTimeService.class, defaultClassLoader()).stream()
          .map(Provider::get).sorted(Sortable::compare).findFirst()
          .orElse((u, s, o) -> (u ? Instant.now().getEpochSecond() : Instant.now().toEpochMilli()));
  static final List<HibernateSessionTimeService> sessionTimeServices =
      ServiceLoader.load(HibernateSessionTimeService.class, defaultClassLoader()).stream()
          .map(Provider::get).sorted(Sortable::compare).collect(Collectors.toList());

  static Map<String, Generator> generators = new ConcurrentHashMap<>();

  /**
   * Returns the generated long type id manually.
   *
   * @param ptu the persistence unit name, use to identify the generator configuration.
   */
  public static long generateManually(String ptu) {
    String usePtu = Qualifiers.resolveName(ptu);
    final Generator generator = getGenerator(usePtu);
    if (generator.usePersistenceTimer) {
      return generator
          .generate(shouldNotNull(resolve(PersistenceService.class).getEntityManagerFactory(usePtu))
              .unwrap(SessionFactoryImplementor.class), null);
    } else {
      return generator.generate(null, null);
    }
  }

  /**
   * Parse instant from given id and persistence unit name
   *
   * @param ptu the persistence unit name, use to identify the generator configuration.
   * @param id the id that will be parse
   * @return the time sequence
   */
  public static Instant parseGeneratedInstant(String ptu, long id) {
    return getGenerator(ptu).snowflakeGenerator.parseGeneratedInstant(id);
  }

  /**
   * Parse sequence from given id and persistence unit name
   *
   * @param ptu the persistence unit name, use to identify the generator configuration.
   * @param id the id that will be parse
   * @return the sequence
   */
  public static long parseGeneratedSequence(String ptu, long id) {
    return getGenerator(ptu).snowflakeGenerator.parseGeneratedSequence(id);
  }

  /**
   * Parse workers id from given id and persistence unit name
   *
   * @param ptu the persistence unit name, use to identify the generator configuration.
   * @param id the id that will be parse
   * @return the workers id
   */
  public static long parseGeneratedWorkersId(String ptu, long id) {
    return getGenerator(ptu).snowflakeGenerator.parseGeneratedWorkersId(id);
  }

  /**
   * Clear the generators
   */
  public static void reset() {
    generators.clear();
  }

  static Generator createGenerator(String ptu) {
    PersistenceUnitInfoMetaData metaData =
        resolve(JPAExtension.class).getPersistenceUnitInfoMetaDatas().values().stream()
            .filter(p -> areEqual(p.getPersistenceUnitName(), ptu)).findFirst().orElseThrow(
                () -> new CorantRuntimeException("Can't find persistence unit %s for id generator!",
                    ptu));

    final GeneralSnowflakeUUIDGenerator generator;

    int dataCenterId = toInteger(metaData.getProperties().getOrDefault(IG_SF_DC_ID,
        Configs.getValue(GL_IG_SF_DC_ID, Integer.class, -1)));
    int workerId = toInteger(metaData.getProperties().getOrDefault(IG_SF_WK_ID,
        Configs.getValue(GL_IG_SF_WK_ID, Integer.class, -1)));
    String ip = asString(metaData.getProperties().get(IG_SF_WK_IP),
        Configs.getValue(GL_IG_SF_WK_IP, String.class));
    boolean usePst = toBoolean(metaData.getProperties().getOrDefault(IG_SF_UP_TM,
        Configs.getValue(GL_IG_SF_UP_TM, String.class, "false")));
    long delayedTiming = toLong(metaData.getProperties().getOrDefault(IG_SF_DL_TM,
        Configs.getValue(GL_IG_SF_DL_TM, Long.class, 16000L)));

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

  static Generator getGenerator(String ptu) {
    return generators.computeIfAbsent(Qualifiers.resolveName(ptu),
        HibernateSnowflakeIdGenerator::createGenerator);
  }

  @Override
  public Serializable generate(SharedSessionContractImplementor session, Object object)
      throws HibernateException {
    final SessionFactoryImplementor sessionFactory = session.getFactory();
    final String ptu = getMapString(sessionFactory.getProperties(),
        org.hibernate.jpa.AvailableSettings.ENTITY_MANAGER_FACTORY_NAME);
    return getGenerator(ptu).generate(sessionFactory, object);
  }

  /**
   * corant-modules-jpa-hibernate-orm
   *
   * @author bingo 下午4:14:47
   *
   */
  public static class Generator {
    final GeneralSnowflakeUUIDGenerator snowflakeGenerator;
    final boolean useSecond;
    final HibernateSessionTimeService timeService;
    final boolean usePersistenceTimer;

    public Generator(final Class<?> providerClass, GeneralSnowflakeUUIDGenerator snowflakeGenerator,
        boolean usePst) {
      this.snowflakeGenerator = snowflakeGenerator;
      useSecond = snowflakeGenerator.getUnit() == ChronoUnit.SECONDS;
      usePersistenceTimer = usePst;
      if (!usePersistenceTimer) {
        timeService = (u, s, o) -> specTimeGenerator.fromEpoch(u, s, o);
      } else {
        timeService = sessionTimeServices.stream().filter(s -> s.accept(providerClass)).findFirst()
            .orElse((u, s, o) -> specTimeGenerator.fromEpoch(u, s, o));
      }
    }

    public long generate(SessionFactoryImplementor sessionFactory, Object object) {
      return snowflakeGenerator.generate(() -> timeService.get(useSecond, sessionFactory, object));
    }

  }

}
