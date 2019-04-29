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
package org.corant.suites.jms.shared;

import static javax.transaction.Status.STATUS_ACTIVE;
import static javax.transaction.Status.STATUS_MARKED_ROLLBACK;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.corant.Corant;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-suites-jms-shared
 *
 * @author bingo 上午12:45:00
 *
 */
public class Transactions {

  static Transaction currentTransaction() {
    if (Corant.instance().select(TransactionManager.class).isResolvable()) {
      try {
        Transaction tx = Corant.instance().select(TransactionManager.class).get().getTransaction();
        return tx;
      } catch (SystemException e) {
        throw new CorantRuntimeException(e);
      }
    }
    return null;
  }

  static boolean isInTransaction() {
    Transaction tx = currentTransaction();
    try {
      return tx != null
          && (tx.getStatus() == STATUS_ACTIVE || tx.getStatus() == STATUS_MARKED_ROLLBACK);
    } catch (SystemException e) {
      throw new CorantRuntimeException(e);
    }
  }
}
