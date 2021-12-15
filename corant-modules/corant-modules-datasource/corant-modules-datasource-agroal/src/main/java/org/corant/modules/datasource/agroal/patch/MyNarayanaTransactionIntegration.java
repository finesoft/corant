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

import static javax.transaction.Status.STATUS_COMMITTED;
import static javax.transaction.Status.STATUS_NO_TRANSACTION;
import static javax.transaction.Status.STATUS_ROLLEDBACK;
import static javax.transaction.Status.STATUS_UNKNOWN;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.sql.XAConnection;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.xa.XAResource;
import org.jboss.tm.XAResourceRecovery;
import org.jboss.tm.XAResourceRecoveryRegistry;
import io.agroal.api.transaction.TransactionAware;
import io.agroal.api.transaction.TransactionIntegration;
import io.agroal.narayana.BaseXAResource;
import io.agroal.narayana.ErrorConditionXAResource;
import io.agroal.narayana.RecoveryXAResource;

/**
 * corant-modules-datasource-agroal
 *
 * <p>
 * Code base from Agroal, if there is infringement, please inform me(finesoft@gmail.com). This class
 * is used to temporarily deal with the problem that the JTA transaction (Resource Local) timeout
 * the local transaction cannot be rolled back.
 *
 * @author bingo 下午4:09:30
 *
 */
public class MyNarayanaTransactionIntegration implements TransactionIntegration {

  // Use this cache as method references are not stable (they are used as bridge between
  // RecoveryConnectionFactory and XAResourceRecovery)
  private static final ConcurrentMap<ResourceRecoveryFactory, XAResourceRecovery> resourceRecoveryCache =
      new ConcurrentHashMap<>();

  private final TransactionManager transactionManager;

  private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;

  private final String jndiName;

  private final boolean connectable;

  private final XAResourceRecoveryRegistry recoveryRegistry;

  // In order to construct a UID that is globally unique, simply pair a UID with an InetAddress.
  private final UUID key = UUID.randomUUID();

  public MyNarayanaTransactionIntegration(TransactionManager transactionManager,
      TransactionSynchronizationRegistry transactionSynchronizationRegistry) {
    this(transactionManager, transactionSynchronizationRegistry, null, false);
  }

  public MyNarayanaTransactionIntegration(TransactionManager transactionManager,
      TransactionSynchronizationRegistry transactionSynchronizationRegistry, String jndiName) {
    this(transactionManager, transactionSynchronizationRegistry, jndiName, false);
  }

  public MyNarayanaTransactionIntegration(TransactionManager transactionManager,
      TransactionSynchronizationRegistry transactionSynchronizationRegistry, String jndiName,
      boolean connectable) {
    this(transactionManager, transactionSynchronizationRegistry, jndiName, connectable, null);
  }

  public MyNarayanaTransactionIntegration(TransactionManager transactionManager,
      TransactionSynchronizationRegistry transactionSynchronizationRegistry, String jndiName,
      boolean connectable, XAResourceRecoveryRegistry recoveryRegistry) {
    this.transactionManager = transactionManager; // NOSONAR
    this.transactionSynchronizationRegistry = transactionSynchronizationRegistry; // NOSONAR
    this.jndiName = jndiName;
    this.connectable = connectable;
    this.recoveryRegistry = recoveryRegistry; // NOSONAR
  }

  @Override
  public void addResourceRecoveryFactory(ResourceRecoveryFactory factory) {
    if (recoveryRegistry != null) {
      recoveryRegistry.addXAResourceRecovery(resourceRecoveryCache.computeIfAbsent(factory,
          f -> new AgroalXAResourceRecovery(f, jndiName)));
    }
  }

  @Override
  public void associate(TransactionAware transactionAware, XAResource xaResource)
      throws SQLException {
    try {
      if (transactionRunning()) {
        if (transactionSynchronizationRegistry.getResource(key) == null) {
          transactionSynchronizationRegistry
              .registerInterposedSynchronization(new InterposedSynchronization(transactionAware));
          transactionSynchronizationRegistry.putResource(key, transactionAware);

          XAResource xaResourceToEnlist;
          if (xaResource != null) {
            xaResourceToEnlist = new BaseXAResource(transactionAware, xaResource, jndiName);
          } else if (connectable) {
            xaResourceToEnlist = new MyConnectableLocalXAResource(transactionAware, jndiName);
          } else {
            xaResourceToEnlist = new MyLocalXAResource(transactionAware, jndiName);
          }
          transactionManager.getTransaction().enlistResource(xaResourceToEnlist);
        } else {
          transactionAware.transactionStart();
        }
      }
      transactionAware.transactionCheckCallback(this::transactionRunning);
    } catch (Exception e) {
      throw new SQLException("Exception in association of connection to existing transaction", e);
    }
  }

  @Override
  public boolean disassociate(TransactionAware connection) throws SQLException {
    if (transactionRunning()) {
      transactionSynchronizationRegistry.putResource(key, null);
    }
    return true;
  }

  @Override
  public TransactionAware getTransactionAware() throws SQLException {
    if (transactionRunning()) {
      return (TransactionAware) transactionSynchronizationRegistry.getResource(key);
    }
    return null;
  }

  // -- //

  @Override
  public void removeResourceRecoveryFactory(ResourceRecoveryFactory factory) {
    if (recoveryRegistry != null) {
      recoveryRegistry.removeXAResourceRecovery(resourceRecoveryCache.remove(factory));
    }
  }

  private boolean transactionRunning() throws SQLException {
    try {
      Transaction transaction = transactionManager.getTransaction();
      if (transaction == null) {
        return false;
      }
      int status = transaction.getStatus();
      return status != STATUS_UNKNOWN && status != STATUS_NO_TRANSACTION
          && status != STATUS_COMMITTED && status != STATUS_ROLLEDBACK;
      // other states are active transaction: ACTIVE, MARKED_ROLLBACK, PREPARING, PREPARED,
      // COMMITTING, ROLLING_BACK
    } catch (Exception e) {
      throw new SQLException("Exception in retrieving existing transaction", e);
    }
  }

  // --- //

  // This auxiliary class is a contraption due to the fact that XAResource is not closable.
  // It creates RecoveryXAResource wrappers that keeps track of lifecycle and closes the associated
  // connection.
  private static class AgroalXAResourceRecovery implements XAResourceRecovery {

    private static final XAResource[] EMPTY_RESOURCES = {};

    private final ResourceRecoveryFactory connectionFactory;
    private final String name;

    AgroalXAResourceRecovery(ResourceRecoveryFactory factory, String jndiName) {
      connectionFactory = factory;
      name = jndiName;
    }

    @Override
    public XAResource[] getXAResources() {
      XAConnection xaConnection = connectionFactory.getRecoveryConnection();
      try {
        return xaConnection == null ? EMPTY_RESOURCES
            : new XAResource[] {new RecoveryXAResource(xaConnection, name)};
      } catch (SQLException e) {
        return new XAResource[] {new ErrorConditionXAResource(xaConnection, e, name)};
      }
    }
  }

  private static class InterposedSynchronization implements Synchronization {

    private final TransactionAware transactionAware;

    InterposedSynchronization(TransactionAware transactionAware) {
      this.transactionAware = transactionAware;
    }

    @Override
    public void afterCompletion(int status) {
      // Return connection to the pool
      try {
        transactionAware.transactionEnd();
      } catch (SQLException ignore) {
        // ignore
      }
    }

    @Override
    public void beforeCompletion() {
      // nothing to do
    }
  }
}
