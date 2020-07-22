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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.corant.config.declarative.ConfigKeyItem;
import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativeConfig;
import org.corant.config.declarative.DeclarativePattern;
import org.corant.context.Unnamed;
import org.corant.context.Qualifiers.NamedQualifierObjectManager.AbstractNamedObject;
import org.corant.shared.normal.Names.JndiNames;
import org.corant.shared.util.Strings;
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

  private static final long serialVersionUID = -7623266774880311110L;
  public static final String JNDI_SUBCTX_NAME = JndiNames.JNDI_COMP_NME + "/Datasources";
  public static final String EMPTY_NAME = Strings.EMPTY;

  /**
   * JDBC driver class to use as a supplier of connections. Must be an implementation of
   * {@link java.sql.Driver}, {@link javax.sql.DataSource} or {@link javax.sql.XADataSource}. Can be
   * null, in which case the driver will be obtained from the URL (using the
   * {@link java.sql.DriverManager#getDriver(String)} mechanism).
   */
  @ConfigKeyItem
  protected Class<?> driver;

  /**
   * The principal to be authenticated in the database. Default is to don't perform authentication.
   */
  @ConfigKeyItem
  protected String username;

  /**
   * The credentials to use in order to authenticate to the database. Default is to don't provide
   * any credentials.
   */
  @ConfigKeyItem
  protected String password;

  /**
   * A SQL command to be executed when a connection is created.
   */
  @ConfigKeyItem
  protected String initialSql;

  /**
   * The database URL to connect to.
   */
  @ConfigKeyItem
  protected String connectionUrl;

  /**
   * Auto judge from Driver class
   */
  @ConfigKeyItem(defaultValue = "true")
  protected boolean jta = true;

  /**
   * If connections should have the auto-commit mode on by default. The transaction integration may
   * disable auto-commit when a connection in enrolled in a transaction.
   */
  @ConfigKeyItem(defaultValue = "true")
  protected boolean autoCommit = true;

  /**
   * Auto judge from Driver class
   */
  @ConfigKeyItem(defaultValue = "true")
  protected boolean xa = true;

  /**
   * The number of connections to be created when the pool starts. Can be smaller than min or bigger
   * than max.
   */
  @ConfigKeyItem(defaultValue = "0")
  protected int initialSize = 0;

  /**
   * The minimum number of connections on the pool. If the pool has to flush connections it may
   * create connections to keep this amount.
   */
  @ConfigKeyItem(defaultValue = "8")
  protected int minSize = 8;

  /**
   * The maximum number of connections on the pool. When the number of acquired connections is equal
   * to this value, further requests will block.
   */
  @ConfigKeyItem(defaultValue = "64")
  protected int maxSize = 64;

  /**
   * Connections acquired for longer than this time period may be reported as leaking. A duration of
   * {@link Duration#ZERO} means that a this feature is disabled.
   */
  @ConfigKeyItem(defaultValue = "PT0S")
  protected Duration leakTimeout = Duration.ofSeconds(0);

  /**
   * Connections that are older than this time period are flushed from the pool. A duration of
   * {@link Duration#ZERO} means that a this feature is disabled.
   */
  @ConfigKeyItem(defaultValue = "PT0M")
  protected Duration maxLifetime = Duration.ofMinutes(0);

  /**
   * Connections idle for longer than this time period are validated (background validation). A
   * duration of {@link Duration#ZERO} means that a this feature is disabled.
   */
  @ConfigKeyItem(defaultValue = "PT60M")
  protected Duration validationTimeout = Duration.ofMinutes(60);

  /**
   * Connections idle for longer than this time period are validated before being acquired
   * (foreground validation). A duration of {@link Duration#ZERO} means that a this feature is
   * disabled.
   */
  @ConfigKeyItem(defaultValue = "PT0S")
  protected Duration idleValidationTimeout = Duration.ofSeconds(0);

  /**
   * Connections idle for longer than this time period are flushed from the pool. A duration of
   * {@link Duration#ZERO} means that a this feature is disabled.
   */
  @ConfigKeyItem(defaultValue = "PT16S")
  protected Duration reapTimeout = Duration.ofSeconds(16L);

  /**
   * The maximum amount of time a thread may be blocked waiting for a connection. If this time
   * expires and still no connection is available, an exception is thrown. A duration of
   * {@link Duration#ZERO} means that a thread will wait indefinitely.
   */
  @ConfigKeyItem(defaultValue = "PT4S")
  protected Duration acquisitionTimeout = Duration.ofSeconds(4L);

  /**
   * Enable collects metrics
   */
  @ConfigKeyItem(defaultValue = "false")
  protected boolean enableMetrics = false;

  /**
   * Other unspecified properties to be passed into the JDBC driver when creating new connections.
   * NOTE: username and password properties are not allowed, these have to be set using the
   * principal / credentials mechanism.
   */
  @ConfigKeyItem(pattern = DeclarativePattern.PREFIX)
  protected Map<String, String> jdbcProperties = new HashMap<>();

  /**
   * If JDBC resources ({@link java.sql.Statement} and {@link java.sql.ResultSet}) should be tracked
   * to be closed if leaked.
   */
  @ConfigKeyItem(defaultValue = "true")
  protected boolean enableTrackJdbcResources = true;

  /**
   * Whether this data source bind to jndi
   */
  @ConfigKeyItem(defaultValue = "false")
  protected boolean bindToJndi = false;

  /**
   *
   * @return the acquisitionTimeout
   */
  public Duration getAcquisitionTimeout() {
    return acquisitionTimeout;
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
   * @return the idleValidationTimeout
   */
  public Duration getIdleValidationTimeout() {
    return idleValidationTimeout;
  }

  /**
   *
   * @return the initialSize
   */
  public int getInitialSize() {
    return initialSize;
  }

  /**
   *
   * @return the initialSql
   */
  public String getInitialSql() {
    return initialSql;
  }

  /**
   *
   * @return the jdbcProperties
   */
  public Map<String, String> getJdbcProperties() {
    return jdbcProperties;
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
   * @return the maxLifetime
   */
  public Duration getMaxLifetime() {
    return maxLifetime;
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
  public int getMinSize() {
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
  public boolean isAutoCommit() {
    return autoCommit;
  }

  /**
   *
   * @return the bindToJndi
   */
  public boolean isBindToJndi() {
    return bindToJndi;
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
   * @return the enableTrackJdbcResources
   */
  public boolean isEnableTrackJdbcResources() {
    return enableTrackJdbcResources;
  }

  /**
   *
   * @return the jta
   */
  public boolean isJta() {
    return jta;
  }

  @Override
  public boolean isValid() {
    return driver != null;
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
