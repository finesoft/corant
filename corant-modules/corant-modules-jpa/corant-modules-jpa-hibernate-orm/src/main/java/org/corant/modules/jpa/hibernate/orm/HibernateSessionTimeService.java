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
package org.corant.modules.jpa.hibernate.orm;

import org.corant.shared.ubiquity.Sortable;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * corant-modules-jpa-hibernate-orm
 *
 * @author bingo 上午10:55:11
 *
 */
@FunctionalInterface
public interface HibernateSessionTimeService extends Sortable {

  default boolean accept(Class<?> provider) {
    return true;
  }

  /**
   * Returns the time sequence, if given sessionFactory is null, implementation must use
   * {@link System#currentTimeMillis()}.
   *
   * @param useEpochSeconds whether use epoch second
   * @param sessionFactory the session factory use to get time sequence from underly data source
   * @param object the entity that need id, do not use here, may be used in future
   */
  long get(boolean useEpochSeconds, SessionFactoryImplementor sessionFactory, Object object);

}
