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
package org.corant;

import static org.corant.shared.normal.Names.CORANT;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.CollectionUtils.asSet;
import static org.corant.shared.util.StreamUtils.asStream;
import java.lang.annotation.Annotation;
import java.time.Instant;
import java.util.Arrays;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.Extension;
import org.corant.kernel.event.CorantLifecycleEvent.LifecycleEventEmitter;
import org.corant.kernel.event.PostContainerStartedEvent;
import org.corant.kernel.event.PostCorantReadyEvent;
import org.corant.kernel.event.PreContainerStopEvent;
import org.corant.kernel.spi.CorantBootHandler;
import org.corant.kernel.util.Unmanageables;
import org.corant.kernel.util.Unmanageables.UnmanageableInstance;
import org.corant.shared.util.LaunchUtils;
import org.corant.shared.util.StopWatch;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.manager.api.WeldManager;

/**
 * corant-kernel
 *
 * @author bingo 上午11:52:09
 *
 */
public class Corant {

  private static Corant INSTANCE;
  private final Class<?> configClass;
  private ClassLoader classLoader = Corant.class.getClassLoader();
  private volatile WeldContainer container;
  private final String[] args;

  public Corant() {
    this(null, null, new String[0]);
  }

  public Corant(Class<?> configClass, String... args) {
    this(configClass, configClass != null ? configClass.getClassLoader() : null, args);
  }

  public Corant(ClassLoader classLoader, String... args) {
    this(null, classLoader, args);
  }

  public Corant(String... args) {
    this(null, null, args);
  }

  Corant(Class<?> configClass, ClassLoader classLoader, String... args) {
    this.configClass = configClass;
    if (classLoader != null) {
      this.classLoader = classLoader;
    }
    this.args = args;
    INSTANCE = this;
  }

  public synchronized static CDI<Object> cdi() {
    validateRunning();
    return INSTANCE.container;
  }

  public static void fireAsyncEvent(Object event, Annotation... qualifiers) {
    validateRunning();
    if (event != null) {
      if (qualifiers.length > 0) {
        INSTANCE.getBeanManager().getEvent().select(qualifiers).fireAsync(event);
      } else {
        INSTANCE.getBeanManager().getEvent().fireAsync(event);
      }
    }
  }

  public static void fireEvent(Object event, Annotation... qualifiers) {
    validateRunning();
    if (event != null) {
      if (qualifiers.length > 0) {
        INSTANCE.getBeanManager().getEvent().select(qualifiers).fire(event);
      } else {
        INSTANCE.getBeanManager().getEvent().fire(event);
      }
    }
  }

  public static Corant instance() {
    return INSTANCE;
  }

  public static <T> UnmanageableInstance<T> produceUnmanageableBean(Class<T> clazz) {
    return Unmanageables.create(clazz);
  }

  public static <T> T resolveManageable(Class<T> manageableBeanClass, Annotation... qualifiers) {
    if (cdi().select(manageableBeanClass, qualifiers).isResolvable()) {
      return cdi().select(manageableBeanClass, qualifiers).get();
    }
    return null;
  }

  public static <T> UnmanageableInstance<T> wrapUnmanageableBean(T object) {
    return Unmanageables.accpet(object);
  }

  private static void validateRunning() {
    shouldBeTrue(INSTANCE != null && INSTANCE.isRuning(),
        "The corant instance is null or is not in running");
  }

  public Corant accept(Consumer<Corant> consumer) {
    if (consumer != null) {
      consumer.accept(this);
    }
    return this;
  }

  public <R> R apply(Function<Corant, R> function) {
    if (function != null) {
      return function.apply(this);
    }
    return null;
  }

  public synchronized WeldManager getBeanManager() {
    validateRunning();
    return (WeldManager) container.getBeanManager();
  }

  public synchronized boolean isRuning() {
    return container != null && container.isRunning();
  }

  public synchronized Corant start() {
    Thread.currentThread().setContextClassLoader(classLoader);
    StopWatch stopWatch = new StopWatch(CORANT).start("Handle before corant start");
    doBeforeStart(classLoader);
    final Logger logger = Logger.getLogger(Corant.class.getName());
    stopWatch.stop(tk -> log(logger, "%s, in %s seconds ", tk.getTaskName(), tk.getTimeSeconds()))
        .start("Initializes the CDI container");
    Weld weld = new Weld();
    weld.setClassLoader(classLoader);
    weld.addExtensions(new CorantExtension());
    if (configClass != null) {
      weld.addPackages(true, configClass);
    }
    container = weld.addProperty(Weld.SHUTDOWN_HOOK_SYSTEM_PROPERTY, true).initialize();

    stopWatch.stop(tk -> log(logger, "%s, in %s seconds ", tk.getTaskName(), tk.getTimeSeconds()))
        .start("Initializes all suites");

    doAfterContainerInitialized();

    stopWatch.stop(tk -> log(logger, "%s, in %s seconds ", tk.getTaskName(), tk.getTimeSeconds()))
        .start("Handle after corant initialized");

    doAfterStarted(classLoader);

    stopWatch.stop(tk -> log(logger, "%s, in %s seconds ", tk.getTaskName(), tk.getTimeSeconds()))
        .destroy(sw -> log(logger,
            "Finished all initialization in %s seconds, ready to receive the service.",
            sw.getTotalTimeSeconds()));
    log(logger, "Finished at: %s", Instant.now());
    log(logger, "Final memory: %sM/%sM/%sM", LaunchUtils.getUsedMemoryMb(),
        LaunchUtils.getTotalMemoryMb(), LaunchUtils.getMaxMemoryMb());
    printBoostLine();
    return this;
  }

  public synchronized void stop() {
    if (isRuning()) {
      LifecycleEventEmitter emitter = container.select(LifecycleEventEmitter.class).get();
      emitter.fire(new PreContainerStopEvent());
      ConfigProviderResolver.instance().releaseConfig(ConfigProvider.getConfig());
      container.close();
    }
  }

  void doAfterContainerInitialized() {
    LifecycleEventEmitter emitter = container.select(LifecycleEventEmitter.class).get();
    emitter.fire(new PostContainerStartedEvent(args));
  }

  void doAfterStarted(ClassLoader classLoader) {
    asStream(ServiceLoader.load(CorantBootHandler.class, classLoader))
        .sorted(CorantBootHandler::compareTo)
        .forEach(h -> h.handleAfterStarted(this, Arrays.copyOf(args, args.length)));
    LifecycleEventEmitter emitter = container.select(LifecycleEventEmitter.class).get();
    emitter.fire(new PostCorantReadyEvent(args));
  }

  void doBeforeStart(ClassLoader classLoader) {
    asStream(ServiceLoader.load(CorantBootHandler.class, classLoader))
        .sorted(CorantBootHandler::compareTo)
        .forEach(h -> h.handleBeforeStart(classLoader, Arrays.copyOf(args, args.length)));
  }

  private void log(Logger logger, String msgOrFmt, Object... args) {
    if (args.length > 0) {
      logger.info(() -> String.format(msgOrFmt, args));
    } else {
      logger.info(() -> msgOrFmt);
    }

  }

  private void printBoostLine() {
    if (!asSet(args).contains("-disable_boost_line")) {
      String spLine = "--------------------------------------------------";
      System.out.println(spLine + spLine + "\n");
    }
  }

  class CorantExtension implements Extension {
    void onAfterBeanDiscovery(@Observes AfterBeanDiscovery event) {
      event.addBean().addType(Corant.class).scope(ApplicationScoped.class)
          .addQualifier(Default.Literal.INSTANCE).addQualifier(Any.Literal.INSTANCE)
          .produceWith((obj) -> Corant.this);
    }
  }
}
