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
package org.corant.suites.logging;

import org.corant.Corant;
import org.corant.kernel.spi.CorantBootHandler;

/**
 * corant-suites-logging
 *
 * @author bingo 上午10:38:40
 *
 */
public class Log4jProvider implements CorantBootHandler {

  @Override
  public void handleAfterStarted(Corant corant, String... args) {}

  @Override
  public void handleBeforeStart(ClassLoader classLoader, String... args) {
    // System.clearProperty("java.util.logging.manager");
    System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
  }

}
