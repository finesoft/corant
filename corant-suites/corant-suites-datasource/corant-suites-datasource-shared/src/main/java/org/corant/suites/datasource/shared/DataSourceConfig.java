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
package org.corant.suites.datasource.shared;

import static org.corant.shared.util.StringUtils.isNotBlank;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.corant.config.resolve.ConfigKeyItem;
import org.corant.config.resolve.ConfigKeyRoot;
import org.corant.config.resolve.DeclarativeConfig;
import org.corant.config.resolve.DeclarativePattern;
import org.corant.kernel.normal.Names.JndiNames;
import org.corant.kernel.util.Qualifiers.NamedQualifierObjectManager.AbstractNamedObject;
import org.corant.kernel.util.Unnamed;
import org.corant.shared.util.StringUtils;
import org.eclipse.microprofile.config.Config;

/**
 * corant-suites-datasource-shared
 *
 * @see Unnamed
 * @author bingo 下午3:45:45
 *
 */
@ConfigKeyRoot("datasource")
public class DataSourceConfig extends AbstractNamedObject implements DeclarativeConfig {

  public static final String JNDI_SUBCTX_NAME = JndiNames.JNDI_COMP_NME + "/Datasources";
  public static final String EMPTY_NAME = StringUtils.EMPTY;

  @ConfigKeyItem
  protected Class<?> driver;

  @ConfigKeyItem
  protected String username;

  @ConfigKeyItem
  protected String password;

  @ConfigKeyItem
  protected String connectionUrl;

  @ConfigKeyItem(defaultValue = "true")
  protected Boolean jta = true;

  @ConfigKeyItem(defaultValue = "true")
  protected Boolean autoCommit = true;

  @ConfigKeyItem(defaultValue = "false")
  protected Boolean connectable;

  @ConfigKeyItem(defaultValue = "true")
  protected Boolean xa = true;

  @ConfigKeyItem(defaultValue = "4")
  protected Integer initialSize = 4;

  @ConfigKeyItem(defaultValue = "0")
  protected Integer minSize = 0;

  @ConfigKeyItem(defaultValue = "64")
  protected Integer maxSize = 64;

  @ConfigKeyItem(defaultValue = "PT0S")
  protected Duration leakTimeout = Duration.ZERO;

  @ConfigKeyItem(defaultValue = "PT15M")
  protected Duration validationTimeout = Duration.ofMinutes(15);

  @ConfigKeyItem(defaultValue = "PT0S")
  protected Duration reapTimeout = Duration.ZERO;

  @ConfigKeyItem(defaultValue = "PT0S")
  protected Duration acquisitionTimeout = Duration.ZERO;

  @ConfigKeyItem(defaultValue = "true")
  protected Boolean validateConnection = true;

  @ConfigKeyItem(defaultValue = "false")
  protected Boolean enableMetrics = false;

  @ConfigKeyItem(pattern = DeclarativePattern.PREFIX)
  protected Map<String, Object> additionProperties = new HashMap<>();

  @ConfigKeyItem(defaultValue = "true")
  protected Boolean bindToJndi = true;

  /**
   *
   * @return the acquisitionTimeout
   */
  public Duration getAcquisitionTimeout() {
    return acquisitionTimeout;
  }

  /**
   *
   * @return the additionProperties
   */
  public Map<String, Object> getAdditionProperties() {
    return additionProperties;
  }

  /**
   *
   * @return the connectionUrl
   */
  public String getConnectionUrl() {
    return connectionUrl;
  }

  /**
   *
   * @return the driver
   */
  public Class<?> getDriver() {
    return driver;
  }

  /**
   *
   * @return the initialSize
   */
  public Integer getInitialSize() {
    return initialSize;
  }

  /**
   *
   * @return the leakTimeout
   */
  public Duration getLeakTimeout() {
    return leakTimeout;
  }

  /**
   *
   * @return the maxSize
   */
  public int getMaxSize() {
    return maxSize;
  }

  /**
   *
   * @return the minSize
   */
  public Integer getMinSize() {
    return minSize;
  }

  /**
   *
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  /**
   *
   * @return the reapTimeout
   */
  public Duration getReapTimeout() {
    return reapTimeout;
  }

  /**
   *
   * @return the username
   */
  public String getUsername() {
    return username;
  }

  /**
   *
   * @return the validationTimeout
   */
  public Duration getValidationTimeout() {
    return validationTimeout;
  }

  /**
   *
   * @return the autoCommit
   */
  public Boolean isAutoCommit() {
    return autoCommit;
  }

  public boolean isBindToJndi() {
    return bindToJndi;
  }

  /**
   *
   * @return the connectable
   */
  public Boolean isConnectable() {
    return connectable;
  }

  /**
   *
   * @return the enableMetrics
   */
  public boolean isEnableMetrics() {
    return enableMetrics;
  }

  /**
   *
   * @return the jta
   */
  public Boolean isJta() {
    return jta;
  }

  @Override
  public boolean isValid() {
    return isNotBlank(connectionUrl);
  }

  /**
   *
   * @return the validateConnection
   */
  public Boolean isValidateConnection() {
    return validateConnection;
  }

  /**
   *
   * @return the xa
   */
  public boolean isXa() {
    return xa;
  }

  @Override
  public void onPostConstruct(Config config, String key) {
    setName(key);
  }
}
