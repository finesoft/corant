package org.corant.shared.ubiquity;

import static org.corant.shared.util.Assertions.shouldNoneNull;
import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.util.Systems;

/**
 * corant-shared
 *
 * @author bingo 上午10:40:44
 */
public class Locks {

  /**
   * corant-shared
   *
   * @author bingo 上午11:09:35
   */
  public static class KeyLock<K> {

    protected final Map<K, Semaphore> semaphores = new ConcurrentHashMap<>();
    protected final int availables;

    public KeyLock() {
      this(1);
    }

    public KeyLock(int availables) {
      this.availables = availables;
    }

    public void lock(K key) {
      shouldNotNull(key);
      Semaphore semaphore =
          semaphores.compute(key, (k, v) -> v == null ? new Semaphore(availables) : v);
      semaphore.acquireUninterruptibly();
    }

    public Semaphore remove(K key) {
      if (key != null) {
        return semaphores.remove(key);
      }
      return null;
    }

    public boolean tryLock(K key) {
      shouldNotNull(key);
      Semaphore semaphore =
          semaphores.compute(key, (k, v) -> v == null ? new Semaphore(availables) : v);
      return semaphore.tryAcquire();
    }

    public boolean tryLock(K key, long time, TimeUnit unit) throws InterruptedException {
      shouldNotNull(key);
      Semaphore semaphore =
          semaphores.compute(key, (k, v) -> v == null ? new Semaphore(availables) : v);
      return semaphore.tryAcquire(time, unit);
    }

    public void unlock(K key) {
      shouldNotNull(key);
      Semaphore semaphore = semaphores.get(key);
      if (semaphore != null) {
        semaphore.release();
        return;
      }
      throw new IllegalMonitorStateException();
    }

    public <T> T withLock(K key, Supplier<T> supply) {
      shouldNoneNull(key, supply);
      boolean acquire = false;
      try {
        lock(key);
        acquire = true;
        return supply.get();
      } finally {
        if (acquire) {
          unlock(key);
        }
      }
    }

  }

  /**
   * corant-shared
   * <p>
   * Simple spin lock designed to only be held for a very short time.
   *
   * @author bingo 上午11:08:22
   */
  public static class SpinLock implements Lock {

    protected static final int SPIN_LIMIT = Systems.getCPUs() == 1 ? 0 : 5000;
    protected final AtomicBoolean locked = new AtomicBoolean();

    @Override
    public void lock() {
      for (;;) {
        for (int i = 0; i < SPIN_LIMIT; i++) {
          if (locked.compareAndSet(false, true)) {
            return;
          }
        }
        Thread.yield();
      }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
      for (;;) {
        if (Thread.interrupted()) {
          throw new InterruptedException();
        }
        for (int i = 0; i < SPIN_LIMIT; i++) {
          if (locked.compareAndSet(false, true)) {
            return;
          }
        }
        Thread.yield();
      }
    }

    @Override
    public Condition newCondition() {
      throw new NotSupportedException();
    }

    @Override
    public boolean tryLock() {
      return locked.compareAndSet(false, true);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
      final long deadline = System.nanoTime() + unit.toNanos(time);
      for (;;) {
        if (Thread.interrupted()) {
          throw new InterruptedException();
        }
        long remaining = deadline - System.nanoTime();
        int attempts = (int) Math.min(remaining, SPIN_LIMIT);
        for (int i = 0; i < attempts; i++) {
          if (locked.compareAndSet(false, true)) {
            return true;
          }
        }
        Thread.yield();
      }
    }

    @Override
    public void unlock() {
      if (!locked.compareAndSet(true, false)) {
        throw new IllegalMonitorStateException();
      }
    }

  }

}
