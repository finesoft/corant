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

import static org.corant.shared.util.MapUtils.mapOf;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManagerFactory;
import org.corant.suites.jpa.shared.AbstractJPAProvider;
import org.corant.suites.jpa.shared.inject.JPAProvider;
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
@JPAProvider("org.hibernate.ogm.jpa.HibernateOgmPersistence")
public class HibernateJPAOgmProvider extends AbstractJPAProvider {

  static final Map<String, Object> PROPERTIES =
      mapOf(AvailableSettings.JTA_PLATFORM, new NarayanaJTAPlatform());

  @Override
  public EntityManagerFactory buildEntityManagerFactory(PersistenceUnitInfoMetaData metaData) {
    return new HibernateOgmPersistence().createContainerEntityManagerFactory(metaData, PROPERTIES);
  }

}
