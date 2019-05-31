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
import static org.corant.shared.util.MapUtils.mapOf;
import static org.corant.shared.util.ObjectUtils.forceCast;
import java.util.Map;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import org.corant.Corant;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.datasource.shared.DataSourceConfig;
import org.corant.suites.jpa.shared.AbstractJPAInjectionServices;
import org.corant.suites.jpa.shared.metadata.PersistenceUnitInfoMetaData;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;

/**
 * corant-suites-jpa-hibernate
 *
 * @author bingo 下午4:36:44
 *
 */
public class HibernateJPAInjectionServices extends AbstractJPAInjectionServices {

  static final Map<String, Object> PROPERTIES = mapOf(AvailableSettings.JTA_PLATFORM,
      new NarayanaJTAPlatform(), AvailableSettings.HBM2DDL_DATABASE_ACTION, "none",
      AvailableSettings.HBM2DDL_CHARSET_NAME, "UTF-8");

  @Override
  protected EntityManagerFactory buildEntityManagerFactory(PersistenceUnitInfoMetaData metaData) {
    InitialContext jndi = Corant.instance().select(InitialContext.class).get();
    shouldNotNull(metaData).configDataSource((dsn) -> {
      try {
        return forceCast(
            jndi.lookup(shouldNotNull(dsn).startsWith(DataSourceConfig.JNDI_SUBCTX_NAME) ? dsn
                : DataSourceConfig.JNDI_SUBCTX_NAME + "/" + dsn));
      } catch (NamingException e) {
        throw new CorantRuntimeException(e);
      }
    });
    return new HibernatePersistenceProvider().createContainerEntityManagerFactory(metaData,
        PROPERTIES);
  }

}
