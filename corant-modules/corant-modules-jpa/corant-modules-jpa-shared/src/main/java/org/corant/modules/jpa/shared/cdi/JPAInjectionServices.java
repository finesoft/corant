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
package org.corant.modules.jpa.shared.cdi;

import static org.corant.context.Beans.resolveApply;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import org.corant.context.CDIs;
import org.corant.context.ResourceReferences;
import org.corant.modules.jpa.shared.ExtendedEntityManager;
import org.corant.modules.jpa.shared.JPAProvider;
import org.corant.modules.jpa.shared.PersistenceService;
import org.jboss.weld.injection.spi.JpaInjectionServices;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

/**
 * corant-modules-jpa-shared
 * <p>
 * The JPAInjectionServices SPI implementation
 * </p>
 *
 * @see JPAProvider
 * @author bingo 下午4:18:45
 */
public class JPAInjectionServices implements JpaInjectionServices {

  @Override
  public void cleanup() {
    // Noop
  }

  @Override
  public ResourceReferenceFactory<EntityManager> registerPersistenceContextInjectionPoint(
      InjectionPoint injectionPoint) {
    final PersistenceContext pc = CDIs.getAnnotation(injectionPoint, PersistenceContext.class);
    return ResourceReferences
        .refac(() -> resolveApply(PersistenceService.class, b -> b.getEntityManager(pc)), t -> {
          if (t instanceof ExtendedEntityManager eem) {
            eem.destroy();
          } else {
            t.close();
          }
        });
  }

  @Override
  public ResourceReferenceFactory<EntityManagerFactory> registerPersistenceUnitInjectionPoint(
      InjectionPoint injectionPoint) {
    PersistenceUnit pu = CDIs.getAnnotation(injectionPoint, PersistenceUnit.class);
    return ResourceReferences.refac(
        () -> resolveApply(PersistenceService.class, b -> b.getEntityManagerFactory(pu)),
        EntityManagerFactory::close);
  }

}
