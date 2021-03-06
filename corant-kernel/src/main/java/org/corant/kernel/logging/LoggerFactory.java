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

import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Classes.tryAsClass;
import static org.corant.shared.util.Objects.tryNewInstance;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import org.corant.shared.util.Methods;
import org.corant.shared.util.UnsafeAccessors;

/**
 * @author bingo 下午7:37:00
 *
 */
// @ApplicationScoped
public class LoggerFactory {

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
    LogManager.getLogManager().reset();
    Logger globalLogger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    globalLogger.setLevel(java.util.logging.Level.OFF);// FIXME openJDK weak reference
    Handler[] handlers = globalLogger.getHandlers();
    for (Handler handler : handlers) {
      globalLogger.removeHandler(handler);
    }
    try {
      Class<?> loggerCfgCls = tryAsClass("org.apache.logging.log4j.core.config.Configurator");
      if (loggerCfgCls != null) {
        Method method = Methods.getMatchingMethod(loggerCfgCls, "initialize",
            tryAsClass("org.apache.logging.log4j.core.config.Configuration"));
        method.invoke(null,
            tryNewInstance("org.apache.logging.log4j.core.config.NullConfiguration"));
      }
    } catch (Exception ignore) {
      // Noop
    }
  }

  @Produces
  @Dependent
  protected Logger createLogger(InjectionPoint injectionPoint) {
    return Logger.getLogger(getUserClass(injectionPoint.getMember().getDeclaringClass()).getName());
  }
}
