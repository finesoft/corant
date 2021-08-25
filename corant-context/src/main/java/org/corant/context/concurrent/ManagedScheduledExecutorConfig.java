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
package org.corant.context.concurrent;

import static org.corant.shared.util.Strings.defaultString;
import java.util.Locale;
import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.shared.normal.Names;

/**
 * corant-context
 *
 * @author bingo 下午7:57:40
 *
 */
@ConfigKeyRoot(value = "corant.concurrent.scheduled.executor", ignoreNoAnnotatedItem = false,
    keyIndex = 4)
public class ManagedScheduledExecutorConfig extends ManagedExecutorConfig {

  public static final String DFLT_NAME = Names.CORANT.toUpperCase(Locale.ROOT).concat("(SES)");
  public static final ManagedScheduledExecutorConfig DFLT_INST =
      new ManagedScheduledExecutorConfig(DFLT_NAME);

  private static final long serialVersionUID = 7985921715758101731L;

  public ManagedScheduledExecutorConfig() {}

  private ManagedScheduledExecutorConfig(String name) {
    setName(name);
  }

  @Override
  public String getThreadName() {
    return defaultString(threadName, defaultString(name, DFLT_NAME));
  }

}
