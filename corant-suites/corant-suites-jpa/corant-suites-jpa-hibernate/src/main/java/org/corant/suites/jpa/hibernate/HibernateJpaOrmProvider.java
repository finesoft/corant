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
package org.corant.suites.jpa.hibernate;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.MapUtils.asMap;
import static org.corant.shared.util.ObjectUtils.forceCast;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.datasource.shared.DataSourceConfig;
import org.corant.suites.jpa.shared.AbstractJpaProvider;
import org.corant.suites.jpa.shared.JpaExtension;
import org.corant.suites.jpa.shared.metadata.PersistenceUnitInfoMetaData;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;

/**
 * corant-suites-jpa-hibernate
 *
 * @author bingo 上午11:44:00
 *
 */
@ApplicationScoped
@Named("org.hibernate.jpa.HibernatePersistenceProvider")
public class HibernateJpaOrmProvider extends AbstractJpaProvider {

  static final Map<String, Object> PROPERTIES =
      asMap(AvailableSettings.JTA_PLATFORM, new NarayanaJtaPlatform());

  @Inject
  JpaExtension extension;

  @Override
  protected EntityManagerFactory buildEntityManagerFactory(PersistenceUnitInfoMetaData metaData) {
    shouldNotNull(metaData).configDataSource(dsn -> {
      try {
        return forceCast(
            getJndi().lookup(shouldNotNull(dsn).startsWith(DataSourceConfig.JNDI_SUBCTX_NAME) ? dsn
                : DataSourceConfig.JNDI_SUBCTX_NAME + "/" + dsn));
      } catch (NamingException e) {
        throw new CorantRuntimeException(e);
      }
    });
    return new HibernatePersistenceProvider().createContainerEntityManagerFactory(metaData,
        PROPERTIES);
  }

}
