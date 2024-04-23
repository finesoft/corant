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
package org.corant.shared.util;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableCollection;
import static org.corant.shared.ubiquity.Throwing.uncheckedRunner;
import static org.corant.shared.util.Assertions.shouldNoneNull;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.defaultObject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.logging.Logger;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Names;
import org.corant.shared.ubiquity.Throwing.ThrowingRunnable;

/**
 * corant-shared
 *
 * @author bingo 下午2:55:59
 */
public class Threads {

  public static final String DAEMON_THREAD_NAME_PREFIX = Names.CORANT.concat("-daemon");
  static final AtomicLong DAEMON_THREAD_ID = new AtomicLong(0);

  /**
   * Run a Callable in a daemon thread and return the computed result.
   *
   * @param <V> the computed result type
   * @param callable the callable to be run
   * @return the given callable computed result
   */
  public static <V> V callInDaemon(Callable<V> callable) {
    final FutureTask<V> future = new FutureTask<>(shouldNotNull(callable));
    runInDaemon(future);
    try {
      return future.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new CorantRuntimeException(e);
    }
  }

  /**
   * Runs a Callable in a daemon thread with the given thread name and returns the computed result.
   *
   * @param <V> the computed result type
   * @param threadName the thread name use in daemon thread
   * @param callable the callable to be run
   * @return callInDaemon
   */
  public static <V> V callInDaemon(String threadName, Callable<V> callable) {
    final FutureTask<V> future = new FutureTask<>(shouldNotNull(callable));
    runInDaemon(threadName, future);
    try {
      return future.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new CorantRuntimeException(e);
    }
  }

  /**
   * Returns all active thread groups excluding the system thread group (A thread group is active if
   * it has been not destroyed).
   *
   * @return all thread groups excluding the system thread group. The collection returned is always
   *         unmodifiable.
   * @throws SecurityException if the current thread cannot access the system thread group
   *
   * @throws SecurityException if the current thread cannot modify thread groups from this thread's
   *         thread group up to the system thread group
   */
  public static Collection<ThreadGroup> currentThreadGroups() {
    return findThreadGroups(getSystemThreadGroup(), true, Functions.emptyPredicate(true));
  }

  /**
   * Returns all active threads (A thread is active if it has been started and has not yet died).
   * <p>
   * Note: Code base from org.apache.commons.lang3.
   *
   * @return all active threads. The collection returned is always unmodifiable.
   * @throws SecurityException if the current thread cannot access the system thread group
   *
   * @throws SecurityException if the current thread cannot modify thread groups from this thread's
   *         thread group up to the system thread group
   */
  public static Collection<Thread> currentThreads() {
    return findThreads(Functions.emptyPredicate(true));
  }

  /**
   * Returns a daemon thread factory with the specified thread name.
   *
   * @param threadName the thread name use in daemon thread
   * @return a daemon thread factory
   */
  public static ThreadFactory daemonThreadFactory(final String threadName) {
    return daemonThreadFactory(threadName, Thread.NORM_PRIORITY);
  }

  /**
   * Returns a daemon thread factory with the specified thread name and thread priority
   *
   * @param threadName the thread name use in daemon thread
   * @param priority the thread priority use in daemon thread
   * @return a daemon thread factory
   */
  public static ThreadFactory daemonThreadFactory(final String threadName, final int priority) {
    return r -> {
      // FIXME context propagation
      Thread thread = new Thread(r);
      thread.setName(threadName + '-' + DAEMON_THREAD_ID.incrementAndGet());
      thread.setDaemon(true);
      thread.setPriority(priority);
      return thread;
    };
  }

  /**
   * Delay running a runnable in a daemon thread with the given delay duration.
   *
   * @param delay the delay duration, null means don't delay
   * @param runner the runner
   */
  public static void delayRunInDaemon(Duration delay, Runnable runner) {
    delayRunInDaemon(DAEMON_THREAD_NAME_PREFIX, delay, runner);
  }

  /**
   * Delay running a runnable in a daemon thread with the given thread name and delay duration.
   *
   * @param threadName the thread name use in daemon thread
   * @param delay the delay duration, null means don't delay
   * @param runner the runner
   */
  public static void delayRunInDaemon(String threadName, Duration delay, Runnable runner) {
    daemonThreadFactory(threadName).newThread(() -> {
      if (delay != null) {
        try {
          TimeUnit.MILLISECONDS.sleep(delay.toMillis());
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }
      runner.run();
    }).start();
  }

  /**
   * Delay running a throwing runnable in a daemon thread with the given thread name and delay
   * duration.
   *
   * @param delay the delay duration, null means don't delay
   * @param runner the runner
   */
  public static <E extends Exception> void delayRunInDaemonx(Duration delay,
      ThrowingRunnable<E> runner) {
    delayRunInDaemonx(DAEMON_THREAD_NAME_PREFIX, delay, runner);
  }

  public static <E extends Exception> void delayRunInDaemonx(String threadName, Duration delay,
      ThrowingRunnable<E> runner) {
    delayRunInDaemon(threadName, delay, uncheckedRunner(runner));
  }

  /**
   * Select all active thread groups which match the given predicate and which is a subgroup of the
   * given thread group (or one of its subgroups).
   *
   * <p>
   * Note: Code base from org.apache.commons.lang3.
   *
   * @param group the thread group
   * @param recurse if {@code true} then evaluate the predicate recursively on all thread groups in
   *        all subgroups of the given group
   * @param predicate the predicate
   * @return An unmodifiable {@code Collection} of active thread groups which match the given
   *         predicate and which is a subgroup of the given thread group
   * @throws IllegalArgumentException if the given group or predicate is null
   * @throws SecurityException if the current thread cannot modify thread groups from this thread's
   *         thread group up to the system thread group
   */
  public static Collection<ThreadGroup> findThreadGroups(final ThreadGroup group,
      final boolean recurse, final Predicate<ThreadGroup> predicate) {
    shouldNoneNull(group, predicate);
    int count = group.activeGroupCount();
    ThreadGroup[] threadGroups;
    do {
      threadGroups = new ThreadGroup[count + count / 2 + 1]; // slightly grow the array size
      count = group.enumerate(threadGroups, recurse);
      // return value of enumerate() must be strictly less than the array size according to javadoc
    } while (count >= threadGroups.length);

    final List<ThreadGroup> result = new ArrayList<>(count);
    for (int i = 0; i < count; ++i) {
      if (predicate.test(threadGroups[i])) {
        result.add(threadGroups[i]);
      }
    }
    return unmodifiableCollection(result);
  }

  /**
   * Select all active threads which match the given predicate.
   * <p>
   * Note: Code base from org.apache.commons.lang3.
   *
   * @param predicate the predicate
   * @return An unmodifiable {@code Collection} of active threads matching the given predicate
   *
   * @throws IllegalArgumentException if the predicate is null
   * @throws SecurityException if the current thread cannot access the system thread group
   * @throws SecurityException if the current thread cannot modify thread groups from this thread's
   *         thread group up to the system thread group
   */
  public static Collection<Thread> findThreads(final Predicate<Thread> predicate) {
    return findThreads(getSystemThreadGroup(), true, predicate);
  }

  /**
   * Select all active threads which match the given predicate and which belongs to the given thread
   * group (or one of its subgroups).
   *
   * <p>
   * Note: Code base from org.apache.commons.lang3.
   *
   * @param group the thread group
   * @param recurse if {@code true} then evaluate the predicate recursively on all threads in all
   *        subgroups of the given group
   * @param predicate the predicate
   * @return An unmodifiable {@code Collection} of active threads which match the given predicate
   *         and which belongs to the given thread group
   * @throws IllegalArgumentException if the given group or predicate is null
   * @throws SecurityException if the current thread cannot modify thread groups from this thread's
   *         thread group up to the system thread group
   */
  public static Collection<Thread> findThreads(final ThreadGroup group, final boolean recurse,
      final Predicate<Thread> predicate) {
    shouldNoneNull(group, predicate);
    int count = group.activeCount();
    Thread[] threads;
    do {
      threads = new Thread[count + count / 2 + 1]; // slightly grow the array size
      count = group.enumerate(threads, recurse);
      // return value of enumerate() must be strictly less than the array size according to javadoc
    } while (count >= threads.length);

    final List<Thread> result = new ArrayList<>(count);
    for (int i = 0; i < count; ++i) {
      if (predicate.test(threads[i])) {
        result.add(threads[i]);
      }
    }
    return unmodifiableCollection(result);
  }

  /**
   * Gets the system thread group (sometimes also referred as "root thread group").
   * <p>
   * Note: Code base from org.apache.commons.lang3.
   *
   * @return the system thread group
   * @throws SecurityException if the current thread cannot modify thread groups from this thread's
   *         thread group up to the system thread group
   */
  public static ThreadGroup getSystemThreadGroup() {
    ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
    while (threadGroup.getParent() != null) {
      threadGroup = threadGroup.getParent();
    }
    return threadGroup;
  }

  /**
   * Returns a periodic runner.
   * <p>
   * Note: Use java.util.Timer to reduce busy-waiting for better performance.
   *
   * @param name the runner thread name
   * @param daemon whether is thread daemon
   * @param period the period in milliseconds
   * @param runnable the runnable to run
   */
  public static SimplePeriodRunner periodRunnerOf(String name, boolean daemon, long period,
      Runnable runnable) {
    return new SimplePeriodRunner(name, daemon, period, runnable);
  }

  /**
   * Returns a periodic runner.
   * <p>
   * Note: Use java.util.Timer to reduce busy-waiting for better performance.
   *
   * @param name the runner thread name
   * @param daemon whether is thread daemon
   * @param period the period
   * @param periodUnit the period unit
   * @param runnable the runnable to run
   */
  public static SimplePeriodRunner periodRunnerOf(String name, boolean daemon, long period,
      TimeUnit periodUnit, Runnable runnable) {
    return new SimplePeriodRunner(name, daemon, period, periodUnit, runnable);
  }

  /**
   * Run a runnable in a daemon thread.
   *
   * @param runnable the runnable
   */
  public static void runInDaemon(Runnable runnable) {
    runInDaemon(DAEMON_THREAD_NAME_PREFIX, runnable);
  }

  /**
   * Run a runnable in a daemon thread with given thread name
   *
   * @param threadName the daemon thread name
   * @param runnable the runnable
   */
  public static void runInDaemon(String threadName, Runnable runnable) {
    delayRunInDaemon(threadName, null, runnable);
  }

  /**
   * Run a throwing runnable in a daemon thread with given thread name
   *
   * @param threadName the daemon thread name
   * @param runnable the runnable
   */
  public static <E extends Exception> void runInDaemonx(String threadName,
      ThrowingRunnable<E> runnable) {
    delayRunInDaemonx(threadName, null, runnable);
  }

  /**
   * Run a throwing runnable in a daemon thread.
   *
   * @param runnable the runnable
   */
  public static <E extends Exception> void runInDaemonx(ThrowingRunnable<E> runnable) {
    delayRunInDaemonx(null, runnable);
  }

  /**
   * Try to make the current sleep for the given milliseconds without throwing an
   * InterruptedException.
   *
   * @param ms the length of time to sleep in milliseconds
   */
  public static void tryThreadSleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      // Noop! just try...
    }
  }

  /**
   * corant-shared
   *
   * @author bingo 上午10:24:59
   */
  public static class SimplePeriodRunnable implements Runnable {

    protected final Logger logger = Logger.getLogger(getClass().getName());
    protected final long period;
    protected final TimeUnit periodUnit;
    protected final Object monitor;
    protected final Runnable runnable;
    protected volatile boolean activated = true;
    protected final long periodMs;

    public SimplePeriodRunnable(long period, Runnable runnable) {
      this(period, TimeUnit.MILLISECONDS, null, runnable);
    }

    public SimplePeriodRunnable(long period, TimeUnit periodUnit, Object monitor,
        Runnable runnable) {
      this.period = period;
      this.periodUnit = defaultObject(periodUnit, TimeUnit.MILLISECONDS);
      this.monitor = monitor;
      this.runnable = shouldNotNull(runnable, "The runnable for runner can't null");
      periodMs = period > 0 ? TimeUnit.MILLISECONDS.convert(period, periodUnit) : 0;
    }

    public void deactivate() {
      activated = false;
    }

    public boolean isActivated() {
      return activated;
    }

    @Override
    public void run() {
      try {
        while (activated) {
          if (periodMs > 0) {
            Thread.sleep(periodMs);
          }
          if (activated) {
            runnable.run();
          }
        }
      } catch (InterruptedException e) {
        // Noop, here sleeping is forcibly interrupted, that OK!
      } finally {
        if (monitor != null) {
          synchronized (monitor) {
            logger.fine(() -> format("The period runnable %s is about to exit.",
                Thread.currentThread().getName()));
            if (!activated) {
              monitor.notifyAll();
            } else {
              activated = false;
            }
            logger.fine(
                () -> format("The period runnable %s exits.", Thread.currentThread().getName()));
          }
        } else {
          activated = false;
        }
      }
    }

  }

  /**
   * corant-shared
   *
   * @author bingo 上午10:54:19
   */
  public static class SimplePeriodRunner {

    protected Thread thread;
    protected final Object monitor = new Object();
    protected final SimplePeriodRunnable runnable;
    protected final String name;
    protected final boolean daemon;

    public SimplePeriodRunner(String name, boolean daemon, long period, Runnable runnable) {
      this(name, daemon, period, TimeUnit.MILLISECONDS, runnable);
    }

    public SimplePeriodRunner(String name, boolean daemon, long period, TimeUnit periodUnit,
        Runnable runnable) {
      this.runnable = new SimplePeriodRunnable(period, periodUnit, monitor, runnable);
      this.daemon = daemon;
      this.name = name;
    }

    public SimplePeriodRunner start() {
      synchronized (monitor) {
        if (thread == null) {
          runnable.activated = true;
          thread = new Thread(runnable);
          thread.setDaemon(daemon);
          if (name != null) {
            thread.setName(name);
          }
          thread.start();
        }
      }
      return this;
    }

    public SimplePeriodRunner stop() {
      synchronized (monitor) {
        if (thread != null) {
          while (runnable.isActivated()) {
            runnable.deactivate();
            try {
              thread.interrupt();
              monitor.wait();
            } catch (InterruptedException ex) {
              Thread.currentThread().interrupt();
            }
          }
          thread = null;
        }
      }
      return this;
    }
  }
}
