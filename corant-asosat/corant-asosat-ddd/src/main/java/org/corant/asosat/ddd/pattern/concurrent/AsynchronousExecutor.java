/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.asosat.ddd.pattern.concurrent;

import static org.corant.shared.util.ClassUtils.tryAsClass;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import org.corant.Corant;
import org.corant.shared.exception.NotSupportedException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * @author bingo 下午6:07:59
 *
 */
@ApplicationScoped
public class AsynchronousExecutor {

  public static final String AE_NAME_DFLT = "app-async";
  public static final String AE = "async.executor";
  public static final String AE_NAME = AE + ".name";
  public static final String AE_CORE_SIZE = AE + ".coreSize";
  public static final String AE_MAX_SIZE = AE + ".maxSize";
  public static final String AE_KEEP_ALIVE_VALUE = AE + ".keepAlive.value";
  public static final String AE_KEEP_ALIVE_UNIT = AE + ".keepAlive.unit";
  public static final String AE_QUEUE_FAIR = AE + ".queue.fair.size";
  public static final String AE_QUEUE_CAPACITY = AE + ".queue.capacity";
  public static final String AE_REJEXE_HANDLER_NAME = AE + ".rejectedExecutionHandler.name";

  public static final String SE_NAME_DFLT = "app-schedule";
  public static final String SE = "scheduled.executor";
  public static final String SE_CORE_SIZE = SE + ".coreSize";
  public static final String SE_NAME = SE + ".name";
  public static final String SE_REJEXE_HANDLER_NAME = SE + ".rejectedExecutionHandler.name";

  @Inject
  BeanManager beanManager;

  @Inject
  @ConfigProperty(name = AE_CORE_SIZE, defaultValue = "0")
  int asyncCorePoolSize;

  @Inject
  @ConfigProperty(name = AE_MAX_SIZE, defaultValue = "0")
  int asyncMaxPoolSize;
  @Inject
  @ConfigProperty(name = AE_KEEP_ALIVE_VALUE, defaultValue = "0")
  long asyncKeepAlive;
  @Inject
  @ConfigProperty(name = AE_KEEP_ALIVE_UNIT, defaultValue = "MILLISECONDS")
  String asyncKeepAliveUnit;
  @Inject
  @ConfigProperty(name = AE_QUEUE_CAPACITY, defaultValue = "0")
  int asyncQueueCapacity;
  @Inject
  @ConfigProperty(name = AE_QUEUE_FAIR, defaultValue = "0")
  int asyncQueueFairSize;
  @Inject
  @ConfigProperty(name = AE_REJEXE_HANDLER_NAME, defaultValue = "")
  String rejectedHandlerName;
  @Inject
  @ConfigProperty(name = AE_NAME, defaultValue = AE_NAME_DFLT)
  String threadFactoryName;
  @Inject
  @ConfigProperty(name = SE_NAME, defaultValue = SE_NAME_DFLT)
  String scheduleThreadFactoryName;
  ExecutorService linkedExecutorService;// fair

  ExecutorService arrayExecutorService;// unfair

  ScheduledExecutorService shceduledExecutorService;

  private volatile boolean shutdown = true;

  private final Collection<CreationalContext<?>> contexts = new ArrayList<>(8);

  public AsynchronousExecutor() {
    super();
  }

  public static AsynchronousExecutor instance() {
    return Corant.resolveManageable(AsynchronousExecutor.class);
  }

  public ExecutorService getArrayExecutorService() {
    check();
    return arrayExecutorService;
  }

  public ExecutorService getLinkedExecutorService() {
    check();
    return linkedExecutorService;
  }

  public ScheduledExecutorService getShceduledExecutorService() {
    check();
    return shceduledExecutorService;
  }

  public boolean isShutdown() {
    return shutdown;
  }

  public CompletableFuture<Void> runAsync(Runnable runnable) {
    check();
    return CompletableFuture.runAsync(runnable, arrayExecutorService);
  }

  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    check();
    return shceduledExecutorService.schedule(callable, delay, unit);
  }

  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    check();
    return shceduledExecutorService.schedule(command, delay, unit);
  }

  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period,
      TimeUnit unit) {
    check();
    return shceduledExecutorService.scheduleAtFixedRate(command, initialDelay, period, unit);
  }

  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
      TimeUnit unit) {
    check();
    return shceduledExecutorService.scheduleWithFixedDelay(command, initialDelay, delay, unit);
  }

  public void shutdown(Consumer<List<Runnable>> commencedTasks) {
    if (commencedTasks == null) {
      linkedExecutorService.shutdown();
      arrayExecutorService.shutdown();
      shceduledExecutorService.shutdown();
    } else {
      commencedTasks.accept(linkedExecutorService.shutdownNow());
      commencedTasks.accept(arrayExecutorService.shutdownNow());
      commencedTasks.accept(shceduledExecutorService.shutdownNow());
    }
    for (final CreationalContext<?> ctx : contexts) {
      ctx.release();
    }
    contexts.clear();
    shutdown = true;
  }

  public <T> Future<T> submit(Callable<T> task) {
    return this.submit(task, true);
  }

  public <T> Future<T> submit(Callable<T> task, boolean fair) {
    check();
    if (fair) {
      return linkedExecutorService.submit(task);
    } else {
      return arrayExecutorService.submit(task);
    }
  }

  public Future<?> submit(Runnable task) {
    return this.submit(task, true);
  }

  public Future<?> submit(Runnable task, boolean fair) {
    check();
    if (fair) {
      return linkedExecutorService.submit(task);
    } else {
      return arrayExecutorService.submit(task);
    }
  }

  public <T> Future<T> submit(Runnable task, T result) {
    return this.submit(task, result, true);
  }

  public <T> Future<T> submit(Runnable task, T result, boolean fair) {
    check();
    if (fair) {
      return linkedExecutorService.submit(task, result);
    } else {
      return arrayExecutorService.submit(task, result);
    }
  }

  public <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
    check();
    return CompletableFuture.supplyAsync(supplier, arrayExecutorService);
  }

  void check() {
    if (shutdown) {
      throw new NotSupportedException();
    }
  }

  @PreDestroy
  synchronized void destroy() {
    shutdown(null);
  }

  @PostConstruct
  synchronized void enable() {
    initExecutorJsr236();
    initExecutorManual();
    shutdown = linkedExecutorService == null || shceduledExecutorService == null
        || arrayExecutorService == null;
  }

  private void initExecutorJsr236() {
    if (tryAsClass("javax.enterprise.concurrent.ManagedExecutorService") != null
        && linkedExecutorService == null) {
      linkedExecutorService = Corant.resolveManageable(ManagedExecutorService.class);
    }
    if (tryAsClass("javax.enterprise.concurrent.ManagedScheduledExecutorService") != null
        && shceduledExecutorService == null) {
      shceduledExecutorService = Corant.resolveManageable(ManagedScheduledExecutorService.class);
    }
  }

  private void initExecutorManual() {
    final int coreSize =
        asyncCorePoolSize < 1 ? Math.max(2, Runtime.getRuntime().availableProcessors())
            : asyncCorePoolSize,
        maxSize = asyncMaxPoolSize < 1 ? coreSize : asyncCorePoolSize;
    final TimeUnit timeUnit = TimeUnit.valueOf(asyncKeepAliveUnit);
    final RejectedExecutionHandler rejectedHandler;
    if (isNotBlank(rejectedHandlerName)) {
      rejectedHandler = this.lookupByName(rejectedHandlerName, RejectedExecutionHandler.class);
    } else {
      rejectedHandler = new ThreadPoolExecutor.AbortPolicy();
    }
    if (linkedExecutorService == null) {
      final int capacity = asyncQueueCapacity < 1 ? Integer.MAX_VALUE : asyncQueueCapacity;
      final BlockingQueue<Runnable> linked = new LinkedBlockingQueue<>(capacity);
      final ThreadFactory threadFactory = new DefaultThreadFactory(threadFactoryName + "-linked");
      linkedExecutorService = new ThreadPoolExecutor(coreSize, maxSize, asyncKeepAlive, timeUnit,
          linked, threadFactory, rejectedHandler);
    }

    if (arrayExecutorService == null) {
      final int size = asyncQueueFairSize < 1 ? 1024 : asyncQueueFairSize;
      final BlockingQueue<Runnable> array = new ArrayBlockingQueue<>(size, false);
      final ThreadFactory threadFactory = new DefaultThreadFactory(threadFactoryName + "-array");
      arrayExecutorService = new ThreadPoolExecutor(coreSize, maxSize, asyncKeepAlive, timeUnit,
          array, threadFactory, rejectedHandler);
    }

    if (shceduledExecutorService == null) {
      final String sthreadFactoryName = scheduleThreadFactoryName;
      final ThreadFactory threadFactory = new DefaultThreadFactory(sthreadFactoryName);
      shceduledExecutorService =
          new ScheduledThreadPoolExecutor(coreSize, threadFactory, rejectedHandler);
    }
  }

  private <T> T lookupByName(final String name, final Class<T> type) {
    final Set<Bean<?>> tfb = beanManager.getBeans(name);
    final Bean<?> bean = beanManager.resolve(tfb);
    final CreationalContext<?> ctx = beanManager.createCreationalContext(null);
    if (!beanManager.isNormalScope(bean.getScope())) {
      contexts.add(ctx);
    }
    return type.cast(beanManager.getReference(bean, type, ctx));
  }

  static class DefaultThreadFactory implements ThreadFactory {
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    DefaultThreadFactory(String name) {
      SecurityManager s = System.getSecurityManager();
      group = s != null ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
      namePrefix = name + "-pool-thread-";
    }

    @Override
    public Thread newThread(Runnable r) {
      Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
      if (t.isDaemon()) {
        t.setDaemon(false);
      }
      if (t.getPriority() != Thread.NORM_PRIORITY) {
        t.setPriority(Thread.NORM_PRIORITY);
      }
      return t;
    }
  }
}
