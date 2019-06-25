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
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.corant.Corant;
import org.corant.kernel.util.Qualifiers;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.datasource.shared.DataSourceConfig;
import org.corant.suites.jpa.shared.AbstractJPAProvider;
import org.corant.suites.jpa.shared.inject.JPAProvider;
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
@JPAProvider("org.hibernate.jpa.HibernatePersistenceProvider")
public class HibernateJPAOrmProvider extends AbstractJPAProvider {

  static final Map<String, Object> PROPERTIES = mapOf(AvailableSettings.JTA_PLATFORM,
      new NarayanaJTAPlatform(), AvailableSettings.CDI_BEAN_MANAGER, Corant.me().getBeanManager());

  @Inject
  Instance<DataSource> datasources;

  @Override
  public EntityManagerFactory buildEntityManagerFactory(PersistenceUnitInfoMetaData metaData,
      Map<String, Object> additionalProperties) {
    shouldNotNull(metaData).configDataSource(this::resolveDataSource);
    Map<String, Object> properties = new HashMap<>(PROPERTIES);
    if (additionalProperties != null) {
      properties.putAll(additionalProperties);
    }
    return new HibernatePersistenceProvider().createContainerEntityManagerFactory(metaData,
        properties);
  }

  protected DataSource resolveDataSource(String dataSourceName) {
    if (isNotBlank(dataSourceName)
        && dataSourceName.startsWith(DataSourceConfig.JNDI_SUBCTX_NAME)) {
      try {
        return forceCast(new InitialContext().lookup(dataSourceName));
      } catch (NamingException e) {
        throw new CorantRuntimeException(e);
      }
    } else if (!datasources.isUnsatisfied()) {
      return datasources.select(Qualifiers.resolveNamed(dataSourceName)).get();
    }
    throw new CorantRuntimeException("Can not find any data source named %s", dataSourceName);
  }

}
