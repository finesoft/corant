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

import static org.corant.shared.normal.Names.applicationName;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.MBeans.deregisterFromMBean;
import static org.corant.shared.util.MBeans.registerToMBean;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Streams.streamOf;
import java.lang.annotation.Annotation;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.Extension;
import org.corant.kernel.boot.Power;
import org.corant.kernel.event.CorantLifecycleEvent.LifecycleEventEmitter;
import org.corant.kernel.event.PostContainerStartedEvent;
import org.corant.kernel.event.PostCorantReadyEvent;
import org.corant.kernel.event.PreContainerStopEvent;
import org.corant.kernel.spi.CorantBootHandler;
import org.corant.kernel.util.Launchs;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Annotations;
import org.corant.shared.util.Classes;
import org.corant.shared.util.StopWatch;
import org.corant.shared.util.Strings;

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
 * container class loader and add configuration classes to the set of bean classes for the synthetic
 * bean archive if necessary.</li>
 * <li>Construct the CDI container and initialize it, after the CDI container initialized then fire
 * PostContainerStartedEvent to listeners, those listeners may be use to configure some components
 * after CDI initialized such as web server.</li>
 * <li>After the above execution was completed, fire PostCorantReadyEvent to listeners.</li>
 * </ul>
 * </p>
 *
 * <p>
 * In most circumstances the static {@link #startup(Class, String[])} method can be called directly
 * from your {@literal main} method to bootstrap your application:
 *
 * <pre>
 * public class MyApplication {
 *   // ... Bean definitions
 *   public static void main(String[] arguments) throws Exception {
 *     try(Corant corant = Corant.startup(MyApplication.class, arguments)){
 *      //... some works in CDI
 *   }
 * }
 * </pre>
 *
 * <pre>
 * public class MyApplication {
 *   // ... Bean definitions
 *   public static void main(String[] arguments) throws Exception {
 *    Corant corant = Corant.startup(MyApplication.class, arguments)
 *   }
 * }
 * </pre>
 *
 * <pre>
 * public class MyApplication {
 *   // ... Bean definitions
 *   public static void main(String[] arguments) throws Exception {
 *     try (Corant corant =
 *         Corant.startup(new Class[] {MyApplication.class, MyAnother.class}, arguments)) {
 *     }
 *   }
 * }
 * </pre>
 *
 * You can also use the following code snippets to do some work:
 *
 * <pre>
 * Corant.run(() -> {
 *   // ... some runnable works in CDI
 * });
 * </pre>
 *
 * <pre>
 * Corant.call(MyBean.class).myBeanMethod();
 * </pre>
 *
 * <pre>
 * return Corant.supplier(() -> {
 *   // ... some supplier works in CDI
 * });
 * </pre>
 * <p>
 *
 * @see Corant#Corant(String...)
 * @see Corant#Corant(Class, String...)
 * @see Corant#Corant(ClassLoader, String...)
 * @see Corant#Corant(Class[], ClassLoader, String...)
 * @see CorantBootHandler
 *
 * @author bingo 上午11:52:09
 *
 */
public class Corant implements AutoCloseable {

  public static final String DISABLE_BOOST_LINE_CMD = "-disable_boost-line";
  public static final String DISABLE_BEFORE_START_HANDLER_CMD = "-disable_before-start-handler";
  public static final String DISABLE_AFTER_STARTED_HANDLER_CMD = "-disable_after-started-handler";
  public static final String REGISTER_TO_MBEAN_CMD = "-register_to_mbean";
  public static final String POWER_MBEAN_NAME = applicationName() + ":type=kernel,name=Power";

  private static volatile Corant me; // NOSONAR

  private final Class<?>[] beanClasses;
  private final String[] arguments;
  private final ClassLoader classLoader;
  private SeContainer container;
  private volatile Power power;

  /**
   * Use the class loader of Corant.class as current thread context and the CDI container class
   * loader.
   */
  public Corant() {
    this(null, null, Strings.EMPTY_ARRAY);
  }

  /**
   * Use given config class(synthetic bean class) and arguments construct Corant instance. If the
   * given config class is not null then the class loader of the current thread context and the CDI
   * container will be set with the given config class class loader. The given arguments will be
   * propagate to all CorantBootHandler and all CorantLifecycleEvent listeners.
   *
   * @see #Corant(Class[], ClassLoader, String...)
   * @param configClass
   * @param arguments
   */
  public Corant(Class<?> configClass, String... arguments) {
    this(configClass == null ? null : new Class<?>[] {configClass},
        configClass != null ? configClass.getClassLoader() : null, arguments);
  }

  /**
   * Construct Corant instance with given bean classes(synthetic bean class) and class loader and
   * arguments. If the given bean classes are not null then they will be added to the set of bean
   * classes for the synthetic bean archive. If the given class loader is not null then it will be
   * set to the context ClassLoader for current Thread and the CDI container class loader else we
   * use Corant.class class loader. The given arguments will be propagate to all CorantBootHandler
   * and all CorantLifecycleEvent listeners.
   *
   * @param beanClasses
   * @param classLoader
   * @param arguments
   */
  public Corant(Class<?>[] beanClasses, ClassLoader classLoader, String... arguments) {
    this.beanClasses =
        beanClasses == null ? Classes.EMPTY_ARRAY : Arrays.copyOf(beanClasses, beanClasses.length);
    if (classLoader != null) {
      this.classLoader = classLoader;
    } else {
      this.classLoader = Corant.class.getClassLoader();
    }
    this.arguments =
        arguments == null ? Strings.EMPTY_ARRAY : Arrays.copyOf(arguments, arguments.length);
    setMe(this);
  }

  /**
   * Use given class loader and arguments to construct Corant instance. If the given class loader is
   * not null then the class loader of the current thread context and the CDI container will be set
   * with the given class loader.The given arguments will be propagate to all CorantBootHandler and
   * all CorantLifecycleEvent listeners.
   *
   * @param classLoader
   * @param arguments
   */
  public Corant(ClassLoader classLoader, String... arguments) {
    this(null, classLoader, arguments);
  }

  /**
   * Use given arguments and the class loader of Corant.class to construct Corant instance.The given
   * arguments will be propagate to all CorantBootHandler and all CorantLifecycleEvent listeners.
   *
   * @param arguments
   */
  public Corant(String... arguments) {
    this(null, null, arguments);
  }

  /**
   * Return a CDI bean instance through the bean class and qualifiers; mainly used for temporary
   * works. If the Coarnt application is not started, this method will try to start it with incoming
   * arguments, and this method does not directly close it.
   *
   * @param <T> The bean type
   * @param synthetic Whether the bean class is synthetic
   * @param beanClass The bean class
   * @param annotations The qualifiers
   * @param arguments The application arguments
   * @return The managed bean instance
   */
  public static synchronized <T> T call(boolean synthetic, Class<T> beanClass,
      Annotation[] annotations, String[] arguments) {
    if (current() == null) {
      if (synthetic) {
        startup(beanClass, arguments);
      } else {
        startup(Classes.EMPTY_ARRAY, arguments);
      }
    } else if (!current().isRunning()) {
      current().start(null);
    }
    return CDI.current().select(beanClass, annotations).get();
  }

  /**
   * Return a CDI bean instance through the bean class(non synthetic) and qualifiers; mainly used
   * for temporary works. If the Coarnt application is not started, this method will try to start
   * it, and this method does not directly close it.
   *
   * @param <T>
   * @param beanClass The bean classes
   * @param annotations The bean qualifiers
   * @return The managed bean instance
   */
  public static synchronized <T> T call(Class<T> beanClass, Annotation... annotations) {
    return call(false, beanClass, annotations, Strings.EMPTY_ARRAY);
  }

  /**
   * Return a CDI bean instance through the bean class(synthetic); mainly used for temporary works.
   * If the Coarnt application is not started, this method will try to start it, and this method
   * does not directly close it.
   *
   * @param <T>
   * @param beanClass
   * @param arguments
   * @return callSynthetic
   */
  public static synchronized <T> T callSynthetic(Class<T> beanClass, String... arguments) {
    return call(true, beanClass, Annotations.EMPTY_ARRAY, arguments);
  }

  /**
   * Return the current Corant instance, may be return null if the Corant application isn't started.
   *
   * @return current
   */
  public static Corant current() {
    return me;
  }

  /**
   * Run a runnable program in the CDI environment. This method will try to start the Corant
   * application and automatically close it after the runnable executed.
   *
   * @param runnable
   * @param arguments
   */
  public static synchronized void run(Runnable runnable, String... arguments) {
    try {
      if (current() == null) {
        startup(Classes.EMPTY_ARRAY, arguments);
      } else if (!current().isRunning()) {
        current().start(null);
      }
      runnable.run();
    } catch (Throwable e) {
      throw new CorantRuntimeException(e);
    } finally {
      if (current() != null && current().isRunning()) {
        current().stop();
      }
    }
  }

  /**
   * Shut down the Corant application, If you want to start it again, you can only start it through
   * the {@link #startup()} method, or instantiate Coarnt and call the {@link #start(Consumer)}
   * method.
   */
  public static synchronized void shutdown() {
    if (current() != null) {
      if (current().isRunning()) {
        current().stop();
      }
      if (current().power() != null) {
        deregisterFromMBean(POWER_MBEAN_NAME);
      }
      me = null;
    }
  }

  /**
   * Startup the Coarnt application, construct Corant instance and start it.
   *
   * @return The Coarnt instance
   */
  public static synchronized Corant startup() {
    return startup(Classes.EMPTY_ARRAY, null, null);
  }

  /**
   * Use the specified synthetic bean classes to construct Corant instance and start it.
   *
   * @param beanClasses The synthetic bean classes
   * @return The Corant instance
   */
  public static synchronized Corant startup(Class<?>... beanClasses) {
    return startup(beanClasses, null, null);
  }

  /**
   * Use the specified synthetic config class and arguments to construct Corant instance and start
   * it, the incoming arguments will be propagate to all CorantBootHandler and all
   * CorantLifecycleEvent listeners.
   *
   * @param configClass The synthetic bean class
   * @param arguments
   * @return The Corant instance
   */
  public static synchronized Corant startup(Class<?> configClass, String[] arguments) {
    return startup(new Class[] {configClass}, null, null, arguments);
  }

  /**
   * The complete startup method, this method will use incoming bean classes(synthetic) and class
   * loader and arguments to construct Corant instance and start it, before start this method also
   * provide a pre-initializer callback to handle before the CDI container initialize. The
   * pre-initializer can use for configure the CDI container before initialize it.
   *
   * @param beanClasses The synthetic bean class
   * @param classLoader The class loader use for the current thread context and the CDI container
   * @param preInitializer The pre-initializer callback use for configure the CDI container
   * @param arguments The application arguments can be propagated to related processors
   * @return The Corant instance
   */
  public static synchronized Corant startup(Class<?>[] beanClasses, ClassLoader classLoader,
      Consumer<SeContainerInitializer> preInitializer, String... arguments) {
    Corant corant = new Corant(beanClasses, classLoader, arguments);
    corant.start(preInitializer);
    return corant;
  }

  /**
   * Use the specified synthetic bean classes and arguments to construct Corant instance and start
   * it, the incoming arguments will be propagate to all CorantBootHandler and
   * allCorantLifecycleEvent listeners.
   *
   * @param beanClasses The synthetic bean class
   * @param arguments The application arguments can be propagated to related processors
   * @return The Corant instance
   *
   */
  public static synchronized Corant startup(Class<?>[] beanClasses, String[] arguments) {
    return startup(beanClasses, null, null, arguments);
  }

  /**
   * Use the specified arguments to construct Corant instance and start it, the incoming arguments
   * will be propagate to all CorantBootHandler and all CorantLifecycle Event listeners, The
   * incoming pre-initializer can use for configure the CDI container before initialize it.
   *
   * @param preInitializer The pre-initializer callback use for configure CDI container
   * @param arguments The application arguments can be propagated to related processors
   * @return The Corant instance
   */
  public static synchronized Corant startup(Consumer<SeContainerInitializer> preInitializer,
      String... arguments) {
    return startup(null, null, preInitializer, arguments);
  }

  /**
   * Use the specified arguments to construct Corant instance and start it, the incoming arguments
   * will be propagate to all CorantBootHandler and allCorantLifecycleEvent listeners.
   *
   * @param arguments The application arguments can be propagated to related processors
   * @return The Corant instance
   */
  public static synchronized Corant startup(String... arguments) {
    return startup(Classes.EMPTY_ARRAY, null, null, arguments);
  }

  /**
   * Run a supplier program in the CDI environment. This method will try to start the Corant
   * application and automatically close the Corant application after execution.
   *
   * @param <T>
   * @param supplier The supplier that will be invoked after Corant started
   * @param arguments The application arguments
   * @return The result
   */
  public static synchronized <T> T supplier(Supplier<T> supplier, String... arguments) {
    try {
      if (current() == null) {
        startup(Classes.EMPTY_ARRAY, arguments);
      } else if (!current().isRunning()) {
        current().start(null);
      }
      return supplier.get();
    } catch (Throwable e) {
      throw new CorantRuntimeException(e);
    } finally {
      if (current() != null && current().isRunning()) {
        current().stop();
      }
    }
  }

  private static synchronized void setMe(Corant me) {
    if (me != null) {
      shouldBeTrue(Corant.me == null, "We already have an instance of Corant. Don't repeat it!");
    }
    Corant.me = me;
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

  /**
   * Stop the Corant application, one can restart it.
   */
  @Override
  public void close() throws Exception {
    stop();
  }

  /**
   * This method is not normally used, and should be use CDI.current().getBeanManager().
   *
   * @return getBeanManager
   */
  public synchronized BeanManager getBeanManager() {
    shouldBeTrue(isRunning(), "The corant instance is null or is not in running");
    return container.getBeanManager();
  }

  /**
   * Return the application ClassLoader.
   */
  public ClassLoader getClassLoader() {
    return classLoader;
  }

  /**
   * Return whether the application is running
   *
   * @see SeContainer#isRunning()
   */
  public synchronized boolean isRunning() {
    return container != null && container.isRunning();
  }

  /**
   * Start the Corant application. Configure the CDI container and initialize it, call the
   * appropriate pre-post boot handler and fire some appropriate events, the pre-initializer is used
   * to configure the CDI container before it starts.
   *
   * @param preInitializer start
   */
  public synchronized void start(Consumer<SeContainerInitializer> preInitializer) {
    if (isRunning()) {
      return;
    }
    final StopWatch stopWatch = new StopWatch(applicationName());
    Thread.currentThread().setContextClassLoader(classLoader);
    doBeforeStart(classLoader, stopWatch);
    registerMBean();
    initializeContainer(preInitializer, stopWatch);
    doAfterContainerInitialized(stopWatch);
    doAfterStarted(classLoader, stopWatch);
    doOnReady();
  }

  /**
   * Stop the Corant application. Call the appropriate pre-post boot handler and fire some
   * appropriate events, after calling this method, you can continue to restart the Corant
   * application.
   */
  public synchronized void stop() {
    if (isRunning()) {
      LifecycleEventEmitter emitter = container.select(LifecycleEventEmitter.class).get();
      emitter.fire(new PreContainerStopEvent(arguments));
      container.close();
    }
    container = null;
    streamOf(ServiceLoader.load(CorantBootHandler.class, classLoader))
        .sorted(CorantBootHandler::compare).forEach(h -> {
          try {
            h.handleAfterStopped(classLoader, Arrays.copyOf(arguments, arguments.length));
            h.close();
          } catch (Exception e) {
            log("After stopped handler %s occurred error %s.", h.getClass().getName(),
                e.getMessage());
          }
        });
    log("Stopped %s at %s.\n", applicationName(), Instant.now());
  }

  void doAfterContainerInitialized(StopWatch stopWatch) {
    stopWatch.start("All suites are initialized");

    LifecycleEventEmitter emitter = container.select(LifecycleEventEmitter.class).get();
    emitter.fire(new PostContainerStartedEvent(arguments));

    stopWatch.stop(t -> log("%s, takes %ss.", t.getName(), t.getTimeSeconds()));
  }

  void doAfterStarted(ClassLoader classLoader, StopWatch stopWatch) {
    stopWatch.start("The post-started SPI processing is completed");

    if (hasCommandArgument(DISABLE_AFTER_STARTED_HANDLER_CMD)) {
      return;
    }
    streamOf(ServiceLoader.load(CorantBootHandler.class, classLoader))
        .sorted(CorantBootHandler::compare)
        .forEach(h -> h.handleAfterStarted(this, Arrays.copyOf(arguments, arguments.length)));

    stopWatch.stop(tk -> log("%s, takes %ss.", tk.getName(), tk.getTimeSeconds())).destroy(sw -> {
      double tt = sw.getTotalTimeSeconds();
      if (tt > 8) {
        log("The %s has been started, takes %ss. It's been a long way, but we're here.",
            applicationName(), tt);
      } else {
        log("The %s has been started, takes %ss.", applicationName(), tt);
      }
    });

    log("Default setting: process id: %s, java version: %s, locale: %s, timezone: %s.",
        Launchs.getPid(), Launchs.getJavaVersion(), Locale.getDefault(),
        TimeZone.getDefault().getID());
    log("Final memory: %sM/%sM/%sM%s.", Launchs.getUsedMemoryMb(), Launchs.getTotalMemoryMb(),
        Launchs.getMaxMemoryMb(), boostLine());
  }

  void doBeforeStart(ClassLoader classLoader, StopWatch stopWatch) {
    stopWatch
        .start("Starting " + applicationName() + ", the pre-start SPI processing is completed");
    if (hasCommandArgument(DISABLE_BEFORE_START_HANDLER_CMD)) {
      return;
    }
    streamOf(ServiceLoader.load(CorantBootHandler.class, classLoader))
        .sorted(CorantBootHandler::compare)
        .forEach(h -> h.handleBeforeStart(classLoader, Arrays.copyOf(arguments, arguments.length)));
    stopWatch.stop(t -> log("%s, takes %ss.", t.getName(), t.getTimeSeconds()));
  }

  void doOnReady() {
    LifecycleEventEmitter emitter = container.select(LifecycleEventEmitter.class).get();
    emitter.fire(new PostCorantReadyEvent(arguments));
  }

  synchronized void initializeContainer(Consumer<SeContainerInitializer> preInitializer,
      StopWatch stopWatch) {
    stopWatch.start("Initialization of the CDI container is completed");
    // String id = applicationName().concat("-weld-").concat(UUID.randomUUID().toString());
    SeContainerInitializer initializer = SeContainerInitializer.newInstance();// new Weld(id);
    initializer.setClassLoader(classLoader);
    initializer.addExtensions(new CorantExtension());
    if (beanClasses != null) {
      initializer.addBeanClasses(beanClasses);
    }
    if (preInitializer != null) {
      preInitializer.accept(initializer);
    }
    container = initializer.initialize();
    stopWatch.stop(t -> log("%s, takes %ss.", t.getName(), t.getTimeSeconds()));
  }

  boolean registerMBean() {
    if (!hasCommandArgument(REGISTER_TO_MBEAN_CMD)) {
      return false;
    }
    synchronized (this) {
      if (power == null) {
        power = new Power(beanClasses, arguments);
        registerToMBean(POWER_MBEAN_NAME, power);
        Runtime.getRuntime()
            .addShutdownHook(new Thread(() -> deregisterFromMBean(POWER_MBEAN_NAME)));
      }
      log("Registered %s to MBean server, one can use it for shutdown or re-startup the application.",
          applicationName(), Instant.now());
      return true;
    }

  }

  private String boostLine() {
    if (!hasCommandArgument(DISABLE_BOOST_LINE_CMD)) {
      String spLine = "--------------------------------------------------";
      return "\n".concat(spLine).concat(spLine);
    }
    return "";
  }

  private boolean hasCommandArgument(String cmd) {
    for (String argument : arguments) {
      if (areEqual(argument, cmd)) {
        return true;
      }
    }
    return false;
  }

  private void log(String msgOrFmt, Object... arguments) {
    if (arguments.length > 0) {
      Logger.getLogger(Corant.class.getName()).info(() -> String.format(msgOrFmt, arguments));
    } else {
      Logger.getLogger(Corant.class.getName()).info(() -> msgOrFmt);
    }
  }

  private Power power() {
    return power;
  }

  class CorantExtension implements Extension {
    void onAfterBeanDiscovery(@Observes AfterBeanDiscovery event) {
      event.addBean().addType(Corant.class).scope(ApplicationScoped.class)
          .addQualifier(Default.Literal.INSTANCE).addQualifier(Any.Literal.INSTANCE)
          .produceWith(obj -> Corant.this);
    }
  }
}
