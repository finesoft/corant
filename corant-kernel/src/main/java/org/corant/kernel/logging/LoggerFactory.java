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
package org.corant.kernel.logging;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Classes.tryAsClass;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Functions.emptyPredicate;
import static org.corant.shared.util.Iterables.search;
import static org.corant.shared.util.Methods.getMatchingMethod;
import static org.corant.shared.util.Objects.tryNewInstance;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.function.Predicate;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import org.corant.shared.util.UnsafeAccessors;

/**
 * @author bingo 下午7:37:00
 */
public class LoggerFactory {

  public static void addHandler(Handler handler, String... loggerNames) {
    maintainHandler(false, handler, loggerNames);
  }

  public static void disableAccessWarnings() {
    try {
      Object unsafe = UnsafeAccessors.get();
      Method putObjectVolatile = unsafe.getClass().getDeclaredMethod("putObjectVolatile",
          Object.class, long.class, Object.class);
      Method staticFieldOffset =
          unsafe.getClass().getDeclaredMethod("staticFieldOffset", Field.class);
      Class<?> loggerClass = Class.forName("jdk.internal.module.IllegalAccessLogger");
      Field loggerField = loggerClass.getDeclaredField("logger");
      Long offset = (Long) staticFieldOffset.invoke(unsafe, loggerField);
      putObjectVolatile.invoke(unsafe, loggerClass, offset, null);
    } catch (Exception ignored) {
    }
  }

  public static void disableLogger() {
    // JDK logger
    LogManager.getLogManager().reset();
    Logger globalLogger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    globalLogger.setLevel(java.util.logging.Level.OFF);// FIXME openJDK weak reference
    Handler[] handlers = globalLogger.getHandlers();
    for (Handler handler : handlers) {
      globalLogger.removeHandler(handler);
    }
    try {
      // Log4j2 logger
      Class<?> log4j2CfgCls = tryAsClass("org.apache.logging.log4j.core.config.Configurator");
      if (log4j2CfgCls != null) {
        Method method;
        // set root level OFF
        Class<?> log4j2LevelCls = tryAsClass("org.apache.logging.log4j.Level");
        Object off = getMatchingMethod(log4j2LevelCls, "toLevel", String.class).invoke(null, "OFF");
        method = getMatchingMethod(log4j2CfgCls, "setRootLevel", log4j2LevelCls);
        method.invoke(null, off);
        method = getMatchingMethod(log4j2CfgCls, "setAllLevels", String.class, log4j2LevelCls);
        method.invoke(null, "", off);
        // initialize NullConfiguration
        method = getMatchingMethod(log4j2CfgCls, "initialize",
            tryAsClass("org.apache.logging.log4j.core.config.Configuration"));
        method.invoke(null,
            tryNewInstance("org.apache.logging.log4j.core.config.NullConfiguration"));
      }
    } catch (Exception ignore) {
      // NOOP
    }
  }

  public static void removeHandler(Handler handler, String... loggerNames) {
    maintainHandler(true, handler, loggerNames);
  }

  static void maintainHandler(boolean remove, Handler handler, String... loggerNames) {
    shouldNotNull(handler);
    Enumeration<String> names = LogManager.getLogManager().getLoggerNames();
    Predicate<String> filter =
        isEmpty(loggerNames) ? emptyPredicate(true) : s -> search(loggerNames, s) != -1;
    if (names != null) {
      while (names.hasMoreElements()) {
        String name = names.nextElement();
        if (filter.test(name)) {
          if (remove) {
            Logger.getLogger(name).removeHandler(handler);
          } else {
            Logger.getLogger(name).addHandler(handler);
          }
        }
      }
    }
  }

  @Produces
  @Dependent
  protected Logger createLogger(InjectionPoint injectionPoint) {
    if (injectionPoint.getMember() != null) {
      return Logger
          .getLogger(getUserClass(injectionPoint.getMember().getDeclaringClass()).getName());
    } else {
      return Logger.getGlobal();
    }
  }
}
