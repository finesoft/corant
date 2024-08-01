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
package org.corant.modules.jpa.shared;

import static org.corant.shared.util.Annotations.calculateMembersHashCode;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Strings.defaultString;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;
import jakarta.persistence.PersistenceProperty;
import jakarta.persistence.PersistenceUnit;
import jakarta.persistence.SynchronizationType;
import org.corant.context.qualifier.Qualifiers;
import org.corant.shared.ubiquity.Tuple.Pair;

/**
 * corant-modules-jpa-shared
 *
 * @author bingo 下午2:08:53
 */
public interface PersistenceService {

  String EMPTY_PERSISTENCE_UNIT_NAME = Qualifiers.EMPTY_NAME;

  /**
   * Returns a new not managed entity manager for given persistence context
   *
   * @param pc persistence context use to retrieve the entity manager from the managed entity
   *        manager factory
   * @return a not managed entity manager
   */
  EntityManager createEntityManager(PersistenceContext pc);

  /**
   * Returns the managed entity manager for given persistence context
   *
   * @param pc persistence context use to retrieve the entity manager from entity manager factory
   * @return managed entity manager
   */
  EntityManager getEntityManager(PersistenceContext pc);

  /**
   * Returns the managed entity manager for given persistence unit name, the implicit type of the
   * persistence context is TRANSACTION and synchronization of the persistence context is
   * SYNCHRONIZED
   *
   * @param persistenceUnitName the persistence unit name
   */
  default EntityManager getEntityManager(String persistenceUnitName) {
    return getEntityManager(PersistenceContextLiteral.of(persistenceUnitName));
  }

  /**
   * Returns the managed entity manager factory for given persistence context
   *
   * @param pc persistence context use to retrieve the entity manager factory by
   *        {@link PersistenceContext#unitName()}
   * @return managed entity manager factory
   */
  default EntityManagerFactory getEntityManagerFactory(PersistenceContext pc) {
    return getEntityManagerFactory(PersistenceUnitLiteral.of(pc));
  }

  /**
   * Returns the managed entity manager factory for given persistence unit
   *
   * @param pu persistence unit use to retrieve the entity manager factory
   * @return managed entity manager factory
   */
  EntityManagerFactory getEntityManagerFactory(PersistenceUnit pu);

  /**
   * Returns the managed entity manager factory for given persistence unit name
   *
   * @param persistenceUnitName persistence unit name use to retrieve the entity manager factory
   * @return managed entity manager factory
   */
  default EntityManagerFactory getEntityManagerFactory(String persistenceUnitName) {
    return getEntityManagerFactory(PersistenceUnitLiteral.of(persistenceUnitName));
  }

  /**
   * corant-modules-jpa-shared
   *
   * @author bingo 下午12:01:07
   */
  class PersistenceContextLiteral extends AnnotationLiteral<PersistenceContext>
      implements PersistenceContext {

    private static final long serialVersionUID = -6911793060874174440L;

    private String name;
    private String unitName;
    private PersistenceContextType type = PersistenceContextType.TRANSACTION;
    private SynchronizationType synchronization = SynchronizationType.SYNCHRONIZED;
    private PersistenceProperty[] properties = {};
    private transient volatile Integer hashCode;

    public PersistenceContextLiteral() {
      this(null, null, null, null, (PersistenceProperty[]) null);
    }

    public PersistenceContextLiteral(String name, String unitName, PersistenceContextType type,
        SynchronizationType synchronization, PersistenceProperty[] properties) {
      this.name = defaultString(name);
      this.unitName = defaultString(unitName);
      if (type != null) {
        this.type = type;
      }
      if (synchronization != null) {
        this.synchronization = synchronization;
      }
      if (properties != null) {
        this.properties = Arrays.copyOf(properties, properties.length);
      }
    }

    protected PersistenceContextLiteral(String name, String unitName, PersistenceContextType type,
        SynchronizationType synchronization, Map<String, String> properties) {
      this(name, unitName, type, synchronization,
          properties == null ? null
              : properties.entrySet().stream().map(PersistencePropertyLiteral::new)
                  .toArray(PersistencePropertyLiteral[]::new));
    }

    public static Map<String, String> extractProperties(PersistenceProperty[] pps) {
      Map<String, String> map = new LinkedHashMap<>();
      if (!isEmpty(pps)) {
        for (PersistenceProperty pp : pps) {
          map.put(pp.name(), pp.value());
        }
      }
      return map;
    }

    public static PersistenceContextLiteral of(PersistenceContext pc) {
      shouldNotNull(pc);
      return new PersistenceContextLiteral(pc.name(), pc.unitName(), pc.type(),
          pc.synchronization(), pc.properties());
    }

    public static PersistenceContextLiteral of(String unitName) {
      return of(unitName, null, null, (String[]) null);
    }

    public static PersistenceContextLiteral of(String unitName, PersistenceContextType type) {
      return of(unitName, type, null, (String[]) null);
    }

    public static PersistenceContextLiteral of(String unitName, PersistenceContextType type,
        SynchronizationType synchronization) {
      return of(unitName, type, synchronization, (String[]) null);
    }

    public static PersistenceContextLiteral of(String unitName, PersistenceContextType type,
        SynchronizationType synchronization, String... properties) {
      return of(null, unitName, type, synchronization, properties);
    }

    public static PersistenceContextLiteral of(String name, String unitName,
        PersistenceContextType type, SynchronizationType synchronization, String... properties) {
      return new PersistenceContextLiteral(name, unitName, type, synchronization,
          mapOf((Object[]) properties));
    }

    public static PersistenceContextLiteral of(String unitName,
        SynchronizationType synchronization) {
      return of(unitName, null, synchronization, (String[]) null);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || !PersistenceContext.class.isAssignableFrom(obj.getClass())) {
        return false;
      }
      PersistenceContext other = (PersistenceContext) obj;
      return name.equals(other.name()) && unitName.equals(other.unitName())
          && type.equals(other.type()) && synchronization.equals(other.synchronization())
          && Arrays.equals(properties, other.properties());
    }

    @Override
    public int hashCode() {
      if (hashCode == null) {
        hashCode = calculateMembersHashCode(Pair.of("name", name), Pair.of("unitName", unitName),
            Pair.of("type", type), Pair.of("synchronization", synchronization),
            Pair.of("properties", properties));
      }
      return hashCode;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public PersistenceProperty[] properties() {
      if (properties.length > 0) {
        return Arrays.copyOf(properties, properties.length);
      }
      return properties;
    }

    @Override
    public SynchronizationType synchronization() {
      return synchronization;
    }

    @Override
    public PersistenceContextType type() {
      return type;
    }

    @Override
    public String unitName() {
      return unitName;
    }

  }

  /**
   * corant-modules-jpa-shared
   *
   * @author bingo 上午11:50:24
   */
  class PersistencePropertyLiteral extends AnnotationLiteral<PersistenceProperty>
      implements PersistenceProperty {
    private static final long serialVersionUID = -5166046527595649735L;

    public static final PersistenceProperty[] EMPTY_ARRAY = {};

    private String name;
    private String value;
    private transient volatile Integer hashCode;

    public PersistencePropertyLiteral(Entry<String, String> entry) {
      this(entry.getKey(), entry.getValue());
    }

    public PersistencePropertyLiteral(String name, String value) {
      this.name = shouldNotNull(name);
      this.value = shouldNotNull(value);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || !PersistenceProperty.class.isAssignableFrom(obj.getClass())) {
        return false;
      }
      PersistenceProperty other = (PersistenceProperty) obj;
      return name.equals(other.name()) && value.equals(other.value());
    }

    @Override
    public int hashCode() {
      if (hashCode == null) {
        hashCode = calculateMembersHashCode(Pair.of("name", name), Pair.of("value", value));
      }
      return hashCode;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public String value() {
      return value;
    }

  }

  /**
   * corant-modules-jpa-shared
   *
   * @author bingo 下午12:04:52
   */
  class PersistenceUnitLiteral extends AnnotationLiteral<PersistenceUnit>
      implements PersistenceUnit {
    private static final long serialVersionUID = -2508891695595998643L;
    private String name;
    private String unitName;
    private transient volatile Integer hashCode;

    public PersistenceUnitLiteral() {
      this(null, null);
    }

    protected PersistenceUnitLiteral(String name, String unitName) {
      this.name = defaultString(name);
      this.unitName = defaultString(unitName);
    }

    public static PersistenceUnitLiteral of(PersistenceContext pc) {
      shouldNotNull(pc);
      return new PersistenceUnitLiteral(pc.name(), pc.unitName());
    }

    public static PersistenceUnitLiteral of(PersistenceUnit pu) {
      shouldNotNull(pu);
      return new PersistenceUnitLiteral(pu.name(), pu.unitName());
    }

    public static PersistenceUnitLiteral of(String unitName) {
      return new PersistenceUnitLiteral(null, unitName);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || !PersistenceUnit.class.isAssignableFrom(obj.getClass())) {
        return false;
      }
      PersistenceUnit other = (PersistenceUnit) obj;
      return name.equals(other.name()) && unitName.equals(other.unitName());
    }

    @Override
    public int hashCode() {
      if (hashCode == null) {
        hashCode = calculateMembersHashCode(Pair.of("name", name), Pair.of("unitName", unitName));
      }
      return hashCode;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public String unitName() {
      return unitName;
    }

  }
}
