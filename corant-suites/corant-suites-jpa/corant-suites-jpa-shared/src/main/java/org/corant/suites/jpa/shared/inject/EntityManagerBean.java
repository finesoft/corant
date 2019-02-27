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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.CollectionUtils.asSet;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
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
import org.corant.Corant;
import org.corant.suites.jpa.shared.AbstractJpaProvider;
import org.corant.suites.jpa.shared.metadata.PersistenceContextInfoMetaData;

/**
 * corant-suites-jpa-shared
 *
 * @author bingo 上午10:34:41
 *
 */
public class EntityManagerBean implements Bean<EntityManager>, PassivationCapable {

  static final Logger LOGGER = Logger.getLogger(EntityManagerBean.class.getName());
  static final Set<Annotation> TRANS_QUALIFIERS = Collections
      .unmodifiableSet(asSet(TransactionPersistenceContextType.INST, Any.Literal.INSTANCE));
  static final Set<Annotation> EXTEN_QUALIFIERS =
      Collections.unmodifiableSet(asSet(ExtendedPersistenceContextType.INST, Any.Literal.INSTANCE));
  static final Set<Type> TYPES = Collections.unmodifiableSet(asSet(EntityManager.class));
  final BeanManager beanManager;
  final PersistenceContextInfoMetaData persistenceContextInfoMetaData;

  /**
   * @param beanManager
   * @param persistenceContext
   */
  public EntityManagerBean(BeanManager beanManager,
      PersistenceContextInfoMetaData persistenceContextInfoMetaData) {
    super();
    this.beanManager = beanManager;
    this.persistenceContextInfoMetaData = persistenceContextInfoMetaData;
  }

  @Override
  public EntityManager create(CreationalContext<EntityManager> creationalContext) {
    shouldBeTrue(Corant.instance().select(AbstractJpaProvider.class).isResolvable());
    AbstractJpaProvider provider = Corant.instance().select(AbstractJpaProvider.class).get();
    final EntityManager em = provider.getEntityManager(persistenceContextInfoMetaData);
    LOGGER.fine(
        () -> String.format("Created an entity manager that persistence unit named %s, scope is %s",
            persistenceContextInfoMetaData.getUnit().getPersistenceUnitName(),
            getScope().getSimpleName()));
    return em;
  }

  @Override
  public void destroy(EntityManager instance, CreationalContext<EntityManager> creationalContext) {
    if (instance != null && instance.isOpen()) {
      instance.close();
      LOGGER.fine(
          () -> String.format("Destroyed entity manager that persistence unit named %s scope is %s",
              persistenceContextInfoMetaData.getUnit().getPersistenceUnitName(),
              getScope().getSimpleName()));
    }
  }

  @Override
  public Class<?> getBeanClass() {
    return EntityManagerBean.class;
  }

  @Override
  public String getId() {
    return EntityManagerBean.class.getName() + "."
        + persistenceContextInfoMetaData.getUnit().getPersistenceUnitName();
  }

  @Override
  public Set<InjectionPoint> getInjectionPoints() {
    return Collections.emptySet();
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public Set<Annotation> getQualifiers() {
    return persistenceContextInfoMetaData.getType() == PersistenceContextType.EXTENDED
        ? EXTEN_QUALIFIERS
        : TRANS_QUALIFIERS;
  }

  @Override
  public Class<? extends Annotation> getScope() {
    return persistenceContextInfoMetaData.getType() == PersistenceContextType.EXTENDED
        ? Dependent.class
        : TransactionScoped.class;
  }

  @Override
  public Set<Class<? extends Annotation>> getStereotypes() {
    return Collections.emptySet();
  }

  @Override
  public Set<Type> getTypes() {
    return TYPES;
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
