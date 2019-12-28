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
package org.corant.suites.jta.shared;

import java.time.Duration;
import java.util.Optional;
import org.corant.config.declarative.ConfigKeyItem;
import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativeConfig;

/**
 * corant-suites-jta-shared
 *
 * @author bingo 下午9:11:21
 *
 */
@ConfigKeyRoot(value = "jta.transaction", keyIndex = 2, ignoreNoAnnotatedItem = false)
public class TransactionConfig implements DeclarativeConfig {

  static final TransactionConfig EMPTY = new TransactionConfig();

  @ConfigKeyItem
  boolean bindToJndi = false;

  @ConfigKeyItem
  Optional<Duration> timeout;

  @ConfigKeyItem
  boolean autoRecovery = false;

  public static TransactionConfig empty() {
    return EMPTY;
  }

  /**
   *
   * @return the timeout
   */
  public Optional<Duration> getTimeout() {
    return timeout;
  }

  /**
   *
   * @return the autoRecovery
   */
  public boolean isAutoRecovery() {
    return autoRecovery;
  }

  /**
   *
   * @return the bindToJndi
   */
  public boolean isBindToJndi() {
    return bindToJndi;
  }

}
