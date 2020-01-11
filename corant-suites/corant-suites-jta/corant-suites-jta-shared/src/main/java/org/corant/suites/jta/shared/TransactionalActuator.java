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
import static org.corant.shared.util.ObjectUtils.defaultObject;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.transaction.InvalidTransactionException;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionRequiredException;
import javax.transaction.Transactional.TxType;
import javax.transaction.TransactionalException;
import org.corant.shared.exception.CorantRuntimeException;

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
public abstract class TransactionalActuator<T> {

  static final Logger logger = Logger.getLogger(TransactionalActuator.class.getName());

  final Supplier<T> supplier;
  final Class<?>[] rollbackOn;
  final Class<?>[] dontRollbackOn;

  /**
   * @param supplier
   * @param rollbackOn
   * @param dontRollbackOn
   */
  protected TransactionalActuator(Supplier<T> supplier, Class<?>[] rollbackOn,
      Class<?>[] dontRollbackOn) {
    super();
    this.supplier = shouldNotNull(supplier, "The supplier can't null");
    this.rollbackOn = defaultObject(rollbackOn, new Class[0]);
    this.dontRollbackOn = defaultObject(dontRollbackOn, new Class[0]);
  }

  public T execute() throws Exception {
    final TransactionManager tm = TransactionService.transactionManager();
    final Transaction tx = TransactionService.currentTransaction();
    return doExecute(tm, tx);
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
  public static class MandatoryTransactionalActuator<T> extends TransactionalActuator<T> {

    protected MandatoryTransactionalActuator(Supplier<T> supplier, Class<?>[] rollbackOn,
        Class<?>[] dontRollbackOn) {
      super(supplier, rollbackOn, dontRollbackOn);
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
  public static class NeverTransactionalActuator<T> extends TransactionalActuator<T> {

    protected NeverTransactionalActuator(Supplier<T> supplier, Class<?>[] rollbackOn,
        Class<?>[] dontRollbackOn) {
      super(supplier, rollbackOn, dontRollbackOn);
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
  public static class NotSupportedTransactionalActuator<T> extends TransactionalActuator<T> {

    protected NotSupportedTransactionalActuator(Supplier<T> supplier, Class<?>[] rollbackOn,
        Class<?>[] dontRollbackOn) {
      super(supplier, rollbackOn, dontRollbackOn);
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
  public static class RequiredTransactionalActuator<T> extends TransactionalActuator<T> {

    protected RequiredTransactionalActuator(Supplier<T> supplier, Class<?>[] rollbackOn,
        Class<?>[] dontRollbackOn) {
      super(supplier, rollbackOn, dontRollbackOn);
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
  public static class RequiresNewTransactionalActuator<T> extends TransactionalActuator<T> {

    protected RequiresNewTransactionalActuator(Supplier<T> supplier, Class<?>[] rollbackOn,
        Class<?>[] dontRollbackOn) {
      super(supplier, rollbackOn, dontRollbackOn);
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
  public static class SupportsTransactionalActuator<T> extends TransactionalActuator<T> {

    protected SupportsTransactionalActuator(Supplier<T> supplier, Class<?>[] rollbackOn,
        Class<?>[] dontRollbackOn) {
      super(supplier, rollbackOn, dontRollbackOn);
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
  public static class TransactionalActuatorPlan<T> {

    Class<?>[] rollbackOn = new Class[0];
    Class<?>[] dontRollbackOn = new Class[0];
    TxType txType = TxType.REQUIRED;

    public TransactionalActuatorPlan<T> dontRollbackOn(final Class<?>... dontRollbackOn) {
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

    public TransactionalActuatorPlan<T> rollbackOn(final Class<?>... rollbackOn) {
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

    public TransactionalActuatorPlan<T> txType(final TxType txType) {
      this.txType = defaultObject(txType, TxType.REQUIRED);
      return this;
    }

    protected TransactionalActuator<T> plan(final Supplier<T> supplier) {
      switch (txType) {
        case MANDATORY:
          return new MandatoryTransactionalActuator<>(supplier, rollbackOn, dontRollbackOn);
        case REQUIRES_NEW:
          return new RequiresNewTransactionalActuator<>(supplier, rollbackOn, dontRollbackOn);
        case NEVER:
          return new NeverTransactionalActuator<>(supplier, rollbackOn, dontRollbackOn);
        case NOT_SUPPORTED:
          return new NotSupportedTransactionalActuator<>(supplier, rollbackOn, dontRollbackOn);
        case SUPPORTS:
          return new SupportsTransactionalActuator<>(supplier, rollbackOn, dontRollbackOn);
        default:
          return new RequiredTransactionalActuator<>(supplier, rollbackOn, dontRollbackOn);
      }
    }
  }
}
