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
package org.corant.modules.jta.shared;

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
import org.corant.context.Beans;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.util.Classes;

/**
 * corant-modules-jta-shared
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
  final Integer timeout;

  /**
   * Return a convenient transaction execution object.
   *
   * @param type the TX type
   * @param supplier the customized program executed in this transaction
   * @param synchronization the synchronization code used to register to the current transaction
   * @param rollbackOn the rollbackOn element can be set to indicate exceptions that must cause the
   *        interceptor to mark the transaction for roll back.
   * @param dontRollbackOn the dontRollbackOn element can be set to indicate exceptions that must
   *        not cause the interceptor to mark the transaction for roll back.
   * @param timeout the timeout value (in seconds) that is associated with transactions started by
   *        the current thread with the begin method
   */
  protected TransactionalAction(TxType type, Supplier<T> supplier, Synchronization synchronization,
      Class<?>[] rollbackOn, Class<?>[] dontRollbackOn, Integer timeout) {
    this.type = type;
    this.supplier = shouldNotNull(supplier, "The supplier can't null");
    this.synchronization = synchronization;
    this.rollbackOn = defaultObject(rollbackOn, Classes.EMPTY_ARRAY);
    this.dontRollbackOn = defaultObject(dontRollbackOn, Classes.EMPTY_ARRAY);
    this.timeout = timeout;
  }

  public T execute() throws Exception {
    final TransactionManager tm = TransactionService.transactionManager();
    if (timeout != null && timeout > 0) {
      tm.setTransactionTimeout(timeout);
    }
    final Transaction tx = TransactionService.currentTransaction();
    final Optional<UserTransactionActionHandler> helper =
        Beans.find(UserTransactionActionHandler.class);
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
      if (synchronization != null && tx != null) {
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
   * corant-modules-jta-shared
   *
   * @author bingo 上午10:45:40
   *
   */
  public static class MandatoryTransactionalAction<T> extends TransactionalAction<T> {

    protected MandatoryTransactionalAction(Supplier<T> supplier, Synchronization synchronization,
        Class<?>[] rollbackOn, Class<?>[] dontRollbackOn, Integer timeout) {
      super(TxType.MANDATORY, supplier, synchronization, rollbackOn, dontRollbackOn, timeout);
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
   * corant-modules-jta-shared
   *
   * @author bingo 上午10:46:08
   *
   */
  public static class NeverTransactionalAction<T> extends TransactionalAction<T> {

    protected NeverTransactionalAction(Supplier<T> supplier, Synchronization synchronization,
        Class<?>[] rollbackOn, Class<?>[] dontRollbackOn, Integer timeout) {
      super(TxType.NEVER, supplier, synchronization, rollbackOn, dontRollbackOn, timeout);
    }

    @Override
    protected T doExecute(TransactionManager tm, Transaction tx) throws Exception {
      if (tx != null) {
        throw new TransactionalException("Don't be called inside a transaction context.",
            new InvalidTransactionException());
      }
      return executeInCallerTx(null);
    }
  }

  /**
   * corant-modules-jta-shared
   *
   * @author bingo 上午10:45:44
   *
   */
  public static class NotSupportedTransactionalAction<T> extends TransactionalAction<T> {

    protected NotSupportedTransactionalAction(Supplier<T> supplier, Synchronization synchronization,
        Class<?>[] rollbackOn, Class<?>[] dontRollbackOn, Integer timeout) {
      super(TxType.NOT_SUPPORTED, supplier, synchronization, rollbackOn, dontRollbackOn, timeout);
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
   * corant-modules-jta-shared
   *
   * @author bingo 上午10:45:49
   *
   */
  public static class RequiredTransactionalAction<T> extends TransactionalAction<T> {

    protected RequiredTransactionalAction(Supplier<T> supplier, Synchronization synchronization,
        Class<?>[] rollbackOn, Class<?>[] dontRollbackOn, Integer timeout) {
      super(TxType.REQUIRED, supplier, synchronization, rollbackOn, dontRollbackOn, timeout);
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
   * corant-modules-jta-shared
   *
   * @author bingo 上午10:45:52
   *
   */
  public static class RequiresNewTransactionalAction<T> extends TransactionalAction<T> {

    protected RequiresNewTransactionalAction(Supplier<T> supplier, Synchronization synchronization,
        Class<?>[] rollbackOn, Class<?>[] dontRollbackOn, Integer timeout) {
      super(TxType.REQUIRES_NEW, supplier, synchronization, rollbackOn, dontRollbackOn, timeout);
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
   * corant-modules-jta-shared
   *
   * @author bingo 上午10:46:01
   *
   */
  public static class SupportsTransactionalAction<T> extends TransactionalAction<T> {

    protected SupportsTransactionalAction(Supplier<T> supplier, Synchronization synchronization,
        Class<?>[] rollbackOn, Class<?>[] dontRollbackOn, Integer timeout) {
      super(TxType.SUPPORTS, supplier, synchronization, rollbackOn, dontRollbackOn, timeout);
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
   * corant-modules-jta-shared
   *
   * <p>
   * The simplifies programmatic transaction execution by wrapping the transaction manager and
   * providing some convenient methods around which a transactional boundary is started.
   *
   * @author bingo 上午10:45:58
   *
   */
  public static class TransactionalActuator<T> {

    Class<?>[] rollbackOn = Classes.EMPTY_ARRAY;
    Class<?>[] dontRollbackOn = Classes.EMPTY_ARRAY;
    TxType txType = TxType.REQUIRED;
    Synchronization synchronization;
    Integer timeout;

    public TransactionalActuator<T> dontRollbackOn(final Class<?>... dontRollbackOn) {
      this.dontRollbackOn = dontRollbackOn;
      return this;
    }

    /**
     * Execute the given Supplier in the transaction context and return the result.
     *
     * @param supplier the execution
     */
    public T get(final Supplier<T> supplier) {
      try {
        return plan(shouldNotNull(supplier)).execute();
      } catch (Exception e) {
        throw new CorantRuntimeException(e);
      }
    }

    /**
     * Specify TxType as TxType.MANDATORY, which is equivalent to using annotation
     * {@code @Transactional(TxType.MANDATORY)}
     *
     * @see TxType#MANDATORY
     */
    public TransactionalActuator<T> mandatory() {
      return txType(TxType.MANDATORY);
    }

    /**
     * Specify TxType as TxType.NEVER, which is equivalent to using annotation
     * {@code @Transactional(TxType.NEVER)}
     *
     * @see TxType#NEVER
     */
    public TransactionalActuator<T> never() {
      return txType(TxType.NEVER);
    }

    /**
     * Specify TxType as TxType.NOT_SUPPORTED, which is equivalent to using annotation
     * {@code @Transactional(TxType.NOT_SUPPORTED)}
     *
     * @see TxType#NOT_SUPPORTED
     */
    public TransactionalActuator<T> notSupported() {
      return txType(TxType.NOT_SUPPORTED);
    }

    /**
     * Specify TxType as TxType.REQUIRED, which is equivalent to using annotation
     * {@code @Transactional(TxType.REQUIRED)}
     *
     * @see TxType#REQUIRED
     */
    public TransactionalActuator<T> required() {
      return txType(TxType.REQUIRED);
    }

    /**
     * Specify TxType as TxType.REQUIRES_NEW, which is equivalent to using annotation
     * {@code @Transactional(TxType.REQUIRES_NEW)}
     *
     * @see TxType#REQUIRES_NEW
     */
    public TransactionalActuator<T> requiresNew() {
      return txType(TxType.REQUIRES_NEW);
    }

    public TransactionalActuator<T> rollbackOn(final Class<?>... rollbackOn) {
      this.rollbackOn = rollbackOn;
      return this;
    }

    /**
     * Execute the given Runnable in the transaction context
     *
     * @param runner the execution
     */
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

    /**
     * Specify TxType as TxType.SUPPORTS, which is equivalent to using annotation
     * {@code @Transactional(TxType.SUPPORTS)}
     *
     * @see TxType#SUPPORTS
     */
    public TransactionalActuator<T> supports() {
      return txType(TxType.SUPPORTS);
    }

    public TransactionalActuator<T> synchronization(final Synchronization synchronization) {
      if ((txType == TxType.NEVER || txType == TxType.NOT_SUPPORTED) && synchronization != null) {
        throw new NotSupportedException(
            "The synchronization contained in the current actuator may not execute correctly!");
      }
      this.synchronization = synchronization;
      return this;
    }

    /**
     * Set the timeout value(in seconds) that is associated with transactions started by this
     * actuator.
     *
     * @param timeout The value of the timeout in seconds. If the value is zero,the transaction
     *        service restores the default value. If the value is negative a SystemException is
     *        thrown.
     */
    public TransactionalActuator<T> timeout(final int timeout) {
      this.timeout = timeout;
      return this;
    }

    /**
     * Specify the {@link TxType} value of this actuator, so that the actuator can execute
     * corresponding actions within the transaction context according to the given TxType value
     *
     * @param txType the value of TxType
     */
    public TransactionalActuator<T> txType(final TxType txType) {
      if (synchronization != null && (txType == TxType.NEVER || txType == TxType.NOT_SUPPORTED)) {
        throw new NotSupportedException(
            "The synchronization contained in the current actuator may not execute correctly!");
      }
      this.txType = defaultObject(txType, TxType.REQUIRED);
      return this;
    }

    protected TransactionalAction<T> plan(final Supplier<T> supplier) {
      switch (txType) {
        case MANDATORY:
          return new MandatoryTransactionalAction<>(supplier, synchronization, rollbackOn,
              dontRollbackOn, timeout);
        case REQUIRES_NEW:
          return new RequiresNewTransactionalAction<>(supplier, synchronization, rollbackOn,
              dontRollbackOn, timeout);
        case NEVER:
          return new NeverTransactionalAction<>(supplier, synchronization, rollbackOn,
              dontRollbackOn, timeout);
        case NOT_SUPPORTED:
          return new NotSupportedTransactionalAction<>(supplier, synchronization, rollbackOn,
              dontRollbackOn, timeout);
        case SUPPORTS:
          return new SupportsTransactionalAction<>(supplier, synchronization, rollbackOn,
              dontRollbackOn, timeout);
        default:
          return new RequiredTransactionalAction<>(supplier, synchronization, rollbackOn,
              dontRollbackOn, timeout);
      }
    }
  }

  public interface UserTransactionActionHandler {

    void afterExecute(boolean prestatus, Transaction tx, TransactionManager tm, TxType type);

    boolean beforeExecute(Transaction tx, TransactionManager tm, TxType type, Class<?>[] rollbackOn,
        Class<?>[] dontRollbackOn);
  }
}
