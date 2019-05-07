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
package org.corant.suites.jpa.shared.inject;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.CollectionUtils.setOf;
import static org.corant.shared.util.StringUtils.defaultBlank;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContextType;
import javax.transaction.TransactionScoped;
import org.corant.suites.jpa.shared.metadata.PersistenceContextMetaData;

/**
 * corant-suites-jpa-shared
 *
 * @author bingo 上午10:34:41
 *
 */
public class EntityManagerBean implements Bean<EntityManager>, PassivationCapable {

  static final Logger logger = Logger.getLogger(EntityManagerBean.class.getName());
  static final Set<Type> types = Collections.unmodifiableSet(setOf(EntityManager.class));
  final Set<Annotation> extenQualifiers = new HashSet<>();
  final Set<Annotation> transQualifiers = new HashSet<>();
  final BeanManager beanManager;
  final PersistenceContextMetaData persistenceContextMetaData;
  final String unitName;

  /**
   * @param beanManager
   * @param persistenceContextMetaData
   * @param qualifiers
   */
  public EntityManagerBean(BeanManager beanManager,
      PersistenceContextMetaData persistenceContextMetaData, Annotation... qualifiers) {
    super();
    this.beanManager = beanManager;
    this.persistenceContextMetaData = shouldNotNull(persistenceContextMetaData);
    unitName = persistenceContextMetaData.getUnitName();
    transQualifiers.addAll(setOf(qualifiers));
    transQualifiers.addAll(setOf(TransactionPersistenceContextType.INST, Any.Literal.INSTANCE));
    extenQualifiers.addAll(setOf(qualifiers));
    extenQualifiers.addAll(setOf(ExtendedPersistenceContextType.INST, Any.Literal.INSTANCE));
  }

  @Override
  public EntityManager create(CreationalContext<EntityManager> creationalContext) {
    final EntityManager em = ExtendedEntityManagerFactory.of(unitName).createEntityManager(
        persistenceContextMetaData.getSynchronization(),
        persistenceContextMetaData.getProperties());
    logger.fine(
        () -> String.format("Created an entity manager that persistence unit named %s, scope is %s",
            defaultBlank(unitName, "unnamed"), getScope().getSimpleName()));
    return em;
  }

  @Override
  public void destroy(EntityManager instance, CreationalContext<EntityManager> creationalContext) {
    if (instance != null && instance.isOpen()) {
      instance.close();
      logger.fine(
          () -> String.format("Destroyed entity manager that persistence unit named %s scope is %s",
              defaultBlank(unitName, "unnamed"), getScope().getSimpleName()));
    }
  }

  @Override
  public Class<?> getBeanClass() {
    return EntityManagerBean.class;
  }

  @Override
  public String getId() {
    return EntityManagerBean.class.getName() + "." + unitName;
  }

  @Override
  public Set<InjectionPoint> getInjectionPoints() {
    return Collections.emptySet();
  }

  @Override
  public String getName() {
    return "EntityManagerBean." + persistenceContextMetaData.getUnitName();
  }

  @Override
  public Set<Annotation> getQualifiers() {
    return persistenceContextMetaData.getType() == PersistenceContextType.EXTENDED ? extenQualifiers
        : transQualifiers;
  }

  @Override
  public Class<? extends Annotation> getScope() {
    return persistenceContextMetaData.getType() == PersistenceContextType.EXTENDED ? Dependent.class
        : TransactionScoped.class;
  }

  @Override
  public Set<Class<? extends Annotation>> getStereotypes() {
    return Collections.emptySet();
  }

  @Override
  public Set<Type> getTypes() {
    return types;
  }

  @Override
  public boolean isAlternative() {
    return false;
  }

  @Override
  public boolean isNullable() {
    return false;
  }

}
