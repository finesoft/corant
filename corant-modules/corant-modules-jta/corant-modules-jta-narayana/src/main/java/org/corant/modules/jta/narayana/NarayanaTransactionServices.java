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
package org.corant.modules.jta.narayana;

import static org.corant.context.Beans.find;
import static org.corant.context.Beans.resolveApply;
import static org.corant.context.Beans.select;
import static org.corant.shared.util.Assertions.shouldNotNull;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import org.corant.modules.jta.shared.TransactionService;
import org.corant.shared.exception.CorantRuntimeException;
import org.jboss.weld.transaction.spi.TransactionServices;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;

/**
 * corant-modules-jta-narayana
 *
 * @author bingo 下午7:47:50
 *
 */
public class NarayanaTransactionServices implements TransactionServices {

  @Override
  public void cleanup() {
    // NOOP
  }

  public TransactionManager getTransactionManager() {
    return resolveApply(NarayanaTransactionService.class,
        NarayanaTransactionService::getTransactionManager);
  }

  @Override
  public UserTransaction getUserTransaction() {
    return resolveApply(NarayanaTransactionService.class,
        NarayanaTransactionService::getUserTransaction);
  }

  @Override
  public boolean isTransactionActive() {
    try {
      int status = getTransactionManager().getStatus();
      return status == Status.STATUS_ACTIVE || status == Status.STATUS_COMMITTING
          || status == Status.STATUS_MARKED_ROLLBACK || status == Status.STATUS_PREPARED
          || status == Status.STATUS_PREPARING || status == Status.STATUS_ROLLING_BACK;
    } catch (SystemException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public void registerSynchronization(Synchronization synchronizedObserver) {
    try {
      getTransactionManager().getTransaction().registerSynchronization(synchronizedObserver);
    } catch (IllegalStateException | RollbackException | SystemException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @ApplicationScoped
  public static class NarayanaTransactionService implements TransactionService {

    @Override
    public TransactionManager getTransactionManager() {
      return find(TransactionManager.class)
          .orElse(com.arjuna.ats.jta.TransactionManager.transactionManager());
    }

    @Override
    public UserTransaction getUserTransaction() {
      final Instance<JTAEnvironmentBean> jtaEnvironmentBeans =
          shouldNotNull(select(JTAEnvironmentBean.class));
      if (jtaEnvironmentBeans.isUnsatisfied()) {
        return com.arjuna.ats.jta.common.jtaPropertyManager.getJTAEnvironmentBean()
            .getUserTransaction();
      } else {
        return jtaEnvironmentBeans.get().getUserTransaction();
      }
    }

  }
}
