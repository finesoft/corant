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
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.ClassUtils.tryAsClass;
import static org.corant.shared.util.CollectionUtils.asSet;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.persistence.EntityManagerFactory;
import org.corant.Corant;
import org.corant.suites.jpa.shared.AbstractJpaProvider;
import org.corant.suites.jpa.shared.metadata.PersistenceUnitInfoMetaData;

/**
 * corant-suites-jpa-shared
 *
 * @author bingo 上午10:34:41
 *
 */
public class EntityManagerFactoryBean implements Bean<EntityManagerFactory>, PassivationCapable {

  static final Logger LOGGER = Logger.getLogger(EntityManagerFactoryBean.class.getName());
  static final Set<Annotation> QUALIFIERS =
      Collections.unmodifiableSet(asSet(Default.Literal.INSTANCE));
  static final Set<Type> TYPES = Collections.unmodifiableSet(asSet(EntityManagerFactory.class));
  final BeanManager beanManager;
  final PersistenceUnitInfoMetaData persistenceUnitInfoMetaData;

  /**
   * @param beanManager
   * @param persistenceContext
   */
  public EntityManagerFactoryBean(BeanManager beanManager,
      PersistenceUnitInfoMetaData persistenceUnitInfoMetaData) {
    super();
    this.beanManager = beanManager;
    this.persistenceUnitInfoMetaData = persistenceUnitInfoMetaData;
  }

  @Override
  public EntityManagerFactory create(CreationalContext<EntityManagerFactory> creationalContext) {
    NamedLiteral proNme =
        NamedLiteral.of(persistenceUnitInfoMetaData.getPersistenceProviderClassName());
    Instance<AbstractJpaProvider> provider =
        Corant.instance().select(AbstractJpaProvider.class, proNme);
    shouldBeTrue(provider.isResolvable(), "Can not find jpa provider named %s.", proNme.value());
    final EntityManagerFactory emf =
        shouldNotNull(provider.get().getEntityManagerFactory(persistenceUnitInfoMetaData));
    LOGGER.info(() -> String.format(
        "Created an entity manager factory that persistence unit named %s, provider is %s.",
        persistenceUnitInfoMetaData.getPersistenceUnitName(),
        tryAsClass(proNme.value()).getSimpleName()));
    return emf;
  }

  @Override
  public void destroy(EntityManagerFactory instance,
      CreationalContext<EntityManagerFactory> creationalContext) {
    if (instance != null && instance.isOpen()) {
      instance.close();
      LOGGER.info(
          () -> String.format("Destroyed entity manager factory that persistence unit named %s.",
              persistenceUnitInfoMetaData.getPersistenceUnitName()));
    }
  }

  @Override
  public Class<?> getBeanClass() {
    return EntityManagerFactoryBean.class;
  }

  @Override
  public String getId() {
    return EntityManagerFactoryBean.class.getName();
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
    return ApplicationScoped.class;
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
