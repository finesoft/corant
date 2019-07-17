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

import static org.corant.shared.util.ClassUtils.tryAsClass;
import java.lang.reflect.Method;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import org.corant.shared.util.MethodUtils;

/**
 * @author bingo 下午7:37:00
 *
 */
@ApplicationScoped
public class LoggerFactory {

  public static void disableLogger() {
    LogManager.getLogManager().reset();
    Logger globalLogger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    globalLogger.setLevel(java.util.logging.Level.OFF);
    Handler[] handlers = globalLogger.getHandlers();
    for (Handler handler : handlers) {
      globalLogger.removeHandler(handler);
    }
    try {
      Class<?> loggerCfgCls = tryAsClass("org.apache.logging.log4j.core.config.Configurator");
      if (loggerCfgCls != null) {
        Method method = MethodUtils.getMatchingMethod(loggerCfgCls, "initialize",
            tryAsClass("org.apache.logging.log4j.core.config.Configuration"));
        method.invoke(null,
            tryAsClass("org.apache.logging.log4j.core.config.NullConfiguration").newInstance());
      }
    } catch (Exception ignore) {
    }
  }

  @Produces
  Logger createLogger(InjectionPoint injectionPoint) {
    return Logger.getLogger(injectionPoint.getMember().getDeclaringClass().getName());
  }
}
