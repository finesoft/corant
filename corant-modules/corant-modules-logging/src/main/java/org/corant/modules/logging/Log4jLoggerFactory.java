/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.logging;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * corant-modules-logging
 *
 * @author bingo 下午5:07:19
 *
 */
public class Log4jLoggerFactory {

  @Produces
  @Dependent
  Logger createLogger(InjectionPoint injectionPoint) {
    if (injectionPoint.getMember() != null) {
      return LogManager.getLogger(injectionPoint.getMember().getDeclaringClass().getName());
    } else {
      return LogManager.getRootLogger();
    }
  }
}
