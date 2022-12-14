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
package org.corant.modules.flyway;

import org.corant.config.declarative.ConfigKeyItem;
import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativeConfig;
import org.flywaydb.core.api.configuration.ClassicConfiguration;

/**
 * corant-modules-flyway
 *
 * @author bingo 下午12:48:02
 *
 */
@ConfigKeyRoot(value = "corant.flyway.migrate", keyIndex = 3, ignoreNoAnnotatedItem = false)
public class FlywayConfig extends ClassicConfiguration implements DeclarativeConfig {

  public static final String DEFAULT_LOCATION = "META-INF/dbmigration";

  private static final long serialVersionUID = 3571486311594362397L;

  public static final FlywayConfig EMPTY = new FlywayConfig(false, DEFAULT_LOCATION);

  @ConfigKeyItem(defaultValue = "false")
  protected boolean enable;

  @ConfigKeyItem(defaultValue = DEFAULT_LOCATION)
  protected String locationPrefix;

  @ConfigKeyItem(defaultValue = "true")
  protected boolean useDriverManagerDataSource;

  public FlywayConfig() {

  }

  /**
   * @param enable
   * @param locationPrefix
   */
  protected FlywayConfig(boolean enable, String locationPrefix) {
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

  public boolean isUseDriverManagerDataSource() {
    return useDriverManagerDataSource;
  }

}
