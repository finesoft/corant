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
package org.corant.suites.jta.shared;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.defaultObject;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.transaction.InvalidTransactionException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionRequiredException;
import javax.transaction.Transactional.TxType;
import javax.transaction.TransactionalException;
import org.corant.context.Instances;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.NotSupportedException;

/**
 * corant-suites-jta-shared
 *
 * Unfinish yet! This is an experiential function, used to handle transaction-related operations
 * manually.
 *
 * NOTE: Some code come from narayana, if there is infringement, please inform
 * me(finesoft@gmail.com).
 *
 * Thanks for the narayana team!
 *
 * @author bingo 上午9:45:50
 *
 */
public abstract class TransactionalAction<T> {

  static final Logger logger = Logger.getLogger(TransactionalAction.class.getName());

  final Supplier<T> supplier;
  final Class<?>[] rollbackOn;
  final Class<?>[] dontRollbackOn;
  final TxType type;
  final Synchronization synchronization;

  /**
   * @param type;
   * @param supplier
   * @param synchronization
   * @param rollbackOn
   * @param dontRollbackOn
   */
  protected TransactionalAction(TxType type, Supplier<T> supplier, Synchronization synchronization,
      Class<?>[] rollbackOn, Class<?>[] dontRollbackOn) {
    super();
    this.type = type;
    this.supplier = shouldNotNull(supplier, "The supplier can't null");
    this.synchronization = synchronization;
    this.rollbackOn = defaultObject(rollbackOn, new Class[0]);
    this.dontRollbackOn = defaultObject(dontRollbackOn, new Class[0]);
  }

  public T execute() throws Exception {
    final TransactionManager tm = TransactionService.transactionManager();
    final Transaction tx = TransactionService.currentTransaction();
    final Optional<UserTransactionActionHandler> helper =
        Instances.find(UserTransactionActionHandler.class);
    boolean prestatus = false;
    if (helper.isPresent()) {
      prestatus = helper.get().beforeExecute(tx, tm, type, rollbackOn, dontRollbackOn);
    }
    try {
      return doExecute(tm, tx);
    } finally {
      if (helper.isPresent()) {
        helper.get().afterExecute(prestatus, tx, tm, type);
      }
    }
  }

  protected abstract T doExecute(TransactionManager tm, Transaction tx) throws Exception;

  protected void endTransaction(TransactionManager tm, Transaction tx) throws Exception {
    if (tx != tm.getTransaction()) {
      throw new CorantRuntimeException("Wrong transaction on thread");
    }

    if (tx.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
      tm.rollback();
    } else {
      tm.commit();
    }
  }

  protected T executeInCallerTx(Transaction tx) throws Exception {
    try {
      if (synchronization != null) {
        tx.registerSynchronization(synchronization);
      }
      return supplier.get();
    } catch (Exception e) {
      handleException(e, tx);
    }
    throw new CorantRuntimeException("UNREACHABLE");
  }

  protected T executeInNoTx() throws Exception {
    return supplier.get();
  }

  protected T executeInOurTx(TransactionManager tm) throws Exception {
    tm.begin();
    Transaction tx = tm.getTransaction();
    try {
      if (synchronization != null) {
        tx.registerSynchronization(synchronization);
      }
      return supplier.get();
    } catch (Exception e) {
      handleException(e, tx);
    } finally {
      endTransaction(tm, tx);
    }
    throw new CorantRuntimeException("UNREACHABLE");
  }

  protected void handleException(Exception e, Transaction tx) throws Exception {
    for (Class<?> dontRollbackOnClass : dontRollbackOn) {
      if (dontRollbackOnClass.isAssignableFrom(e.getClass())) {
        throw e;
      }
    }

    for (Class<?> rollbackOnClass : rollbackOn) {
      if (rollbackOnClass.isAssignableFrom(e.getClass())) {
        tx.setRollbackOnly();
        throw e;
      }
    }

    if (e instanceof RuntimeException) {
      tx.setRollbackOnly();
      throw e;
    }

    throw e;
  }

  /**
   * corant-suites-jta-shared
   *
   * @author bingo 上午10:45:40
   *
   */
  public static class MandatoryTransactionalAction<T> extends TransactionalAction<T> {

    protected MandatoryTransactionalAction(Supplier<T> supplier, Synchronization synchronization,
        Class<?>[] rollbackOn, Class<?>[] dontRollbackOn) {
      super(TxType.MANDATORY, supplier, synchronization, rollbackOn, dontRollbackOn);
    }

    @Override
    protected T doExecute(TransactionManager tm, Transaction tx) throws Exception {
      if (tx == null) {
        throw new TransactionalException("Transaction is required for execution",
            new TransactionRequiredException());
      }
      return executeInCallerTx(tx);
    }
  }

  /**
   * corant-suites-jta-shared
   *
   * @author bingo 上午10:46:08
   *
   */
  public static class NeverTransactionalAction<T> extends TransactionalAction<T> {

    protected NeverTransactionalAction(Supplier<T> supplier, Synchronization synchronization,
        Class<?>[] rollbackOn, Class<?>[] dontRollbackOn) {
      super(TxType.NEVER, supplier, synchronization, rollbackOn, dontRollbackOn);
    }

    @Override
    protected T doExecute(TransactionManager tm, Transaction tx) throws Exception {
      if (tx != null) {
        throw new TransactionalException("Don't be called inside a transaction context.",
            new InvalidTransactionException());
      }
      return executeInCallerTx(tx);
    }
  }

  /**
   * corant-suites-jta-shared
   *
   * @author bingo 上午10:45:44
   *
   */
  public static class NotSupportedTransactionalAction<T> extends TransactionalAction<T> {

    protected NotSupportedTransactionalAction(Supplier<T> supplier, Synchronization synchronization,
        Class<?>[] rollbackOn, Class<?>[] dontRollbackOn) {
      super(TxType.NOT_SUPPORTED, supplier, synchronization, rollbackOn, dontRollbackOn);
    }

    @Override
    protected T doExecute(TransactionManager tm, Transaction tx) throws Exception {
      if (tx != null) {
        tm.suspend();
        try {
          return executeInNoTx();
        } finally {
          tm.resume(tx);
        }
      } else {
        return executeInNoTx();
      }
    }
  }

  /**
   * corant-suites-jta-shared
   *
   * @author bingo 上午10:45:49
   *
   */
  public static class RequiredTransactionalAction<T> extends TransactionalAction<T> {

    protected RequiredTransactionalAction(Supplier<T> supplier, Synchronization synchronization,
        Class<?>[] rollbackOn, Class<?>[] dontRollbackOn) {
      super(TxType.REQUIRED, supplier, synchronization, rollbackOn, dontRollbackOn);
    }

    @Override
    protected T doExecute(TransactionManager tm, Transaction tx) throws Exception {
      if (tx == null) {
        return executeInOurTx(tm);
      } else {
        return executeInCallerTx(tx);
      }
    }
  }

  /**
   * corant-suites-jta-shared
   *
   * @author bingo 上午10:45:52
   *
   */
  public static class RequiresNewTransactionalAction<T> extends TransactionalAction<T> {

    protected RequiresNewTransactionalAction(Supplier<T> supplier, Synchronization synchronization,
        Class<?>[] rollbackOn, Class<?>[] dontRollbackOn) {
      super(TxType.REQUIRES_NEW, supplier, synchronization, rollbackOn, dontRollbackOn);
    }

    @Override
    protected T doExecute(TransactionManager tm, Transaction tx) throws Exception {
      if (tx != null) {
        tm.suspend();
        try {
          return executeInOurTx(tm);
        } finally {
          tm.resume(tx);
        }
      } else {
        return executeInOurTx(tm);
      }
    }
  }

  /**
   * corant-suites-jta-shared
   *
   * @author bingo 上午10:46:01
   *
   */
  public static class SupportsTransactionalAction<T> extends TransactionalAction<T> {

    protected SupportsTransactionalAction(Supplier<T> supplier, Synchronization synchronization,
        Class<?>[] rollbackOn, Class<?>[] dontRollbackOn) {
      super(TxType.SUPPORTS, supplier, synchronization, rollbackOn, dontRollbackOn);
    }

    @Override
    protected T doExecute(TransactionManager tm, Transaction tx) throws Exception {
      if (tx == null) {
        return executeInNoTx();
      } else {
        return executeInCallerTx(tx);
      }
    }
  }

  /**
   * corant-suites-jta-shared
   *
   * @author bingo 上午10:45:58
   *
   */
  public static class TransactionalActuator<T> {

    Class<?>[] rollbackOn = new Class[0];
    Class<?>[] dontRollbackOn = new Class[0];
    TxType txType = TxType.REQUIRED;
    Synchronization synchronization;

    public TransactionalActuator<T> dontRollbackOn(final Class<?>... dontRollbackOn) {
      this.dontRollbackOn = dontRollbackOn;
      return this;
    }

    public T get(final Supplier<T> supplier) {
      try {
        return plan(shouldNotNull(supplier)).execute();
      } catch (Exception e) {
        throw new CorantRuntimeException(e);
      }
    }

    public TransactionalActuator<T> mandatory() {
      return txType(TxType.MANDATORY);
    }

    public TransactionalActuator<T> never() {
      return txType(TxType.NEVER);
    }

    public TransactionalActuator<T> notSupported() {
      return txType(TxType.NOT_SUPPORTED);
    }

    public TransactionalActuator<T> required() {
      return txType(TxType.REQUIRED);
    }

    public TransactionalActuator<T> requiresNew() {
      return txType(TxType.REQUIRES_NEW);
    }

    public TransactionalActuator<T> rollbackOn(final Class<?>... rollbackOn) {
      this.rollbackOn = rollbackOn;
      return this;
    }

    public void run(final Runnable runner) {
      shouldNotNull(runner);
      try {
        plan(() -> {
          runner.run();
          return null;
        }).execute();
      } catch (Exception e) {
        throw new CorantRuntimeException(e);
      }
    }

    public TransactionalActuator<T> supports() {
      return txType(TxType.SUPPORTS);
    }

    public TransactionalActuator<T> synchronization(final Synchronization synchronization) {
      if ((txType == TxType.NEVER || txType == TxType.NOT_SUPPORTED) && synchronization != null) {
        throw new NotSupportedException();
      }
      this.synchronization = synchronization;
      return this;
    }

    public TransactionalActuator<T> txType(final TxType txType) {
      if (synchronization != null && (txType == TxType.NEVER || txType == TxType.NOT_SUPPORTED)) {
        throw new NotSupportedException();
      }
      this.txType = defaultObject(txType, TxType.REQUIRED);
      return this;
    }

    protected TransactionalAction<T> plan(final Supplier<T> supplier) {
      switch (txType) {
        case MANDATORY:
          return new MandatoryTransactionalAction<>(supplier, synchronization, rollbackOn,
              dontRollbackOn);
        case REQUIRES_NEW:
          return new RequiresNewTransactionalAction<>(supplier, synchronization, rollbackOn,
              dontRollbackOn);
        case NEVER:
          return new NeverTransactionalAction<>(supplier, synchronization, rollbackOn,
              dontRollbackOn);
        case NOT_SUPPORTED:
          return new NotSupportedTransactionalAction<>(supplier, synchronization, rollbackOn,
              dontRollbackOn);
        case SUPPORTS:
          return new SupportsTransactionalAction<>(supplier, synchronization, rollbackOn,
              dontRollbackOn);
        default:
          return new RequiredTransactionalAction<>(supplier, synchronization, rollbackOn,
              dontRollbackOn);
      }
    }
  }

  public interface UserTransactionActionHandler {

    void afterExecute(boolean prestatus, Transaction tx, TransactionManager tm, TxType type);

    boolean beforeExecute(Transaction tx, TransactionManager tm, TxType type, Class<?>[] rollbackOn,
        Class<?>[] dontRollbackOn);
  }
}
