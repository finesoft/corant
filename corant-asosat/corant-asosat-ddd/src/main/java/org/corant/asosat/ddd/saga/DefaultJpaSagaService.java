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
package org.corant.asosat.ddd.saga;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import org.corant.shared.exception.NotSupportedException;
import org.corant.suites.ddd.annotation.qualifier.JPA;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.suites.ddd.repository.JpaRepository;
import org.corant.suites.ddd.saga.Saga;
import org.corant.suites.jpa.shared.JpaUtils;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午2:50:28
 *
 */
@InfrastructureServices
@ApplicationScoped
public class DefaultJpaSagaService extends AbstractSagaService {

  @Inject
  @JPA
  protected JpaRepository repo;

  protected final Map<Class<?>, Boolean> persistSagaClasses =
      new ConcurrentHashMap<>(256, 0.75f, 256);

  @Transactional
  @Override
  public void persist(Saga saga) {
    if (persistSagaClasses.computeIfAbsent(saga.getClass(), JpaUtils::isPersistenceEntityClass)) {
      repo.persist(saga);
      repo.getEntityManager().flush();
    } else {
      throw new NotSupportedException();
    }
  }

}
