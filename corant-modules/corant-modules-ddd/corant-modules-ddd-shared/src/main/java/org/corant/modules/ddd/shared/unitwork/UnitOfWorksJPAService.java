/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.ddd.shared.unitwork;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Specializes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.SynchronizationType;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import org.corant.modules.jpa.shared.JPAService;
import org.corant.modules.jta.shared.TransactionService;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-ddd-shared
 *
 * @author bingo 18:32:33
 */
@ApplicationScoped
@Specializes
public class UnitOfWorksJPAService extends JPAService {

  @Inject
  protected UnitOfWorks unitOfWorks;

  @Inject
  protected TransactionManager transactionManager;

  @Override
  public EntityManager createEntityManager(PersistenceContext pc) {
    if (pc.synchronization() == SynchronizationType.SYNCHRONIZED
        && TransactionService.isCurrentTransactionActive()) {
      try {
        unitOfWorks.currentDefaultUnitOfWorksManager()
            .initializeCurrentUnitOfWork(transactionManager.getTransaction());
      } catch (SystemException e) {
        throw new CorantRuntimeException(e);
      }
    }
    return super.createEntityManager(pc);
  }

}
