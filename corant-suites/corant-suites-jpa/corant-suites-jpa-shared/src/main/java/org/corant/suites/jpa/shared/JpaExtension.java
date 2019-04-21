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

import static org.corant.shared.util.Assertions.shouldBeFalse;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.ObjectUtils.isEquals;
import static org.corant.shared.util.StringUtils.defaultTrim;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
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
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceUnit;
import org.corant.kernel.util.Cdis;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.jndi.DefaultReference;
import org.corant.suites.jpa.shared.inject.EntityManagerBean;
import org.corant.suites.jpa.shared.inject.EntityManagerFactoryBean;
import org.corant.suites.jpa.shared.inject.ExtendedPersistenceContextType;
import org.corant.suites.jpa.shared.inject.PersistenceContextInjectionPoint;
import org.corant.suites.jpa.shared.inject.TransactionPersistenceContextType;
import org.corant.suites.jpa.shared.metadata.PersistenceContextMetaData;
import org.corant.suites.jpa.shared.metadata.PersistenceUnitInfoMetaData;
import org.corant.suites.jpa.shared.metadata.PersistenceUnitMetaData;
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
public class JpaExtension implements Extension {

  protected final Set<PersistenceUnitMetaData> persistenceUnits =
      Collections.newSetFromMap(new ConcurrentHashMap<PersistenceUnitMetaData, Boolean>());
  protected final Set<PersistenceContextMetaData> persistenceContexts =
      Collections.newSetFromMap(new ConcurrentHashMap<PersistenceContextMetaData, Boolean>());
  protected final Map<String, PersistenceUnitInfoMetaData> persistenceUnitInfoMetaDatas =
      new ConcurrentHashMap<>();
  protected Logger logger = Logger.getLogger(getClass().getName());

  volatile boolean initedJndiSubCtx = false;
  volatile InitialContext jndi;

  public PersistenceUnitInfoMetaData getPersistenceUnitInfoMetaData(String name) {
    return persistenceUnitInfoMetaDatas.get(defaultTrim(name));
  }

  void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery abd, final BeanManager beanManager) {
    // assembly
    persistenceContexts.forEach(pc -> {
      if (persistenceUnits.stream().map(PersistenceUnitMetaData::getUnitName)
          .noneMatch((un) -> isEquals(pc.getUnitName(), un))) {
        persistenceUnits.add(new PersistenceUnitMetaData(null, pc.getUnitName()));
      }
    });
    // check persistence unit injections
    persistenceUnits.forEach(pumd -> {
      final String unitName = pumd.getUnitName();
      shouldBeTrue(persistenceUnitInfoMetaDatas.containsKey(unitName),
          "Can not find persistence unit named %s for injection.", unitName);
    });
    // create entity manager factory bean from persistence units and register ref to jndi
    persistenceUnitInfoMetaDatas.forEach((un, puim) -> {
      abd.addBean(new EntityManagerFactoryBean(beanManager, un));
      registerJndi(un);
    });
    // create entity manager bean from persistence contexts
    persistenceContexts.forEach(pcmd -> {
      Annotation qualifier = Cdis.resolveNamed(pcmd.getUnitName());
      abd.addBean(new EntityManagerBean(beanManager, pcmd, qualifier));
    });
  }

  void onBeforeBeanDiscovery(@Observes final BeforeBeanDiscovery event) {
    JpaConfig.from(ConfigProvider.getConfig()).forEach(persistenceUnitInfoMetaDatas::put);
  }

  void onProcessInjectionPoint(@Observes ProcessInjectionPoint<?, ?> pip, BeanManager beanManager) {
    final InjectionPoint ip = pip.getInjectionPoint();
    final PersistenceUnit pu = Cdis.getAnnotated(ip).getAnnotation(PersistenceUnit.class);
    if (pu != null) {
      persistenceUnits.add(new PersistenceUnitMetaData(pu));
      pip.configureInjectionPoint().addQualifiers(Cdis.resolveNameds(pu.unitName()));
    }
    final PersistenceContext pc = Cdis.getAnnotated(ip).getAnnotation(PersistenceContext.class);
    if (pc != null) {
      if (pc.type() != PersistenceContextType.TRANSACTION) {
        shouldBeFalse(ip.getBean().getScope().equals(ApplicationScoped.class));
        pip.setInjectionPoint(new PersistenceContextInjectionPoint(ip,
            ExtendedPersistenceContextType.INST, Any.Literal.INSTANCE));
      } else {
        pip.setInjectionPoint(new PersistenceContextInjectionPoint(ip,
            TransactionPersistenceContextType.INST, Any.Literal.INSTANCE));
      }
      persistenceContexts.add(new PersistenceContextMetaData(pc));
    }
  }

  void registerJndi(String un) {
    if (isNotBlank(un)) {
      try {
        if (jndi == null) {
          jndi = new InitialContext();
        }
        if (!initedJndiSubCtx) {
          jndi.createSubcontext(JpaConfig.JNDI_SUBCTX_NAME);
          initedJndiSubCtx = true;
        }
        String jndiName = JpaConfig.JNDI_SUBCTX_NAME + "/" + un;
        jndi.bind(jndiName,
            new DefaultReference(EntityManagerFactory.class, Cdis.resolveNameds(un)));
        logger.info(() -> String.format("Bind entity manager factory %s to jndi!", jndiName));
      } catch (NamingException e) {
        throw new CorantRuntimeException(e);
      }
    }
  }
}
