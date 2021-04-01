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
import org.hibernate.engine.spi.SharedSessionContractImplementor;

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

  long resolve(boolean useEpochSeconds, SharedSessionContractImplementor session, Object object);

}
