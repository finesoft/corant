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
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceUnit;
import javax.persistence.SynchronizationType;
import org.corant.kernel.util.CdiUtils;
import org.corant.shared.normal.Names.PersistenceNames;

/**
 * corant-suites-jpa-shared
 *
 * @author bingo 下午3:09:15
 *
 */
@ApplicationScoped
public abstract class AbstractJpaInjectionProvider {

  protected static final Map<String, EntityManagerFactory> emfs = new ConcurrentHashMap<>();

  @PreDestroy
  public void onPreDestroy() {
    for (EntityManagerFactory rrf : emfs.values()) {
      rrf.close();
    }
    emfs.clear();
  }

  protected abstract EntityManagerFactory buildEntityManagerFactoryRrf(String unitName);

  protected abstract EntityManager buildEntityManagerRrf(EntityManagerFactory emf, String unitName,
      PersistenceContextType pcType, SynchronizationType syncType, Map<String, ?> pps);

  protected EntityManager produceEntityManager(InjectionPoint injectionPoint) {
    final PersistenceContext pc =
        CdiUtils.getAnnotated(injectionPoint).getAnnotation(PersistenceContext.class);
    final PersistenceContextType pct = pc.type();
    final SynchronizationType st = pc.synchronization();
    final Map<String, String> pps =
        asStream(pc.properties()).collect(Collectors.toMap(p -> p.name(), p -> p.value()));
    String unitName = resolveUnitName(pc.name(), pc.unitName());
    return buildEntityManagerRrf(emfs.computeIfAbsent(unitName, this::buildEntityManagerFactoryRrf),
        unitName, pct, st, pps);
  }

  protected EntityManagerFactory produceEntityManagerFactory(InjectionPoint injectionPoint) {
    final PersistenceUnit pu =
        CdiUtils.getAnnotated(injectionPoint).getAnnotation(PersistenceUnit.class);
    String unitName = resolveUnitName(pu.name(), pu.unitName());
    return emfs.computeIfAbsent(unitName, this::buildEntityManagerFactoryRrf);
  }

  protected String resolveUnitName(String name, String unitName) {
    String usePuName = defaultString(unitName, PersistenceNames.PU_DFLT_NME);
    usePuName = isEmpty(name) ? usePuName : usePuName + "." + name;
    return usePuName;
  }
}
