package org.corant.context.concurrent.executor;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class FutureResult<V> implements Future<V> {

  private final V resultValue;

  /**
   * Creates a <code>FutureResult</code> instance to wrap the result of an asynchronous method call
   *
   * @param result the result of an asynchronous method call to be made available to the client
   */
  public FutureResult(V result) {
    resultValue = result;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    throw new java.lang.IllegalStateException("Object does not represent an acutal Future");
  }

  @Override
  public V get() throws InterruptedException, ExecutionException {
    return resultValue;
  }

  @Override
  public V get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    throw new java.lang.IllegalStateException("Object does not represent an acutal Future");
  }

  @Override
  public boolean isCancelled() {
    throw new java.lang.IllegalStateException("Object does not represent an acutal Future");
  }

  @Override
  public boolean isDone() {
    throw new java.lang.IllegalStateException("Object does not represent an acutal Future");
  }
}
