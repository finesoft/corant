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
import static org.corant.shared.util.CollectionUtils.setOf;
import static org.corant.shared.util.StreamUtils.streamOf;
import java.lang.annotation.Annotation;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.TimeZone;
import java.util.UUID;
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
import org.corant.shared.normal.Names;
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
 * In most circumstances the static {@link #run(Class, String[])} method can be called directly from
 * your {@literal main} method to bootstrap your application:
 *
 * <pre>
 * public class MyApplication {
 *   // ... Bean definitions
 *   public static void main(String[] arguments) throws Exception {
 *     try(Corant corant = Corant.run(MyApplication.class, arguments)){
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
 *   public static void main(String[] arguments) throws Exception {
 *    Corant corant = Corant.run(MyApplication.class, arguments)
 *   }
 * }
 * </pre>
 *
 * OR
 *
 * <pre>
 * public class MyApplication {
 *   // ... Bean definitions
 *   public static void main(String[] arguments) throws Exception {
 *     try (Corant corant =
 *         Corant.run(new Class[] {MyApplication.class, MyAnother.class}, arguments)) {
 *     }
 *   }
 * }
 * </pre>
 * <p>
 *
 * @see Corant#Corant(String...)
 * @see Corant#Corant(Class, String...)
 * @see Corant#Corant(ClassLoader, String...)
 * @see Corant#Corant(Class[], ClassLoader, String...)
 * @see CorantBootHandler
 * @see ApplicationConfigSourceProvider
 * @see ApplicationAdjustConfigSourceProvider
 * @see ApplicationProfileConfigSourceProvider
 *
 * @author bingo 上午11:52:09
 *
 */
public class Corant implements AutoCloseable {

  public static final String DISABLE_BOOST_LINE_CMD = "-disable_boost-line";
  public static final String DISABLE_BEFORE_START_HANDLER_CMD = "-disable_before-start-handler";
  public static final String DISABLE_AFTER_STARTED_HANDLER_CMD = "-disable_after-started-handler";

  private static volatile Corant me; // NOSONAR

  private final Class<?>[] beanClasses;
  private final String[] arguments;
  private final ClassLoader classLoader;
  private WeldContainer container;

  /**
   * Use the class loader of Corant.class as current thread context and the CDI container class
   * loader.
   */
  public Corant() {
    this(null, null, new String[0]);
  }

  /**
   * Use given config class and arguments construct Corant instance. If the given config class is
   * not null then the class loader of the current thread context and the CDI container will be set
   * with the given config class class loader. The given arguments will be propagate to all
   * CorantBootHandler and all CorantLifecycleEvent listeners.
   *
   * @see #Corant(Class, ClassLoader, String...)
   * @param configClass
   * @param arguments
   */
  public Corant(Class<?> configClass, String... arguments) {
    this(configClass == null ? null : new Class<?>[] {configClass},
        configClass != null ? configClass.getClassLoader() : null, arguments);
  }

  /**
   * Construct Coarnt instance with given bean classes and class loader and arguments. If the given
   * bean classes are not null then they will be added to the set of bean classes for the synthetic
   * bean archive. If the given class loader is not null then it will be set to the context
   * ClassLoader for current Thread and the CDI container class loader else we use Corant.class
   * class loader. The given arguments will be propagate to all CorantBootHandler and all
   * CorantLifecycleEvent listeners.
   *
   * @param beanClasses
   * @param classLoader
   * @param arguments
   */
  public Corant(Class<?>[] beanClasses, ClassLoader classLoader, String... arguments) {
    this.beanClasses =
        beanClasses == null ? new Class[0] : Arrays.copyOf(beanClasses, beanClasses.length);
    if (classLoader != null) {
      this.classLoader = classLoader;
    } else {
      this.classLoader = Corant.class.getClassLoader();
    }
    this.arguments = arguments;
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

  public static Corant current() {
    return me;
  }

  public static void fireAsyncEvent(Object event, Annotation... qualifiers) {
    if (event != null) {
      if (qualifiers.length > 0) {
        CDI.current().getBeanManager().getEvent().select(qualifiers).fireAsync(event);
      } else {
        CDI.current().getBeanManager().getEvent().fireAsync(event);
      }
    }
  }

  public static void fireEvent(Object event, Annotation... qualifiers) {
    if (event != null) {
      if (qualifiers.length > 0) {
        CDI.current().getBeanManager().getEvent().select(qualifiers).fire(event);
      } else {
        CDI.current().getBeanManager().getEvent().fire(event);
      }
    }
  }

  public static synchronized Corant run(Class<?> configClass, String... arguments) {
    Corant corant = new Corant(configClass, arguments);
    corant.start(null);
    return corant;
  }

  public static synchronized Corant run(Class<?>[] beanClasses) {
    Corant corant = new Corant(beanClasses, null);
    corant.start(null);
    return corant;
  }

  public static synchronized Corant run(Class<?>[] beanClasses, String... arguments) {
    Corant corant = new Corant(beanClasses, null, arguments);
    corant.start(null);
    return corant;
  }

  public static synchronized Corant run(ClassLoader classLoader, Consumer<Weld> preInitializer,
      String... arguments) {
    Corant corant = new Corant(classLoader, arguments);
    corant.start(preInitializer);
    return corant;
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

  @Override
  public void close() throws Exception {
    stop();
  }

  /**
   * This method is not normally used, and should be use CDI.current().getBeanManager(), except to
   * take advantage of some of the features of Weld.
   *
   * @return getBeanManager
   */
  public synchronized WeldManager getBeanManager() {
    shouldBeTrue(isRuning(), "The corant instance is null or is not in running");
    return (WeldManager) container.getBeanManager();
  }

  /**
   * 
   * @return the classLoader
   */
  public ClassLoader getClassLoader() {
    return classLoader;
  }

  public synchronized Object getId() {
    return container.getId();
  }

  public synchronized boolean isRuning() {
    return container != null && container.isRunning();
  }

  public synchronized void start(Consumer<Weld> preInitializer) {
    if (me != null) {
      return;
    } else {
      setMe(this);
    }
    Thread.currentThread().setContextClassLoader(classLoader);
    StopWatch stopWatch = StopWatch.press(applicationName(),
        "Perform the spi handlers before ".concat(applicationName()).concat(" starting"));
    doBeforeStart(classLoader);
    final Logger logger = Logger.getLogger(Corant.class.getName());
    stopWatch.stop(tk -> log(logger, "%s in %s seconds.", tk.getTaskName(), tk.getTimeSeconds()))
        .start("Initializes the CDI container");
    String id = Names.applicationName().concat("-weld-").concat(UUID.randomUUID().toString());
    Weld weld = new Weld(id);
    weld.setClassLoader(classLoader);
    weld.addExtensions(new CorantExtension());
    if (beanClasses != null) {
      weld.addBeanClasses(beanClasses);
    }
    if (preInitializer != null) {
      preInitializer.accept(weld);
    }
    container = weld.addProperty(Weld.SHUTDOWN_HOOK_SYSTEM_PROPERTY, true).initialize();

    stopWatch.stop(tk -> log(logger, "%s in %s seconds.", tk.getTaskName(), tk.getTimeSeconds()))
        .start("Initializes all suites");

    doAfterContainerInitialized();

    stopWatch.stop(tk -> log(logger, "%s in %s seconds ", tk.getTaskName(), tk.getTimeSeconds()))
        .start("Perform the spi handlers after corant startup");

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
    log(logger,
        "Final memory: %sM/%sM/%sM, process id: %s, java version: %s, default locale: %s, default timezone: %s.",
        LaunchUtils.getUsedMemoryMb(), LaunchUtils.getTotalMemoryMb(), LaunchUtils.getMaxMemoryMb(),
        LaunchUtils.getPid(), LaunchUtils.getJavaVersion(), Locale.getDefault(),
        TimeZone.getDefault().getID());
    printBoostLine();

    doOnReady();
  }

  public synchronized void stop() {
    if (isRuning()) {
      LifecycleEventEmitter emitter = container.select(LifecycleEventEmitter.class).get();
      emitter.fire(new PreContainerStopEvent(arguments));
      container.close();
    }
    container = null;
    setMe(null);
  }

  void doAfterContainerInitialized() {
    LifecycleEventEmitter emitter = container.select(LifecycleEventEmitter.class).get();
    emitter.fire(new PostContainerStartedEvent(arguments));
  }

  void doAfterStarted(ClassLoader classLoader) {
    if (setOf(arguments).contains(DISABLE_AFTER_STARTED_HANDLER_CMD)) {
      return;
    }
    streamOf(ServiceLoader.load(CorantBootHandler.class, classLoader))
        .sorted(CorantBootHandler::compare)
        .forEach(h -> h.handleAfterStarted(this, Arrays.copyOf(arguments, arguments.length)));
  }

  void doBeforeStart(ClassLoader classLoader) {
    if (setOf(arguments).contains(DISABLE_BEFORE_START_HANDLER_CMD)) {
      return;
    }
    streamOf(ServiceLoader.load(CorantBootHandler.class, classLoader))
        .sorted(CorantBootHandler::compare)
        .forEach(h -> h.handleBeforeStart(classLoader, Arrays.copyOf(arguments, arguments.length)));
  }

  void doOnReady() {
    LifecycleEventEmitter emitter = container.select(LifecycleEventEmitter.class).get();
    emitter.fire(new PostCorantReadyEvent(arguments));
  }

  private void log(Logger logger, String msgOrFmt, Object... arguments) {
    if (arguments.length > 0) {
      logger.info(() -> String.format(msgOrFmt, arguments));
    } else {
      logger.info(() -> msgOrFmt);
    }
  }

  private void printBoostLine() {
    if (!setOf(arguments).contains(DISABLE_BOOST_LINE_CMD)) {
      String spLine = "--------------------------------------------------";
      System.out.println(spLine.concat(spLine).concat("\n"));
    }
  }

  class CorantExtension implements Extension {
    void onAfterBeanDiscovery(@Observes AfterBeanDiscovery event) {
      event.addBean().addType(Corant.class).scope(ApplicationScoped.class)
          .addQualifier(Default.Literal.INSTANCE).addQualifier(Any.Literal.INSTANCE)
          .produceWith(obj -> Corant.this);
    }
  }
}
