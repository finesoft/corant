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
package org.corant.suites.jpa.hibernate;

import static org.corant.shared.util.MapUtils.asMap;
import static org.corant.shared.util.ObjectUtils.forceCast;
import java.util.Map;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContextType;
import javax.persistence.SynchronizationType;
import org.corant.kernel.event.PostContainerStartedEvent;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.jpa.shared.AbstractJpaInjectionProvider;
import org.corant.suites.jpa.shared.PersistenceUnitMetaData;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;

/**
 * corant-suites-jpa-hibernate
 *
 * @author bingo 下午7:26:06
 *
 */
@ApplicationScoped
public class HibernateJpaInjectionProvider extends AbstractJpaInjectionProvider {

  final Map<String, Object> properties =
      asMap(AvailableSettings.JTA_PLATFORM, new NarayanaJtaPlatform());
  final SynchronizationType syncType = SynchronizationType.SYNCHRONIZED;

  @Inject
  HibernateJpaExtension extension;

  @Inject
  InitialContext jndi;

  @Override
  protected EntityManagerFactory buildEntityManagerFactoryRrf(String unitName) {
    final PersistenceUnitMetaData pumd = extension.getPersistenceUnitMetaDatas().get(unitName);
    pumd.configDataSource(dsn -> {
      try {
        return forceCast(jndi.lookup(dsn));
      } catch (NamingException e) {
        throw new CorantRuntimeException(e);
      }
    });
    return new HibernatePersistenceProvider().createContainerEntityManagerFactory(pumd, properties);
  }

  @Override
  protected EntityManager buildEntityManagerRrf(EntityManagerFactory emf, String unitName,
      PersistenceContextType pcType, SynchronizationType syncType, Map<String, ?> pps) {
    return emf.createEntityManager(syncType, pps);
  }



  @Produces
  @Override
  protected EntityManager produceEntityManager(InjectionPoint injectionPoint) {
    return super.produceEntityManager(injectionPoint);
  }

  @Produces
  @ApplicationScoped
  @Override
  protected EntityManagerFactory produceEntityManagerFactory(InjectionPoint injectionPoint) {
    return super.produceEntityManagerFactory(injectionPoint);
  }

  void onPostContainerStarted(@Observes @Priority(100) PostContainerStartedEvent e) {
    System.out.println("===========");
  }
}
