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

import static org.corant.shared.util.StreamUtils.asStream;
import static org.corant.shared.util.StringUtils.defaultString;
import static org.corant.shared.util.StringUtils.isEmpty;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceUnit;
import javax.persistence.SynchronizationType;
import org.corant.kernel.util.CdiUtils;
import org.corant.shared.normal.Names.PersistenceNames;
import org.jboss.weld.injection.spi.JpaInjectionServices;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

/**
 * corant-suites-jpa-shared
 *
 * @author bingo 下午3:09:15
 *
 */
public abstract class AbstractJpaInjectionServices implements JpaInjectionServices {

  protected static final Map<String, ResourceReferenceFactory<EntityManagerFactory>> emfs =
      new ConcurrentHashMap<>();
  protected static final Map<String, ResourceReferenceFactory<EntityManager>> ems =
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
    final PersistenceContext pc =
        CdiUtils.getAnnotated(injectionPoint).getAnnotation(PersistenceContext.class);
    final PersistenceContextType pct = pc.type();
    final SynchronizationType st = pc.synchronization();
    final Map<String, String> pps =
        asStream(pc.properties()).collect(Collectors.toMap(p -> p.name(), p -> p.value()));
    String unitName = resolveUnitName(pc.name(), pc.unitName());
    ResourceReferenceFactory<EntityManagerFactory> emf =
        emfs.computeIfAbsent(unitName, this::buildEntityManagerFactoryRrf);
    return ems.computeIfAbsent(unitName,
        (un) -> buildEntityManagerRrf(emf.createResource().getInstance(), un, pct, st, pps));
  }

  @Override
  public ResourceReferenceFactory<EntityManagerFactory> registerPersistenceUnitInjectionPoint(
      InjectionPoint injectionPoint) {
    final PersistenceUnit pu =
        CdiUtils.getAnnotated(injectionPoint).getAnnotation(PersistenceUnit.class);
    String unitName = resolveUnitName(pu.name(), pu.unitName());
    return emfs.computeIfAbsent(unitName, this::buildEntityManagerFactoryRrf);
  }

  @Override
  public EntityManager resolvePersistenceContext(InjectionPoint injectionPoint) {
    return registerPersistenceContextInjectionPoint(injectionPoint).createResource().getInstance();
  }

  @Override
  public EntityManagerFactory resolvePersistenceUnit(InjectionPoint injectionPoint) {
    return registerPersistenceUnitInjectionPoint(injectionPoint).createResource().getInstance();
  }

  protected abstract ResourceReferenceFactory<EntityManagerFactory> buildEntityManagerFactoryRrf(
      String unitName);

  protected abstract ResourceReferenceFactory<EntityManager> buildEntityManagerRrf(
      EntityManagerFactory emf, String unitName, PersistenceContextType pcType,
      SynchronizationType syncType, Map<String, ?> pps);

  protected String resolveUnitName(String name, String unitName) {
    String usePuName = defaultString(unitName, PersistenceNames.PU_DFLT_NME);
    usePuName = isEmpty(name) ? usePuName : usePuName + "." + name;
    return usePuName;
  }
}
