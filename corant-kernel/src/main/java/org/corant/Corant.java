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

import static org.corant.kernel.normal.Names.applicationName;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.CollectionUtils.setOf;
import static org.corant.shared.util.StreamUtils.streamOf;
import java.lang.annotation.Annotation;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import org.corant.kernel.event.CorantLifecycleEvent.LifecycleEventEmitter;
import org.corant.kernel.event.PostContainerStartedEvent;
import org.corant.kernel.event.PostCorantReadyEvent;
import org.corant.kernel.event.PreContainerStopEvent;
import org.corant.kernel.spi.CorantBootHandler;
import org.corant.kernel.util.Manageables;
import org.corant.kernel.util.Unmanageables;
import org.corant.kernel.util.Unmanageables.UnmanageableInstance;
import org.corant.shared.util.LaunchUtils;
import org.corant.shared.util.StopWatch;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.manager.api.WeldManager;

/**
 * corant-kernel
 *
 * <p>
 * Class that can be used to bootstrap and launch a Corant application from a Java main method. By
 * default class will perform the following steps to bootstrap your application:
 * <ul>
 * <li>Execute the boot preprocessor to handle some works before CDI container start, the works like
 * set some appropriate configuration properties to intervene system running.</li>
 * <li>Configure appropriate class loader to the current thread context class loader and CDI
 * container class loader and add configuration class to the set of bean classes for the synthetic
 * bean archive if necessary.</li>
 * <li>Construct the CDI container and initialize it, after the CDI container initialized then fire
 * PostContainerStartedEvent to listeners, those listeners may be use to configure some components
 * after CDI initialized such as web server.</li>
 * <li>After the above execution was completed, fire PostCorantReadyEvent to listeners.</li>
 * </ul>
 * </p>
 *
 * <p>
 * In most circumstances the static {@link #run(Class, String[])} method can be called directly from
 * your {@literal main} method to bootstrap your application:
 *
 * <pre>
 * public class MyApplication {
 *   // ... Bean definitions
 *   public static void main(String[] args) throws Exception {
 *     try(Corant corant = Corant.run(MyApplication.class, args)){
 *      //... some works in CDI
 *   }
 * }
 * </pre>
 *
 * OR
 *
 * <pre>
 * public class MyApplication {
 *   // ... Bean definitions
 *   public static void main(String[] args) throws Exception {
 *    Corant corant = Corant.run(MyApplication.class, args)
 *   }
 * }
 * </pre>
 *
 * OR
 *
 * <pre>
 * public class MyApplication {
 *   // ... Bean definitions
 *   public static void main(String[] args) throws Exception {
 *    try(Corant corant = new Corant(args).start(MyApplication.class,MyAnother.class....){
 *    }
 *   }
 * }
 * </pre>
 * <p>
 *
 * @see #Corant(Class, ClassLoader, String...)
 * @see #Corant(Class, String...)
 * @see #Corant(ClassLoader, String...)
 * @see CorantBootHandler
 * @see ApplicationConfigSourceProvider
 * @see ApplicationAdjustConfigSourceProvider
 * @see ApplicationProfileConfigSourceProvider
 *
 * @author bingo 上午11:52:09
 *
 */
public class Corant implements AutoCloseable {

  private static volatile Corant me;
  private final Class<?> configClass;
  private final String[] args;
  private ClassLoader classLoader = Corant.class.getClassLoader();
  private volatile WeldContainer container;
  private volatile Object id;

  /**
   * Use the class loader of Corant.class as current thread context and the CDI container class
   * loader.
   */
  public Corant() {
    this(null, null, new String[0]);
  }

  /**
   * Construct Coarnt instance with given config class and class loader and args. If the given
   * config class is not null then it will be added to the set of bean classes for the synthetic
   * bean archive. If the given class loader is not null then it will be set to the context
   * ClassLoader for current Thread and the CDI container class loader else we use Corant.class
   * class loader. The given args will be propagate to all CorantBootHandler and all
   * CorantLifecycleEvent listeners.
   *
   * @param configClass
   * @param classLoader
   * @param args
   */
  public Corant(Class<?> configClass, ClassLoader classLoader, String... args) {
    this.configClass = configClass;
    if (classLoader != null) {
      this.classLoader = classLoader;
    }
    this.args = args;
  }

  /**
   * Use given config class and args construct Corant instance. If the given config class is not
   * null then the class loader of the current thread context and the CDI container will be set with
   * the given config class class loader. The given args will be propagate to all CorantBootHandler
   * and all CorantLifecycleEvent listeners.
   *
   * @see #Corant(Class, ClassLoader, String...)
   * @param configClass
   * @param args
   */
  public Corant(Class<?> configClass, String... args) {
    this(configClass, configClass != null ? configClass.getClassLoader() : null, args);
  }

  /**
   * Use given class loader and args to construct Corant instance. If the given class loader is not
   * null then the class loader of the current thread context and the CDI container will be set with
   * the given class loader.The given args will be propagate to all CorantBootHandler and all
   * CorantLifecycleEvent listeners.
   *
   * @param classLoader
   * @param args
   */
  public Corant(ClassLoader classLoader, String... args) {
    this(null, classLoader, args);
  }

  /**
   * Use given args and the class loader of Corant.class to construct Corant instance.The given args
   * will be propagate to all CorantBootHandler and all CorantLifecycleEvent listeners.
   *
   * @param args
   */
  public Corant(String... args) {
    this(null, null, args);
  }

  public static void fireAsyncEvent(Object event, Annotation... qualifiers) {
    validateRunning();
    if (event != null) {
      if (qualifiers.length > 0) {
        me.getBeanManager().getEvent().select(qualifiers).fireAsync(event);
      } else {
        me.getBeanManager().getEvent().fireAsync(event);
      }
    }
  }

  public static void fireEvent(Object event, Annotation... qualifiers) {
    validateRunning();
    if (event != null) {
      if (qualifiers.length > 0) {
        me.getBeanManager().getEvent().select(qualifiers).fire(event);
      } else {
        me.getBeanManager().getEvent().fire(event);
      }
    }
  }

  public synchronized static Instance<Object> instance() {
    validateRunning();
    return me.container;
  }

  public static Corant me() {
    return me;
  }

  public static <T> UnmanageableInstance<T> produceUnmanageableBean(Class<T> clazz) {
    return Unmanageables.create(clazz);
  }

  public static <T> T resolveManageable(Class<T> manageableBeanClass, Annotation... qualifiers) {
    if (instance().select(manageableBeanClass, qualifiers).isResolvable()) {
      return instance().select(manageableBeanClass, qualifiers).get();
    }
    return null;
  }

  @SuppressWarnings("resource")
  public synchronized static Corant run(Class<?> configClass, String... args) {
    return new Corant(configClass, args).start();
  }

  @SuppressWarnings("resource")
  public synchronized static Corant run(Object configObject, String... args) {
    if (configObject == null) {
      return new Corant(args);
    }
    Corant inst = new Corant(configObject.getClass(), args).start();
    if (!Manageables.isManagedBean(configObject)
        && Corant.instance().select(configObject.getClass()).isUnsatisfied()) {
      Corant.wrapUnmanageableBean(configObject);
    }
    return inst;
  }

  public synchronized static Optional<Instance<Object>> tryInstance() {
    if (me == null || me.container == null) {
      return Optional.empty();
    }
    return Optional.of(me.container);
  }

  public static <T> UnmanageableInstance<T> wrapUnmanageableBean(T object) {
    return Unmanageables.accpet(object);
  }

  private static synchronized void setMe(Corant me) {
    if (me != null) {
      shouldBeTrue(Corant.me == null, "We already have an instance of Corant. Don't repeat it!");
    }
    Corant.me = me;
  }

  private static void validateRunning() {
    shouldBeTrue(me != null && me.isRuning(), "The corant instance is null or is not in running");
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

  @Override
  public void close() throws Exception {
    stop();
  }

  public synchronized WeldManager getBeanManager() {
    validateRunning();
    return (WeldManager) container.getBeanManager();
  }

  public Object getId() {
    return id;
  }

  public synchronized boolean isRuning() {
    return container != null && container.isRunning();
  }

  public synchronized Corant start(Class<?>... beanClasses) {
    if (isRuning()) {
      return this;
    }
    setMe(this);
    Thread.currentThread().setContextClassLoader(classLoader);
    StopWatch stopWatch = StopWatch.press(applicationName(),
        "Perform the handler before " + applicationName() + " starting");
    doBeforeStart(classLoader);

    final Logger logger = Logger.getLogger(Corant.class.getName());
    stopWatch.stop(tk -> log(logger, "%s in %s seconds.", tk.getTaskName(), tk.getTimeSeconds()))
        .start("Initializes the CDI container");

    Weld weld = new Weld();
    weld.setClassLoader(classLoader);
    weld.addExtensions(new CorantExtension());
    if (configClass != null) {
      weld.addBeanClass(configClass);
    }
    for (Class<?> beanClass : beanClasses) {
      weld.addBeanClass(beanClass);
    }
    container = weld.addProperty(Weld.SHUTDOWN_HOOK_SYSTEM_PROPERTY, true).initialize();
    id = container.getId();

    stopWatch.stop(tk -> log(logger, "%s in %s seconds.", tk.getTaskName(), tk.getTimeSeconds()))
        .start("Initializes all suites");

    doAfterContainerInitialized();

    stopWatch.stop(tk -> log(logger, "%s in %s seconds ", tk.getTaskName(), tk.getTimeSeconds()))
        .start("Perform the handler after corant startup");

    doAfterStarted(classLoader);

    stopWatch.stop(tk -> log(logger, "%s in %s seconds.", tk.getTaskName(), tk.getTimeSeconds()))
        .destroy(sw -> {
          double tt = sw.getTotalTimeSeconds();
          if (tt > 8) {
            log(logger,
                "Finished all initialization in %s seconds. It's been a long way, but we're here.",
                tt);
          } else {
            log(logger, "Finished all initialization in %s seconds.", tt);
          }
        });

    log(logger, "Finished at: %s.", Instant.now());
    log(logger, "Final memory: %sM/%sM/%sM, process id: %s.", LaunchUtils.getUsedMemoryMb(),
        LaunchUtils.getTotalMemoryMb(), LaunchUtils.getMaxMemoryMb(), LaunchUtils.getPid());
    printBoostLine();

    doOnReady();
    return this;
  }

  public synchronized void stop() {
    if (isRuning()) {
      LifecycleEventEmitter emitter = container.select(LifecycleEventEmitter.class).get();
      emitter.fire(new PreContainerStopEvent(args));
      container.close();
    }
    setMe(null);
  }

  void doAfterContainerInitialized() {
    LifecycleEventEmitter emitter = container.select(LifecycleEventEmitter.class).get();
    emitter.fire(new PostContainerStartedEvent(args));
  }

  void doAfterStarted(ClassLoader classLoader) {
    streamOf(ServiceLoader.load(CorantBootHandler.class, classLoader))
        .sorted(CorantBootHandler::compare)
        .forEach(h -> h.handleAfterStarted(this, Arrays.copyOf(args, args.length)));
  }

  void doBeforeStart(ClassLoader classLoader) {
    streamOf(ServiceLoader.load(CorantBootHandler.class, classLoader))
        .sorted(CorantBootHandler::compare)
        .forEach(h -> h.handleBeforeStart(classLoader, Arrays.copyOf(args, args.length)));
  }

  void doOnReady() {
    LifecycleEventEmitter emitter = container.select(LifecycleEventEmitter.class).get();
    emitter.fire(new PostCorantReadyEvent(args));
  }

  private void log(Logger logger, String msgOrFmt, Object... args) {
    if (args.length > 0) {
      logger.info(() -> String.format(msgOrFmt, args));
    } else {
      logger.info(() -> msgOrFmt);
    }
  }

  private void printBoostLine() {
    if (!setOf(args).contains("-disable_boost_line")) {
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
