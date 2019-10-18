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
package org.corant.suites.flyway;

import org.corant.config.resolve.ConfigKeyItem;
import org.corant.config.resolve.ConfigKeyRoot;
import org.corant.config.resolve.DeclarativeConfig;
import org.flywaydb.core.api.configuration.ClassicConfiguration;

/**
 * corant-suites-flyway
 *
 * @author bingo 下午12:48:02
 *
 */
@ConfigKeyRoot(value = "flyway.migrate", keyIndex = 2, ignoreNoAnnotatedItem = false)
public class FlywayConfig extends ClassicConfiguration implements DeclarativeConfig {

  public static final FlywayConfig EMPTY = new FlywayConfig(false, "META-INF/dbmigration");

  @ConfigKeyItem(defaultValue = "false")
  boolean enable;

  @ConfigKeyItem(defaultValue = "META-INF/dbmigration")
  String locationPrefix;

  public FlywayConfig() {

  }

  /**
   * @param enable
   * @param locationPrefix
   */
  protected FlywayConfig(boolean enable, String locationPrefix) {
    super();
    this.enable = enable;
    this.locationPrefix = locationPrefix;
  }

  /**
   *
   * @return the locationPrefix
   */
  public String getLocationPrefix() {
    return locationPrefix;
  }

  /**
   *
   * @return the enable
   */
  public boolean isEnable() {
    return enable;
  }

}
