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
import java.util.logging.Logger;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import org.corant.kernel.util.CdiUtils;
import org.corant.kernel.util.ResourceReferences;
import org.corant.suites.jpa.shared.metadata.PersistenceContextMetaData;
import org.corant.suites.jpa.shared.metadata.PersistenceUnitMetaData;
import org.jboss.weld.injection.spi.JpaInjectionServices;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

/**
 * The JpaInjectionServices implemention, we don't use it by default. We use
 * AbstractJpaProvider by default, because it support scope. If you're going to
 * use it, the new subclasses, and set in the meta-inf/services corresponding
 * "org. jboss.Weld.bootstrap.api.Service" interface description file.
 *
 * @see AbstractJpaProvider
 * @author bingo 下午4:18:45
 *
 */
public abstract class AbstractJpaInjectionServices implements JpaInjectionServices {

  protected static final Map<PersistenceUnitMetaData, EntityManagerFactory> EMFS =
      new ConcurrentHashMap<>();

  protected Logger logger = Logger.getLogger(getClass().getName());

  @Override
  public void cleanup() {
    EMFS.values().forEach(emf -> {
      if (emf.isOpen()) {
        emf.close();
      }
    });
  }

  @Override
  public ResourceReferenceFactory<EntityManager> registerPersistenceContextInjectionPoint(
      InjectionPoint injectionPoint) {
    PersistenceContext pc =
        CdiUtils.getAnnotated(injectionPoint).getAnnotation(PersistenceContext.class);
    PersistenceContextMetaData pcmd = PersistenceContextMetaData.of(pc);
    return ResourceReferences.refac(() -> getEntityManagerFactory(pcmd.getUnit())
        .createEntityManager(pcmd.getSynchronization(), pcmd.getProperties()));
  }

  @Override
  public ResourceReferenceFactory<EntityManagerFactory> registerPersistenceUnitInjectionPoint(
      InjectionPoint injectionPoint) {
    PersistenceUnit pu = CdiUtils.getAnnotated(injectionPoint).getAnnotation(PersistenceUnit.class);
    final PersistenceUnitMetaData pumd = PersistenceUnitMetaData.of(pu);
    return ResourceReferences.refac(() -> getEntityManagerFactory(pumd));
  }

  @Override
  public EntityManager resolvePersistenceContext(InjectionPoint injectionPoint) {
    return registerPersistenceContextInjectionPoint(injectionPoint).createResource().getInstance();
  }

  @Override
  public EntityManagerFactory resolvePersistenceUnit(InjectionPoint injectionPoint) {
    return registerPersistenceUnitInjectionPoint(injectionPoint).createResource().getInstance();
  }

  protected abstract EntityManagerFactory buildEntityManagerFactory(
      PersistenceUnitMetaData metaData);

  protected EntityManagerFactory getEntityManagerFactory(PersistenceUnitMetaData pumd) {
    return EMFS.computeIfAbsent(pumd, this::buildEntityManagerFactory);
  }

}
