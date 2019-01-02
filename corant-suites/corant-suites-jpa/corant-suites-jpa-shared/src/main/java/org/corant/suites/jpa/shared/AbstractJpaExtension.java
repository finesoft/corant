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

import static org.corant.shared.util.ObjectUtils.shouldNotNull;
import static org.corant.shared.util.StreamUtils.asStream;
import static org.corant.shared.util.StringUtils.defaultString;
import static org.corant.shared.util.StringUtils.isEmpty;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.persistence.SynchronizationType;
import org.corant.kernel.util.CdiUtils;
import org.corant.shared.normal.Names.PersistenceNames;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * corant-suites-jpa-shared
 *
 * @author bingo 下午7:22:45
 *
 */
public abstract class AbstractJpaExtension implements Extension {

  protected static final Map<String, EntityManagerFactory> EMFS = new ConcurrentHashMap<>();
  protected static final Set<InjectionPoint> PUIPS =
      Collections.newSetFromMap(new ConcurrentHashMap<InjectionPoint, Boolean>());
  protected static final Set<InjectionPoint> PCIPS =
      Collections.newSetFromMap(new ConcurrentHashMap<InjectionPoint, Boolean>());

  private final Map<String, PersistenceUnitMetaData> persistenceUnitMetaDatas = new HashMap<>();

  protected abstract EntityManager buildEntityManager(EntityManagerFactory emf,
      SynchronizationType syncType, Map<String, ?> pps);

  protected abstract EntityManagerFactory buildEntityManagerFactory(Instance<?> instance,
      String unitName, PersistenceUnitMetaData persistenceUnitMetaData);

  protected EntityManagerFactory getEntityManagerFactory(Instance<?> instance, String unitName,
      PersistenceUnitMetaData persistenceUnitMetaData) {
    return EMFS.computeIfAbsent(unitName,
        pun -> buildEntityManagerFactory(instance, pun, persistenceUnitMetaData));
  }

  protected Map<String, PersistenceUnitMetaData> getPersistenceUnitMetaDatas() {
    return Collections.unmodifiableMap(persistenceUnitMetaDatas);
  }

  protected String resolveUnitName(String name, String unitName) {
    String usePuName = defaultString(unitName, PersistenceNames.PU_DFLT_NME);
    usePuName = isEmpty(name) ? usePuName : usePuName + "." + name;
    return usePuName;
  }

  void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery abd) {
    PUIPS.forEach(ip -> {
      final PersistenceUnit pu = CdiUtils.getAnnotated(ip).getAnnotation(PersistenceUnit.class);
      final String pun = resolveUnitName(pu.name(), pu.unitName());
      PersistenceUnitMetaData pumd = shouldNotNull(persistenceUnitMetaDatas.get(pun));
      abd.<EntityManagerFactory>addBean().addQualifier(Default.Literal.INSTANCE)
          .addTransitiveTypeClosure(EntityManagerFactory.class)
          .beanClass(EntityManagerFactory.class).scope(ApplicationScoped.class)
          .produceWith(inst -> getEntityManagerFactory(inst, pun, pumd))
          .disposeWith((emf, beans) -> emf.close());
    });

    PCIPS.forEach(ip -> {
      final PersistenceContext pc =
          CdiUtils.getAnnotated(ip).getAnnotation(PersistenceContext.class);
      // final PersistenceContextType pct = pc.type();
      final SynchronizationType st = pc.synchronization();
      final String pun = resolveUnitName(pc.name(), pc.unitName());
      final PersistenceUnitMetaData pumd = shouldNotNull(persistenceUnitMetaDatas.get(pun));
      final Map<String, ?> pps =
          asStream(pc.properties()).collect(Collectors.toMap(p -> p.name(), p -> p.value()));
      abd.<EntityManager>addBean().addQualifier(Default.Literal.INSTANCE)
          .addTransitiveTypeClosure(EntityManager.class).beanClass(EntityManager.class)
          .scope(Dependent.class).produceWith(inst -> {
            return buildEntityManager(getEntityManagerFactory(inst, pun, pumd), st, pps);
          }).disposeWith((em, beans) -> {
          });
    });
  }

  void onBeforeBeanDiscovery(@Observes final BeforeBeanDiscovery event) {
    JpaConfig cfg = JpaConfig.from(ConfigProvider.getConfig());
    cfg.getMetaDatas().forEach((n, pu) -> {
      persistenceUnitMetaDatas.put(n, pu);
    });
  }

  void onProcessInjectionPoint(@Observes ProcessInjectionPoint<?, ?> pip) {
    final InjectionPoint ip = pip.getInjectionPoint();
    if (CdiUtils.getAnnotated(ip).getAnnotation(PersistenceUnit.class) != null) {
      PUIPS.add(ip);
    }
    if (CdiUtils.getAnnotated(ip).getAnnotation(PersistenceContext.class) != null) {
      PCIPS.add(ip);
    }
  }
}
