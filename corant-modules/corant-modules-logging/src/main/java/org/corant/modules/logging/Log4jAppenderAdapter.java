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

import java.io.Serializable;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;

/**
 * corant-modules-logging
 *
 * @author bingo 下午8:12:28
 *
 */
public class Log4jAppenderAdapter implements Appender {

  @Override
  public void append(LogEvent event) {}

  @Override
  public ErrorHandler getHandler() {
    return null;
  }

  @Override
  public Layout<? extends Serializable> getLayout() {
    return null;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public State getState() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean ignoreExceptions() {
    return false;
  }

  @Override
  public void initialize() {}

  @Override
  public boolean isStarted() {
    return false;
  }

  @Override
  public boolean isStopped() {
    return false;
  }

  @Override
  public void setHandler(ErrorHandler handler) {

  }

  @Override
  public void start() {

  }

  @Override
  public void stop() {}

}
