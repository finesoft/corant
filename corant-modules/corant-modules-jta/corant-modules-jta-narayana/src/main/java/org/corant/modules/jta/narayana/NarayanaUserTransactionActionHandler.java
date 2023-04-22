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

import static java.security.AccessController.doPrivileged;
import java.security.PrivilegedAction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.Transactional.TxType;
import org.corant.modules.jta.shared.TransactionalAction.UserTransactionActionHandler;
import org.jboss.tm.usertx.UserTransactionOperationsProvider;
import com.arjuna.ats.jta.common.jtaPropertyManager;

/**
 * corant-modules-jta-narayana
 *
 * NOTE: Some code come from narayana, if there is infringement, please inform
 * me(finesoft@gmail.com).
 *
 * @author bingo 下午7:25:07
 *
 */
@ApplicationScoped
public class NarayanaUserTransactionActionHandler implements UserTransactionActionHandler {

  @Override
  public void afterExecute(boolean prestatus, Transaction tx, TransactionManager tm, TxType type) {
    UserTransactionOperationsProvider userTransactionProvider =
        jtaPropertyManager.getJTAEnvironmentBean().getUserTransactionOperationsProvider();
    setAvailability(userTransactionProvider, prestatus);
  }

  @Override
  public boolean beforeExecute(Transaction tx, TransactionManager tm, TxType type,
      Class<?>[] rollbackOn, Class<?>[] dontRollbackOn) {
    return setUserTransactionAvailable(type == TxType.NEVER || type == TxType.NOT_SUPPORTED);
  }

  protected void setAvailability(UserTransactionOperationsProvider userTransactionProvider,
      boolean available) {
    if (System.getSecurityManager() == null) {
      userTransactionProvider.setAvailability(available);
    } else {
      doPrivileged((PrivilegedAction<Object>) () -> {
        userTransactionProvider.setAvailability(available);
        return null;
      });
    }
  }

  protected boolean setUserTransactionAvailable(boolean available) {
    UserTransactionOperationsProvider userTransactionProvider =
        jtaPropertyManager.getJTAEnvironmentBean().getUserTransactionOperationsProvider();
    boolean previousUserTransactionAvailability = userTransactionProvider.getAvailability();
    setAvailability(userTransactionProvider, available);
    return previousUserTransactionAvailability;
  }

}
