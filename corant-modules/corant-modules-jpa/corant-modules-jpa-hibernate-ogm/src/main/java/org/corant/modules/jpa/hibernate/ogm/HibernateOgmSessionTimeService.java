/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.jpa.hibernate.ogm;

import static org.corant.shared.util.Maps.getMapInstant;
import static org.corant.shared.util.Maps.mapOf;
import org.bson.Document;
import org.corant.modules.jpa.hibernate.orm.HibernateOrmSessionTimeService;
import org.corant.shared.normal.Priorities;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;

/**
 * corant-modules-jpa-hibernate-ogm
 *
 * @author bingo 上午11:09:24
 *
 */
public class HibernateOgmSessionTimeService extends HibernateOrmSessionTimeService {

  static final Document timeBson =
      new Document(mapOf("serverStatus", 1, "repl", 0, "metrics", 0, "locks", 0));

  @Override
  public boolean accept(Class<?> provider) {
    return provider.equals(org.hibernate.ogm.jpa.HibernateOgmPersistence.class);
  }

  @Override
  public long get(boolean useEpochSeconds, SessionFactoryImplementor sessionFactory,
      Object object) {
    MongoDBDatastoreProvider mp = resolveMongoDBDatastoreProvider(sessionFactory);
    if (mp != null) {
      final long epochMillis =
          getMapInstant(mp.getDatabase().runCommand(timeBson), "localTime").toEpochMilli();
      return useEpochSeconds ? epochMillis / 1000L + 1 : epochMillis;
    } else {
      return super.get(useEpochSeconds, sessionFactory, object);
    }
  }

  @Override
  public int getPriority() {
    return Priorities.APPLICATION_LOWER;
  }

  MongoDBDatastoreProvider resolveMongoDBDatastoreProvider(
      SessionFactoryImplementor sessionFactory) {
    try {
      if (sessionFactory != null) {
        return sessionFactory.getServiceRegistry().getService(MongoDBDatastoreProvider.class);
      }
    } catch (Exception e) {
      // Noop FIXME
    }
    return null;
  }
}
