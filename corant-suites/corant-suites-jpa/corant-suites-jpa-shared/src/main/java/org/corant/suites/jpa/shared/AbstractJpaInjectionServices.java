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
package org.corant.suites.jpa.shared;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.jboss.weld.injection.spi.JpaInjectionServices;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

public abstract class AbstractJpaInjectionServices implements JpaInjectionServices {

  protected static Map<String, ResourceReferenceFactory<EntityManagerFactory>> emfs =
      new ConcurrentHashMap<>();
  protected static Map<String, ResourceReferenceFactory<EntityManager>> ems =
      new ConcurrentHashMap<>();

  @Override
  public void cleanup() {
    for (ResourceReferenceFactory<EntityManagerFactory> rrf : emfs.values()) {
      rrf.createResource().getInstance().close();
    }
    emfs.clear();
  }

  @Override
  public ResourceReferenceFactory<EntityManager> registerPersistenceContextInjectionPoint(
      InjectionPoint injectionPoint) {
    return null;
  }

  @Override
  public ResourceReferenceFactory<EntityManagerFactory> registerPersistenceUnitInjectionPoint(
      InjectionPoint injectionPoint) {
    return null;
  }

  @Override
  public EntityManager resolvePersistenceContext(InjectionPoint injectionPoint) {
    return registerPersistenceContextInjectionPoint(injectionPoint).createResource().getInstance();
  }

  @Override
  public EntityManagerFactory resolvePersistenceUnit(InjectionPoint injectionPoint) {
    return registerPersistenceUnitInjectionPoint(injectionPoint).createResource().getInstance();
  }

}
