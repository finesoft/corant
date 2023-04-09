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
package org.corant.modules.jta.shared;

import static javax.transaction.Status.STATUS_COMMITTED;
import static javax.transaction.Status.STATUS_NO_TRANSACTION;
import static javax.transaction.Status.STATUS_ROLLEDBACK;
import static javax.transaction.Status.STATUS_UNKNOWN;
import static javax.transaction.xa.XAException.XAER_INVAL;
import static javax.transaction.xa.XAException.XAER_NOTA;
import static javax.transaction.xa.XAException.XAER_RMERR;
import static javax.transaction.xa.XAException.XA_RBROLLBACK;
import java.sql.SQLException;
import java.util.UUID;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * corant-modules-jta-shared
 *
 * @author bingo 下午2:26:37
 *
 */
public interface TransactionIntegrator {

  static XAException xaException(int errorCode, String message) {
    XAException xaException = new XAException(message);
    xaException.errorCode = errorCode;
    return xaException;
  }

  static XAException xaException(int errorCode, String message, Throwable cause) {
    XAException xaException = xaException(errorCode, message + cause.getMessage());
    xaException.initCause(cause);
    return xaException;
  }

  void associate(TransactionAwareness transactionAware, XAResource xaResource) throws SQLException;

  boolean disassociate(TransactionAwareness transactionAware) throws SQLException;

  TransactionAwareness getTransactionAwareness() throws SQLException;

  boolean isTransactionRunning() throws SQLException;

  /**
   * corant-modules-jta-shared
   *
   * @author bingo 下午3:00:20
   *
   */
  class LocalXAResource implements XAResource {
    private final TransactionAwareness transactionAwareness;
    private Xid currentXid;

    public LocalXAResource(TransactionAwareness transactionAwareness) {
      this.transactionAwareness = transactionAwareness; // NOSONAR
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
      if (xid == null || !xid.equals(currentXid)) {
        throw xaException(XAER_NOTA, "Invalid xid to transactionCommit");
      }

      currentXid = null;
      try {
        transactionAwareness.transactionBeforeCompletion(true);
        transactionAwareness.transactionCommit();
      } catch (Exception t) {
        throw xaException(onePhase ? XA_RBROLLBACK : XAER_RMERR,
            "Error trying to transactionCommit local transaction: ", t);
      }
    }

    @Override
    public void end(Xid xid, int flags) throws XAException {
      if (xid == null || !xid.equals(currentXid)) {
        throw xaException(XAER_NOTA, "Invalid xid to transactionEnd");
      }
    }

    @Override
    public void forget(Xid xid) throws XAException {
      throw xaException(XAER_NOTA, "Forget not supported in local XA resource");
    }

    public Object getConnection() throws Throwable {
      return transactionAwareness.getConnection();
    }

    @Override
    public int getTransactionTimeout() throws XAException {
      return 0;
    }

    @Override
    public boolean isSameRM(XAResource xaResource) throws XAException {
      return this == xaResource;
    }

    @Override
    public int prepare(Xid xid) throws XAException {
      return XA_OK;
    }

    // --- XA Resource Wrapper //

    @Override
    public Xid[] recover(int flags) throws XAException {
      throw xaException(XAER_RMERR, "No recover in local XA resource");
    }

    @Override
    public void rollback(Xid xid) throws XAException {
      if (xid == null || !xid.equals(currentXid)) {
        throw xaException(XAER_NOTA, "Invalid xid to transactionRollback");
      }
      currentXid = null;
      try {
        transactionAwareness.transactionBeforeCompletion(false);
        transactionAwareness.transactionRollback();
      } catch (Exception t) {
        throw xaException(XAER_RMERR, "Error trying to transactionRollback local transaction: ", t);
      }
    }

    @Override
    public boolean setTransactionTimeout(int timeout) throws XAException {
      return false;
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
      if (currentXid == null) {
        if (flags != TMNOFLAGS) {
          throw xaException(XAER_INVAL, "Starting resource with wrong flags");
        }
        try {
          transactionAwareness.transactionStart();
        } catch (Exception t) {
          throw xaException(XAER_RMERR, "Error trying to start local transaction: ", t);
        }
        currentXid = xid;
      } else if (flags != TMJOIN && flags != TMRESUME) {
        throw xaException(XAException.XAER_DUPID, "Invalid flag for join|resume");
      }
    }
  }

  /**
   * corant-modules-jta-shared
   *
   * @author bingo 下午2:59:56
   *
   */
  class LocalXATransactionIntegrator implements TransactionIntegrator {

    protected final TransactionManager transactionManager;

    protected final TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    protected final UUID key = UUID.randomUUID();

    public LocalXATransactionIntegrator(TransactionManager transactionManager,
        TransactionSynchronizationRegistry transactionSynchronizationRegistry) {
      this.transactionManager = transactionManager;
      this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
    }

    @Override
    public void associate(TransactionAwareness transactionAwareness, XAResource xaResource)
        throws SQLException {
      try {
        if (isTransactionRunning()) {
          if (transactionSynchronizationRegistry.getResource(key) == null) {
            transactionSynchronizationRegistry
                .registerInterposedSynchronization(SynchronizationAdapter.afterCompletion(() -> {
                  try {
                    transactionAwareness.transactionEnd();
                    disassociate(transactionAwareness);
                  } catch (Exception e) {
                  }
                }));
            transactionSynchronizationRegistry.putResource(key, transactionAwareness);
            XAResource xaResourceToEnlist = resolveXAResource(transactionAwareness, xaResource);
            transactionManager.getTransaction().enlistResource(xaResourceToEnlist);
          } else {
            transactionAwareness.transactionStart();
          }
        }
      } catch (Exception e) {
        throw new SQLException("Exception in association of connection to existing transaction", e);
      }
    }

    @Override
    public boolean disassociate(TransactionAwareness transactionAware) throws SQLException {
      if (isTransactionRunning()) {
        transactionSynchronizationRegistry.putResource(key, null);
      }
      return true;
    }

    @Override
    public TransactionAwareness getTransactionAwareness() throws SQLException {
      if (isTransactionRunning()) {
        return (TransactionAwareness) transactionSynchronizationRegistry.getResource(key);
      }
      return null;
    }

    @Override
    public boolean isTransactionRunning() throws SQLException {
      try {
        Transaction transaction = transactionManager.getTransaction();
        if (transaction == null) {
          return false;
        }
        int status = transaction.getStatus();
        return status != STATUS_UNKNOWN && status != STATUS_NO_TRANSACTION
            && status != STATUS_COMMITTED && status != STATUS_ROLLEDBACK;
      } catch (Exception e) {
        throw new SQLException("Exception in retrieving existing transaction", e);
      }
    }

    protected XAResource resolveXAResource(TransactionAwareness transactionAwareness,
        XAResource xaResource) {
      return xaResource == null ? new LocalXAResource(transactionAwareness) : xaResource;
    }

  }
}
