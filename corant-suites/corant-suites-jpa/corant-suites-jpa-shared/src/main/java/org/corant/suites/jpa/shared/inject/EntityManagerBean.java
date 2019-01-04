/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.suites.jpa.shared.inject;

import static org.corant.shared.util.CollectionUtils.asSet;
import static org.corant.shared.util.ObjectUtils.shouldBeTrue;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContextType;
import javax.transaction.TransactionScoped;
import org.corant.Corant;
import org.corant.suites.jpa.shared.AbstractJpaProvider;
import org.corant.suites.jpa.shared.metadata.PersistenceContextMetaData;

/**
 * corant-suites-jpa-shared
 *
 * @author bingo 上午10:34:41
 *
 */
public class EntityManagerBean implements Bean<EntityManager>, PassivationCapable {

  final static Set<Annotation> QUALIFIERS =
      Collections.unmodifiableSet(asSet(Default.Literal.INSTANCE));
  final Set<Type> TYPES = Collections.unmodifiableSet(asSet(EntityManager.class));
  final BeanManager beanManager;
  final PersistenceContextMetaData persistenceContextMetaData;

  /**
   * @param beanManager
   * @param persistenceContext
   */
  public EntityManagerBean(BeanManager beanManager,
      PersistenceContextMetaData persistenceContextMetaData) {
    super();
    this.beanManager = beanManager;
    this.persistenceContextMetaData = persistenceContextMetaData;
  }

  @Override
  public EntityManager create(CreationalContext<EntityManager> creationalContext) {
    shouldBeTrue(Corant.cdi().select(AbstractJpaProvider.class).isResolvable());
    AbstractJpaProvider provider = Corant.cdi().select(AbstractJpaProvider.class).get();
    return provider.getEntityManager(persistenceContextMetaData);
  }

  @Override
  public void destroy(EntityManager instance, CreationalContext<EntityManager> creationalContext) {
    if (instance != null && instance.isOpen() && getScope().equals(TransactionScoped.class)) {
      instance.flush();
      instance.clear();
      instance.close();
    }
  }

  @Override
  public Class<?> getBeanClass() {
    return EntityManagerBean.class;
  }

  @Override
  public String getId() {
    return EntityManagerBean.class.getName();
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
    return QUALIFIERS;
  }

  @Override
  public Class<? extends Annotation> getScope() {
    return persistenceContextMetaData.getType() == PersistenceContextType.TRANSACTION
        ? TransactionScoped.class
        : Dependent.class;
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
