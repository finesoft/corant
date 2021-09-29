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
package org.corant.modules.logging;

import org.corant.Corant;
import org.corant.kernel.spi.CorantBootHandler;
import org.corant.shared.util.Systems;

/**
 * corant-modules-logging
 *
 * @author bingo 上午10:38:40
 *
 */
public class Log4jProvider implements CorantBootHandler {

  public static final String LOGMANAER_KEY = "java.util.logging.manager";
  public static final String JBOSS_LOGGER_KEY = "org.jboss.logging.provider";
  public static final String JUL_LOGMANAGER = "org.apache.logging.log4j.jul.LogManager";

  @Override
  public int getPriority() {
    return 0;
  }

  @Override
  public void handleAfterStarted(Corant corant, String... args) {}

  @Override
  public void handleBeforeStart(ClassLoader classLoader, String... args) {
    if (!JUL_LOGMANAGER.equals(Systems.getProperty(LOGMANAER_KEY))) {
      Systems.setProperty(LOGMANAER_KEY, JUL_LOGMANAGER);
    }
    if (!"log4j2".equals(Systems.getProperty(JBOSS_LOGGER_KEY))) {
      Systems.setProperty(JBOSS_LOGGER_KEY, "log4j2"); // FIXME
    }
  }

}
