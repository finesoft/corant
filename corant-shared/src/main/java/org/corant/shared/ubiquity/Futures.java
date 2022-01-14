package org.corant.shared.ubiquity;

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.corant.shared.exception.NotSupportedException;

public interface Futures {

  static <V> FutureResult<V> complete(V result) {
    return new FutureResult<>(result);
  }

  public final class FutureResult<V> implements Future<V> {

    private final V resultValue;

    /**
     * Creates a <code>FutureResult</code> instance to wrap the result of an asynchronous method
     * call
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

  class NoncancelabilityFuture<V> extends SimpleFuture<V> {

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      throw new NotSupportedException("Can't support cancel");
    }

    @Override
    public boolean isCancelled() {
      throw new NotSupportedException("Can't support cancel");
    }

  }

  class SimpleFuture<V> implements Future<V> {

    private volatile boolean completed;
    private volatile boolean cancelled;
    private volatile V result;
    private volatile Throwable throwable;

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
      if (completed) {
        return false;
      }
      completed = true;
      cancelled = true;
      notifyAll();
      return true;
    }

    public synchronized boolean failure(Throwable throwable) {
      if (completed) {
        return false;
      }
      completed = true;
      this.throwable = throwable;
      notifyAll();
      return true;
    }

    @Override
    public synchronized V get() throws InterruptedException, ExecutionException {
      while (!completed) {
        wait();
      }
      return retrieve();
    }

    @Override
    public synchronized V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
      if (completed) {
        return retrieve();
      }
      final long millis = Math.max(shouldNotNull(unit).toMillis(timeout), 0);
      long waitTime = millis;
      if (waitTime == 0) {
        throw new TimeoutException();
      } else {
        final long startTime = System.currentTimeMillis();
        for (;;) {
          wait(waitTime);
          if (completed) {
            return retrieve();
          } else {
            waitTime = millis - (System.currentTimeMillis() - startTime);
            if (waitTime <= 0) {
              throw new TimeoutException();
            }
          }
        }
      }
    }

    @Override
    public boolean isCancelled() {
      return cancelled;
    }

    @Override
    public boolean isDone() {
      return completed;
    }

    public synchronized boolean success(V result) {
      if (completed) {
        return false;
      }
      completed = true;
      this.result = result;
      notifyAll();
      return true;
    }

    V retrieve() throws ExecutionException {
      if (throwable != null) {
        throw new ExecutionException(throwable);
      }
      if (cancelled) {
        throw new CancellationException();
      }
      return this.result;
    }
  }
}
