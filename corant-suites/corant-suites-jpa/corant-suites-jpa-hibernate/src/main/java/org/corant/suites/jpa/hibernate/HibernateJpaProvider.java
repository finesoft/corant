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
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.jpa.shared.AbstractJpaProvider;
import org.corant.suites.jpa.shared.JpaExtension;
import org.corant.suites.jpa.shared.metadata.PersistenceUnitInfoMetaData;
import org.corant.suites.jpa.shared.metadata.PersistenceUnitMetaData;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;

/**
 * corant-suites-jpa-hibernate
 *
 * @author bingo 上午11:44:00
 *
 */
@ApplicationScoped
public class HibernateJpaProvider extends AbstractJpaProvider {

  final Map<String, Object> properties =
      asMap(AvailableSettings.JTA_PLATFORM, new NarayanaJtaPlatform());

  @Inject
  JpaExtension extension;

  @Inject
  InitialContext jndi;

  @Override
  protected EntityManagerFactory build(PersistenceUnitMetaData metaData) {
    String name = metaData.getMixedName();
    PersistenceUnitInfoMetaData puimd =
        shouldNotNull(extension.getPersistenceUnitInfoMetaData(name));
    puimd.configDataSource(dsn -> {
      try {
        return forceCast(jndi.lookup(dsn));
      } catch (NamingException e) {
        throw new CorantRuntimeException(e);
      }
    });
    return new HibernatePersistenceProvider().createContainerEntityManagerFactory(puimd,
        properties);
  }

}
