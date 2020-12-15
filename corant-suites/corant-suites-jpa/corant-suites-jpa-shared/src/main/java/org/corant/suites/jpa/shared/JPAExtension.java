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

import static org.corant.context.Qualifiers.resolveNameds;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import org.corant.context.NamingReference;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.jpa.shared.PersistenceService.PersistenceUnitLiteral;
import org.corant.suites.jpa.shared.cdi.EntityManagerFactoryBean;
import org.corant.suites.jpa.shared.metadata.PersistenceUnitInfoMetaData;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * corant-suites-jpa-shared
 *
 * Initialize the named qualifier Entity Manager Factory bean for injection, use Unnamed qualifier
 * for injection while the configurations do not assign a name.
 *
 * TODO: Check persistence unit data source availability AfterDeploymentValidation.
 *
 * @author bingo 上午11:32:56
 *
 */
public class JPAExtension implements Extension {

  protected final Map<PersistenceUnit, PersistenceUnitInfoMetaData> persistenceUnitInfoMetaDatas =
      new ConcurrentHashMap<>();
  protected Logger logger = Logger.getLogger(getClass().getName());
  volatile InitialContext jndi;
  volatile boolean finishedMetadatas = false;

  public PersistenceUnitInfoMetaData getPersistenceUnitInfoMetaData(PersistenceUnit pu) {
    if (isBlank(pu.unitName()) && persistenceUnitInfoMetaDatas.size() == 1) {
      return persistenceUnitInfoMetaDatas.values().iterator().next();// FIXME
    }
    return persistenceUnitInfoMetaDatas.get(pu);
  }

  public Map<PersistenceUnit, PersistenceUnitInfoMetaData> getPersistenceUnitInfoMetaDatas() {
    shouldBeTrue(finishedMetadatas, "Persistent unit metadata collection has not been completed!");
    return Collections.unmodifiableMap(persistenceUnitInfoMetaDatas);
  }

  protected void onBeforeShutdown(@Observes @Priority(0) BeforeShutdown bs) {
    persistenceUnitInfoMetaDatas.clear();
  }

  void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery abd, final BeanManager beanManager) {
    // create entity manager factory bean from persistence units
    Map<String, Annotation[]> qualifiers = resolveNameds(persistenceUnitInfoMetaDatas.keySet()
        .stream().map(PersistenceUnit::unitName).collect(Collectors.toSet()));
    persistenceUnitInfoMetaDatas.forEach((pu, puim) -> {
      abd.addBean(new EntityManagerFactoryBean(beanManager, pu, qualifiers.get(pu.unitName())));
      if (puim.isBindToJndi()) {
        registerJndi(pu.unitName(), qualifiers.get(pu.unitName()));
      }
    });
  }

  void onBeforeBeanDiscovery(@Observes final BeforeBeanDiscovery event) {
    JPAConfig.from(ConfigProvider.getConfig()).forEach(pu -> persistenceUnitInfoMetaDatas
        .put(PersistenceUnitLiteral.of(pu.getPersistenceUnitName()), pu));
    finishedMetadatas = true;
  }

  synchronized void registerJndi(String un, Annotation[] quas) {
    if (isNotBlank(un)) {
      try {
        if (jndi == null) {
          jndi = new InitialContext();
          jndi.createSubcontext(JPAConfig.JNDI_SUBCTX_NAME);
        }
        String jndiName = JPAConfig.JNDI_SUBCTX_NAME + "/" + un;
        jndi.bind(jndiName, new NamingReference(EntityManagerFactory.class, quas));
        logger.fine(() -> String.format("Bind entity manager factorties %s to jndi.", jndiName));
      } catch (NamingException e) {
        throw new CorantRuntimeException(e);
      }
    }
  }

  void validate(@Observes AfterDeploymentValidation adv, BeanManager bm) {
    // TODO FIXME validate config check data source etc
  }
}
