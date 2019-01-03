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
import java.lang.annotation.Annotation;
import java.util.Map;
import javax.enterprise.inject.Instance;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SynchronizationType;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.jpa.shared.AbstractJpaExtension;
import org.corant.suites.jpa.shared.PersistenceUnitMetaData;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;

/**
 * corant-suites-jpa-hibernate
 *
 * @author bingo 下午6:54:00
 *
 */
public class HibernateJpaExtension extends AbstractJpaExtension {

  final Map<String, Object> properties =
      asMap(AvailableSettings.JTA_PLATFORM, new NarayanaJtaPlatform());
  final SynchronizationType syncType = SynchronizationType.SYNCHRONIZED;

  @Override
  protected EntityManager buildEntityManager(EntityManagerFactory emf, SynchronizationType syncType,
      Map<String, ?> pps) {
    return emf.createEntityManager(syncType, pps);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  protected EntityManagerFactory buildEntityManagerFactory(final Instance instance, String unitName,
      PersistenceUnitMetaData persistenceUnitMetaData) {
    logger.config(
        () -> String.format("Build entity manager factory for persistence unit %s", unitName));
    PersistenceUnitMetaData pumd = persistenceUnitMetaData;
    pumd.configDataSource(dsn -> {
      try {
        InitialContext jndi =
            (InitialContext) instance.select(InitialContext.class, new Annotation[0]).get();
        return forceCast(jndi.lookup(dsn));
      } catch (NamingException e) {
        throw new CorantRuntimeException(e);
      }
    });
    return new HibernatePersistenceProvider().createContainerEntityManagerFactory(pumd, properties);
  }


}
