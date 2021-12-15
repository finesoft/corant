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
package org.corant.modules.datasource.agroal.patch;

import static javax.transaction.xa.XAException.XAER_INVAL;
import static javax.transaction.xa.XAException.XAER_NOTA;
import static javax.transaction.xa.XAException.XAER_RMERR;
import static javax.transaction.xa.XAException.XA_RBROLLBACK;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.jboss.tm.XAResourceWrapper;
import io.agroal.api.transaction.TransactionAware;
import io.agroal.pool.ConnectionHandler;

/**
 * corant-modules-datasource-agroal
 *
 * <p>
 * Code base from Agroal, if there is infringement, please inform me(finesoft@gmail.com). This class
 * is used to temporarily deal with the problem that the JTA transaction (Resource Local) timeout
 * the local transaction cannot be rolled back.
 *
 * @author bingo 下午4:13:35
 *
 */
public class MyLocalXAResource implements XAResourceWrapper {

  private static final String PRODUCT_NAME =
      MyLocalXAResource.class.getPackage().getImplementationTitle();
  private static final String PRODUCT_VERSION =
      MyLocalXAResource.class.getPackage().getImplementationVersion();

  private final TransactionAware transactionAware;
  private final String jndiName;
  private Xid currentXid;

  public MyLocalXAResource(TransactionAware transactionAware, String jndiName) {
    this.transactionAware = transactionAware; // NOSONAR
    this.jndiName = jndiName;
  }

  private static XAException xaException(int errorCode, String message) {
    XAException xaException = new XAException(message);
    xaException.errorCode = errorCode;
    return xaException;
  }

  private static XAException xaException(int errorCode, String message, Throwable cause) {
    XAException xaException = xaException(errorCode, message + cause.getMessage());
    xaException.initCause(cause);
    return xaException;
  }

  @Override
  public void commit(Xid xid, boolean onePhase) throws XAException {
    if (xid == null || !xid.equals(currentXid)) {
      throw xaException(XAER_NOTA, "Invalid xid to transactionCommit");
    }

    currentXid = null;
    try {
      transactionAware.transactionBeforeCompletion(true);
      transactionAware.transactionCommit();
    } catch (Exception t) {
      transactionAware.setFlushOnly();
      throw xaException(onePhase ? XA_RBROLLBACK : XAER_RMERR,
          "Error trying to transactionCommit local transaction: ", t);
    }
  }

  @Override
  public void end(Xid xid, int flags) throws XAException {
    if (xid == null || !xid.equals(currentXid)) {
      transactionAware.setFlushOnly();
      throw xaException(XAER_NOTA, "Invalid xid to transactionEnd");
    }
  }

  @Override
  public void forget(Xid xid) throws XAException {
    transactionAware.setFlushOnly();
    throw xaException(XAER_NOTA, "Forget not supported in local XA resource");
  }

  public Object getConnection() throws Throwable {
    return transactionAware.getConnection();
  }

  @Override
  public String getJndiName() {
    return jndiName;
  }

  @Override
  public String getProductName() {
    return PRODUCT_NAME;
  }

  @Override
  public String getProductVersion() {
    return PRODUCT_VERSION;
  }

  @Override
  public XAResource getResource() {
    return this;
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
    transactionAware.setFlushOnly();
    throw xaException(XAER_RMERR, "No recover in local XA resource");
  }

  @Override
  public void rollback(Xid xid) throws XAException {
    if (xid == null || !xid.equals(currentXid)) {
      throw xaException(XAER_NOTA, "Invalid xid to transactionRollback");
    }

    currentXid = null;
    try {
      transactionAware.transactionBeforeCompletion(false);
      // Forced to pretend that it is still in the transaction, it is convenient to use the Resource
      // Local transaction timeout, the database connection can be rolled back normally
      if (transactionAware instanceof ConnectionHandler
          && ((ConnectionHandler) transactionAware).isEnlisted()) {
        transactionAware.transactionCheckCallback(() -> true);
      }
      transactionAware.transactionRollback();
    } catch (Exception t) {
      transactionAware.setFlushOnly();
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
        transactionAware.transactionStart();
      } catch (Exception t) {
        transactionAware.setFlushOnly();
        throw xaException(XAER_RMERR, "Error trying to start local transaction: ", t);
      }
      currentXid = xid;
    } else if (flags != TMJOIN && flags != TMRESUME) {
      throw xaException(XAException.XAER_DUPID, "Invalid flag for join|resume");
    }
  }
}
