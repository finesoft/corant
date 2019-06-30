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
package org.corant.suites.jpa.shared;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.ObjectUtils.isEquals;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import org.corant.kernel.service.PersistenceService.PersistenceContextLiteral;
import org.corant.kernel.service.PersistenceService.PersistenceUnitLiteral;
import org.corant.kernel.util.CDIs;
import org.corant.kernel.util.Instances.NamingReference;
import org.corant.kernel.util.Qualifiers;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.jpa.shared.inject.EntityManagerFactoryBean;
import org.corant.suites.jpa.shared.metadata.PersistenceUnitInfoMetaData;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * corant-suites-jpa-shared
 *
 * Initialize the named qualifier Entity Manager Factory bean for injection, use Unnamed qualifier
 * for injection while the configurations do not assign a name.
 *
 * @author bingo 上午11:32:56
 *
 */
public class JPAExtension implements Extension {

  protected final Set<PersistenceUnit> persistenceUnits =
      Collections.newSetFromMap(new ConcurrentHashMap<PersistenceUnit, Boolean>());
  protected final Set<PersistenceContext> persistenceContexts =
      Collections.newSetFromMap(new ConcurrentHashMap<PersistenceContext, Boolean>());
  protected final Map<PersistenceUnit, PersistenceUnitInfoMetaData> persistenceUnitInfoMetaDatas =
      new ConcurrentHashMap<>();
  protected Logger logger = Logger.getLogger(getClass().getName());

  volatile InitialContext jndi;

  public PersistenceUnitInfoMetaData getPersistenceUnitInfoMetaData(PersistenceUnit pu) {
    return persistenceUnitInfoMetaDatas.get(pu);
  }

  void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery abd, final BeanManager beanManager) {

    // assembly
    persistenceContexts.forEach(pc -> {
      if (persistenceUnits.stream().map(PersistenceUnit::unitName)
          .noneMatch((un) -> isEquals(pc.unitName(), un))) {
        persistenceUnits.add(PersistenceUnitLiteral.of(pc));
      }
    });

    // check persistence pu injections
    persistenceUnits.forEach(pumd -> {
      shouldBeTrue(persistenceUnitInfoMetaDatas.containsKey(pumd),
          "Can not find persistence pu named %s for injection!", pumd.unitName());
    });

    // create entity manager factory bean from persistence units
    persistenceUnitInfoMetaDatas.forEach((pu, puim) -> {
      abd.addBean(new EntityManagerFactoryBean(beanManager, pu));
      registerJndi(pu.unitName());
    });

  }

  void onBeforeBeanDiscovery(@Observes final BeforeBeanDiscovery event) {
    JPAConfig.from(ConfigProvider.getConfig())
        .forEach((pun, pu) -> persistenceUnitInfoMetaDatas.put(PersistenceUnitLiteral.of(pun), pu));
  }

  void onProcessInjectionPoint(@Observes ProcessInjectionPoint<?, ?> pip, BeanManager beanManager) {
    final InjectionPoint ip = pip.getInjectionPoint();
    final PersistenceUnit pu = CDIs.getAnnotated(ip).getAnnotation(PersistenceUnit.class);
    if (pu != null) {
      persistenceUnits.add(PersistenceUnitLiteral.of(pu));
    }
    final PersistenceContext pc = CDIs.getAnnotated(ip).getAnnotation(PersistenceContext.class);
    if (pc != null) {
      persistenceContexts.add(PersistenceContextLiteral.of(pc));
    }
  }

  synchronized void registerJndi(String un) {
    if (isNotBlank(un)) {
      try {
        if (jndi == null) {
          jndi = new InitialContext();
          jndi.createSubcontext(JPAConfig.JNDI_SUBCTX_NAME);
        }
        String jndiName = JPAConfig.JNDI_SUBCTX_NAME + "/" + un;
        jndi.bind(jndiName,
            new NamingReference(EntityManagerFactory.class, Qualifiers.resolveNameds(un)));
        logger.info(() -> String.format("Bind entity manager factorties %s to jndi.", jndiName));
      } catch (NamingException e) {
        throw new CorantRuntimeException(e);
      }
    }
  }
}
