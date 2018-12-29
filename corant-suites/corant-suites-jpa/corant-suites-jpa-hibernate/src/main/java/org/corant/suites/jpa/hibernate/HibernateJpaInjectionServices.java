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
import static org.corant.shared.util.ObjectUtils.shouldNotNull;
import java.util.Map;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContextType;
import javax.persistence.SynchronizationType;
import org.corant.kernel.util.ResourceReferences;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.jpa.shared.AbstractJpaInjectionServices;
import org.corant.suites.jpa.shared.PersistenceUnitMetaData;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

/**
 * corant-suites-jpa-hibernate
 *
 * @author bingo 下午7:26:06
 *
 */
public class HibernateJpaInjectionServices extends AbstractJpaInjectionServices {

  @Inject
  HibernateJpaExtension extension;

  @Inject
  InitialContext jndi;

  final Map<String, Object> properties =
      asMap(AvailableSettings.JTA_PLATFORM, new NarayanaJtaPlatform());
  final SynchronizationType syncType = SynchronizationType.SYNCHRONIZED;

  @Override
  protected ResourceReferenceFactory<EntityManagerFactory> buildEntityManagerFactoryRrf(
      String unitName) {
    final PersistenceUnitMetaData pumd =
        shouldNotNull(extension.getPersistenceUnitMetaDatas().get(unitName),
            "Can not find persistence unit info for %s", unitName);
    pumd.configDataSource(dsn -> {
      try {
        return forceCast(jndi.lookup(dsn));
      } catch (NamingException e) {
        throw new CorantRuntimeException(e);
      }
    });
    return () -> ResourceReferences.of(() -> new HibernatePersistenceProvider()
        .createContainerEntityManagerFactory(pumd, properties));
  }

  @Override
  protected ResourceReferenceFactory<EntityManager> buildEntityManagerRrf(EntityManagerFactory emf,
      String unitName, PersistenceContextType pcType, SynchronizationType syncType,
      Map<String, ?> pps) {
    return () -> ResourceReferences.of(() -> emf.createEntityManager(syncType, pps));
  }
}
