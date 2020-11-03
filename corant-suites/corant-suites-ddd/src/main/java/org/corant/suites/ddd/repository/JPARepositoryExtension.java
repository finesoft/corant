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
package org.corant.suites.ddd.repository;

import static org.corant.context.Instances.find;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Sets.setOf;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import org.corant.context.Qualifiers;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.ddd.annotation.stereotype.Repositories;
import org.corant.suites.ddd.model.AggregateLifecycleManager;
import org.corant.suites.ddd.unitwork.AbstractJPAUnitOfWorksManager;
import org.corant.suites.ddd.unitwork.AbstractJTAJPAUnitOfWorksManager;
import org.corant.suites.ddd.unitwork.UnitOfWorks;
import org.corant.suites.jpa.shared.JPAExtension;
import org.corant.suites.jpa.shared.PersistenceService.PersistenceContextLiteral;

/**
 * corant-suites-ddd
 *
 * @author bingo 下午7:09:18
 *
 */
public class JPARepositoryExtension implements Extension {

  static final Map<String, Annotation[]> qualifiers = new HashMap<>();

  public static Annotation[] resolveQualifiers(Class<?> cls) {
    return qualifiers.get(find(AggregateLifecycleManager.class)
        .orElseThrow(() -> new CorantRuntimeException("Can't find entity lifecycle manager!"))
        .getPersistenceContext(cls).unitName());
  }

  protected void onBeforeShutdown(@Observes @Priority(0) BeforeShutdown bs) {
    qualifiers.clear();
  }

  void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery abd, final BeanManager beanManager) {
    Set<String> names =
        beanManager.getExtension(JPAExtension.class).getPersistenceUnitInfoMetaDatas().keySet()
            .stream().map(PersistenceUnit::unitName).collect(Collectors.toSet());
    qualifiers.clear();
    qualifiers.putAll(Qualifiers.resolveNameds(names));
    qualifiers.forEach((k, v) -> abd.<JPARepository>addBean().addQualifiers(v)
        .addTransitiveTypeClosure(DefaultJPARepository.class).beanClass(DefaultJPARepository.class)
        .scope(ApplicationScoped.class).stereotypes(setOf(Repositories.class))
        .produceWith(beans -> produce(beans, k)).disposeWith((repo, beans) -> {
        }));
  }

  JPARepository produce(Instance<Object> instances, String unitName) {
    Optional<AbstractJTAJPAUnitOfWorksManager> uowm =
        instances.select(UnitOfWorks.class).get().currentDefaultUnitOfWorksManager();
    shouldBeTrue(uowm.isPresent());
    return new DefaultJPARepository(PersistenceContextLiteral.of(unitName), uowm.get());
  }

  /**
   * corant-suites-ddd
   *
   * @author bingo 下午5:06:40
   *
   */
  public static final class DefaultJPARepository extends AbstractJPARepository {

    protected DefaultJPARepository() {}

    protected DefaultJPARepository(PersistenceContext pc, AbstractJPAUnitOfWorksManager uofm) {
      super();
      persistenceContext = pc;
      unitOfWorkManager = uofm;
    }
  }
}
