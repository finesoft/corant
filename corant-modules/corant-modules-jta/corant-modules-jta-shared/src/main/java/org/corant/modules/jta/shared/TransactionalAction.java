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
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.transaction.InvalidTransactionException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
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
public abstract class TransactionalAction {

  static final Logger logger = Logger.getLogger(TransactionalAction.class.getName());

  final Supplier<?> supplier;
  final Class<?>[] rollbackOn;
  final Class<?>[] dontRollbackOn;
  final TxType type;
  final Synchronization synchronization;
  final Integer timeout;
  final boolean rollbackOnly;

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
   * @param rollbackOnly Modify the transaction associated with the target object such that the only
   *        possible outcome of the transaction is to roll back the transaction.
   */
  protected TransactionalAction(TxType type, Supplier<?> supplier, Synchronization synchronization,
      Class<?>[] rollbackOn, Class<?>[] dontRollbackOn, Integer timeout, boolean rollbackOnly) {
    this.type = type;
    this.supplier = shouldNotNull(supplier, "The supplier can't null");
    this.synchronization = synchronization;
    this.rollbackOn = defaultObject(rollbackOn, Classes.EMPTY_ARRAY);
    this.dontRollbackOn = defaultObject(dontRollbackOn, Classes.EMPTY_ARRAY);
    this.timeout = timeout;
    this.rollbackOnly = rollbackOnly;
  }

  public Object execute() throws Exception {
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

  protected abstract Object doExecute(TransactionManager tm, Transaction tx) throws Exception;

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

  protected Object executeInCallerTx(Transaction tx) throws Exception {
    try {
      return supply(tx);
    } catch (Exception e) {
      handleException(e, tx);
    }
    throw new CorantRuntimeException("UNREACHABLE");
  }

  protected Object executeInNoTx() throws Exception {
    return supplier.get();
  }

  protected Object executeInOurTx(TransactionManager tm) throws Exception {
    tm.begin();
    Transaction tx = tm.getTransaction();
    try {
      return supply(tx);
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
    }

    throw e;
  }

  protected Object supply(Transaction tx)
      throws IllegalStateException, SystemException, RollbackException {
    if (tx != null && synchronization != null) {
      tx.registerSynchronization(synchronization);
    }
    Object result = supplier.get();
    if (tx != null && rollbackOnly) {
      tx.setRollbackOnly();
    }
    return result;
  }

  /**
   * corant-modules-jta-shared
   *
   * @author bingo 上午10:45:40
   *
   */
  public static class MandatoryTransactionalAction extends TransactionalAction {

    protected MandatoryTransactionalAction(Supplier<?> supplier, Synchronization synchronization,
        Class<?>[] rollbackOn, Class<?>[] dontRollbackOn, Integer timeout, boolean rollbackOnly) {
      super(TxType.MANDATORY, supplier, synchronization, rollbackOn, dontRollbackOn, timeout,
          rollbackOnly);
    }

    @Override
    protected Object doExecute(TransactionManager tm, Transaction tx) throws Exception {
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
  public static class NeverTransactionalAction extends TransactionalAction {

    protected NeverTransactionalAction(Supplier<?> supplier, Synchronization synchronization,
        Class<?>[] rollbackOn, Class<?>[] dontRollbackOn, Integer timeout, boolean rollbackOnly) {
      super(TxType.NEVER, supplier, synchronization, rollbackOn, dontRollbackOn, timeout,
          rollbackOnly);
    }

    @Override
    protected Object doExecute(TransactionManager tm, Transaction tx) throws Exception {
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
  public static class NotSupportedTransactionalAction extends TransactionalAction {

    protected NotSupportedTransactionalAction(Supplier<?> supplier, Synchronization synchronization,
        Class<?>[] rollbackOn, Class<?>[] dontRollbackOn, Integer timeout, boolean rollbackOnly) {
      super(TxType.NOT_SUPPORTED, supplier, synchronization, rollbackOn, dontRollbackOn, timeout,
          rollbackOnly);
    }

    @Override
    protected Object doExecute(TransactionManager tm, Transaction tx) throws Exception {
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
  public static class RequiredTransactionalAction extends TransactionalAction {

    protected RequiredTransactionalAction(Supplier<?> supplier, Synchronization synchronization,
        Class<?>[] rollbackOn, Class<?>[] dontRollbackOn, Integer timeout, boolean rollbackOnly) {
      super(TxType.REQUIRED, supplier, synchronization, rollbackOn, dontRollbackOn, timeout,
          rollbackOnly);
    }

    @Override
    protected Object doExecute(TransactionManager tm, Transaction tx) throws Exception {
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
  public static class RequiresNewTransactionalAction extends TransactionalAction {

    protected RequiresNewTransactionalAction(Supplier<?> supplier, Synchronization synchronization,
        Class<?>[] rollbackOn, Class<?>[] dontRollbackOn, Integer timeout, boolean rollbackOnly) {
      super(TxType.REQUIRES_NEW, supplier, synchronization, rollbackOn, dontRollbackOn, timeout,
          rollbackOnly);
    }

    @Override
    protected Object doExecute(TransactionManager tm, Transaction tx) throws Exception {
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
  public static class SupportsTransactionalAction extends TransactionalAction {

    protected SupportsTransactionalAction(Supplier<?> supplier, Synchronization synchronization,
        Class<?>[] rollbackOn, Class<?>[] dontRollbackOn, Integer timeout, boolean rollbackOnly) {
      super(TxType.SUPPORTS, supplier, synchronization, rollbackOn, dontRollbackOn, timeout,
          rollbackOnly);
    }

    @Override
    protected Object doExecute(TransactionManager tm, Transaction tx) throws Exception {
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
  public static class TransactionalActuator {

    Class<?>[] rollbackOn = Classes.EMPTY_ARRAY;
    Class<?>[] dontRollbackOn = Classes.EMPTY_ARRAY;
    TxType txType = TxType.REQUIRED;
    Synchronization synchronization;
    Integer timeout;
    boolean rollbackOnly = false;

    /**
     * Register a consumer object for the transaction currently associated with the target object.
     * After the transaction is completed, the transaction manager invokes the accept method of the
     * consumer and pass the status of the transaction completion to it.
     *
     * @param consumer the given consumer
     * @see Transaction#registerSynchronization(Synchronization)
     * @see Synchronization#afterCompletion(int)
     */
    public TransactionalActuator afterCompletion(IntConsumer consumer) {
      return synchronization(SynchronizationAdapter.afterCompletion(consumer));
    }

    /**
     * Register a runnable object for the transaction currently associated with the target object.
     * The transaction manager invokes the run method of the runnable prior to starting the
     * two-phase transaction commit process.
     *
     * @param runnable the given runnable
     *
     * @see Transaction#registerSynchronization(Synchronization)
     * @see Synchronization#beforeCompletion()
     */
    public TransactionalActuator beforeCompletion(Runnable runnable) {
      return synchronization(SynchronizationAdapter.beforeCompletion(runnable));
    }

    /**
     * Set the classes of exceptions that must not cause the transaction manager to mark the
     * transaction for roll-back.
     *
     * @param dontRollbackOn the exception classes
     */
    public TransactionalActuator dontRollbackOn(final Class<?>... dontRollbackOn) {
      this.dontRollbackOn = dontRollbackOn;
      return this;
    }

    /**
     * Execute the given Supplier in the transaction context and return the result.
     *
     * @param supplier the execution
     */
    @SuppressWarnings("unchecked")
    public <T> T get(final Supplier<T> supplier) {
      try {
        return (T) plan(shouldNotNull(supplier)).execute();
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
    public TransactionalActuator mandatory() {
      return txType(TxType.MANDATORY);
    }

    /**
     * Specify TxType as TxType.NEVER, which is equivalent to using annotation
     * {@code @Transactional(TxType.NEVER)}
     *
     * @see TxType#NEVER
     */
    public TransactionalActuator never() {
      return txType(TxType.NEVER);
    }

    /**
     * Specify TxType as TxType.NOT_SUPPORTED, which is equivalent to using annotation
     * {@code @Transactional(TxType.NOT_SUPPORTED)}
     *
     * @see TxType#NOT_SUPPORTED
     */
    public TransactionalActuator notSupported() {
      return txType(TxType.NOT_SUPPORTED);
    }

    /**
     * Specify TxType as TxType.REQUIRED, which is equivalent to using annotation
     * {@code @Transactional(TxType.REQUIRED)}
     *
     * @see TxType#REQUIRED
     */
    public TransactionalActuator required() {
      return txType(TxType.REQUIRED);
    }

    /**
     * Specify TxType as TxType.REQUIRES_NEW, which is equivalent to using annotation
     * {@code @Transactional(TxType.REQUIRES_NEW)}
     *
     * @see TxType#REQUIRES_NEW
     */
    public TransactionalActuator requiresNew() {
      return txType(TxType.REQUIRES_NEW);
    }

    /**
     * Set the classes of exceptions that must cause the transaction manager to mark the transaction
     * for roll-back.
     *
     * @param rollbackOn the exception classes
     */
    public TransactionalActuator rollbackOn(final Class<?>... rollbackOn) {
      this.rollbackOn = rollbackOn;
      return this;
    }

    /**
     * Modify the transaction associated with the target object such that the only possible outcome
     * of the transaction is to roll back the transaction, default is not roll-back.
     * <p>
     * Note: The roll-back will happen after transaction action executed.
     *
     * @see Transaction#setRollbackOnly()
     */
    public TransactionalActuator rollbackOnly() {
      rollbackOnly = true;
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
    public TransactionalActuator supports() {
      return txType(TxType.SUPPORTS);
    }

    /**
     * Register a synchronization object for the transaction associated with the target object.
     *
     * @param synchronization The Synchronization object for the transaction associated with the
     *        target object
     *
     * @see Transaction#registerSynchronization(Synchronization)
     */
    public TransactionalActuator synchronization(final Synchronization synchronization) {
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
    public TransactionalActuator timeout(final int timeout) {
      this.timeout = timeout;
      return this;
    }

    /**
     * Specify the {@link TxType} value of this actuator, so that the actuator can execute
     * corresponding actions within the transaction context according to the given TxType value
     *
     * @param txType the value of TxType
     */
    public TransactionalActuator txType(final TxType txType) {
      if (synchronization != null && (txType == TxType.NEVER || txType == TxType.NOT_SUPPORTED)) {
        throw new NotSupportedException(
            "The synchronization contained in the current actuator may not execute correctly!");
      }
      this.txType = defaultObject(txType, TxType.REQUIRED);
      return this;
    }

    protected <T> TransactionalAction plan(final Supplier<T> supplier) {
      switch (txType) {
        case MANDATORY:
          return new MandatoryTransactionalAction(supplier, synchronization, rollbackOn,
              dontRollbackOn, timeout, rollbackOnly);
        case REQUIRES_NEW:
          return new RequiresNewTransactionalAction(supplier, synchronization, rollbackOn,
              dontRollbackOn, timeout, rollbackOnly);
        case NEVER:
          return new NeverTransactionalAction(supplier, synchronization, rollbackOn, dontRollbackOn,
              timeout, rollbackOnly);
        case NOT_SUPPORTED:
          return new NotSupportedTransactionalAction(supplier, synchronization, rollbackOn,
              dontRollbackOn, timeout, rollbackOnly);
        case SUPPORTS:
          return new SupportsTransactionalAction(supplier, synchronization, rollbackOn,
              dontRollbackOn, timeout, rollbackOnly);
        default:
          return new RequiredTransactionalAction(supplier, synchronization, rollbackOn,
              dontRollbackOn, timeout, rollbackOnly);
      }
    }
  }

  public interface UserTransactionActionHandler {

    void afterExecute(boolean prestatus, Transaction tx, TransactionManager tm, TxType type);

    boolean beforeExecute(Transaction tx, TransactionManager tm, TxType type, Class<?>[] rollbackOn,
        Class<?>[] dontRollbackOn);
  }
}
