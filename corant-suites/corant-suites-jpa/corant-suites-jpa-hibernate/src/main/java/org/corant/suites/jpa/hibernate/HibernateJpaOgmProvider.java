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
import static org.corant.shared.util.StringUtils.split;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import org.corant.suites.jpa.shared.AbstractJpaProvider;
import org.corant.suites.jpa.shared.JpaExtension;
import org.corant.suites.jpa.shared.inject.JpaProvider;
import org.corant.suites.jpa.shared.metadata.PersistenceUnitInfoMetaData;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.ogm.jpa.HibernateOgmPersistence;

/**
 * corant-suites-jpa-hibernate
 *
 * @author bingo 上午11:44:00
 *
 */
@ApplicationScoped
@JpaProvider("org.hibernate.ogm.jpa.HibernateOgmPersistence")
public class HibernateJpaOgmProvider extends AbstractJpaProvider {

  static final Map<String, Object> PROPERTIES =
      asMap(AvailableSettings.JTA_PLATFORM, new NarayanaJtaPlatform());

  @Inject
  JpaExtension extension;

  @Override
  protected EntityManagerFactory buildEntityManagerFactory(PersistenceUnitInfoMetaData metaData) {
    String dsNme = metaData.getJtaDataSourceName();
    String[] noSqls = split(dsNme, ":", true, true);
    shouldNotNull(metaData).configProperties("hibernate.ogm.datastore.provider", noSqls[0]);
    shouldNotNull(metaData).configProperties("hibernate.ogm.datastore.database", noSqls[1]);
    return new HibernateOgmPersistence().createContainerEntityManagerFactory(metaData, PROPERTIES);
  }

}
