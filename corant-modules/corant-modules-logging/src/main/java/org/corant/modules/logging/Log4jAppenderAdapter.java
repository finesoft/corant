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

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.io.Serializable;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.layout.SyslogLayout;

/**
 * corant-modules-logging
 *
 * @author bingo 下午8:12:28
 *
 */
public class Log4jAppenderAdapter extends AbstractAppender {

  public Log4jAppenderAdapter(String name) {
    this(name, new NoOpFilter(), SyslogLayout.newBuilder().build(), true, new Property[0]);
  }

  public Log4jAppenderAdapter(String name, Filter filter, Layout<? extends Serializable> layout,
      boolean ignoreExceptions, Property[] properties) {
    super(shouldNotNull(name), filter, layout, ignoreExceptions, properties);
  }

  @Override
  public void append(LogEvent event) {}

  static class NoOpFilter extends AbstractFilter {

    public NoOpFilter() {
      super(Result.NEUTRAL, Result.NEUTRAL);
    }
  }
}
