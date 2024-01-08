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
package org.corant.modules.datasource.shared;

import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Strings.isNotBlank;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.corant.config.declarative.ConfigKeyItem;
import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativeConfig;
import org.corant.config.declarative.DeclarativePattern;
import org.corant.context.qualifier.Qualifiers;
import org.corant.context.qualifier.Qualifiers.NamedQualifierObjectManager.AbstractNamedObject;
import org.corant.context.qualifier.Unnamed;
import org.corant.shared.normal.Names.JndiNames;
import org.eclipse.microprofile.config.Config;

/**
 * corant-modules-datasource-shared
 *
 * @see Unnamed
 * @author bingo 下午3:45:45
 */
@ConfigKeyRoot(value = "corant.datasource", keyIndex = 2)
public class DataSourceConfig extends AbstractNamedObject implements DeclarativeConfig {

  private static final long serialVersionUID = -7623266774880311110L;
  public static final String JNDI_SUBCTX_NAME = JndiNames.JNDI_COMP_NME + "/Datasources";
  public static final String EMPTY_NAME = Qualifiers.EMPTY_NAME;
  protected static final Logger logger = Logger.getLogger(DataSourceConfig.class.getName());

  @ConfigKeyItem
  protected Class<?> driver;

  @ConfigKeyItem
  protected String username;

  @ConfigKeyItem
  protected String password;

  @ConfigKeyItem
  protected String initialSql;

  @ConfigKeyItem
  protected String connectionUrl;

  @ConfigKeyItem(defaultValue = "true")
  protected boolean jta = true;

  @ConfigKeyItem(defaultValue = "true")
  protected boolean autoCommit = true;

  @ConfigKeyItem(defaultValue = "true")
  protected boolean xa = true;

  @ConfigKeyItem(defaultValue = "0")
  protected int initialSize = 0;

  @ConfigKeyItem(defaultValue = "2")
  protected int minSize = 2;

  @ConfigKeyItem(defaultValue = "64")
  protected int maxSize = 64;

  @ConfigKeyItem(defaultValue = "-1")
  protected int isolationLevel = -1;

  @ConfigKeyItem(defaultValue = "PT0S")
  protected Duration loginTimeout = Duration.ofSeconds(0);

  @ConfigKeyItem(defaultValue = "PT0S")
  protected Duration leakTimeout = Duration.ofSeconds(0);

  @ConfigKeyItem(defaultValue = "PT0M")
  protected Duration maxLifetime = Duration.ofMinutes(0);

  @ConfigKeyItem(defaultValue = "PT3M")
  protected Duration validationTimeout = Duration.ofMinutes(3);

  @ConfigKeyItem(defaultValue = "PT30M")
  protected Duration idleValidationTimeout = Duration.ofMinutes(30);

  @ConfigKeyItem(defaultValue = "PT16S")
  protected Duration reapTimeout = Duration.ofSeconds(16L);

  @ConfigKeyItem(defaultValue = "PT4S")
  protected Duration acquisitionTimeout = Duration.ofSeconds(4L);

  @ConfigKeyItem(defaultValue = "false")
  protected boolean enableMetrics = false;

  @ConfigKeyItem(pattern = DeclarativePattern.PREFIX)
  protected Map<String, String> jdbcProperties = new HashMap<>();

  @ConfigKeyItem(pattern = DeclarativePattern.PREFIX)
  protected Map<String, String> ctrlProperties = new HashMap<>();

  @ConfigKeyItem(defaultValue = "true")
  protected boolean enableTrackJdbcResources = true;

  @ConfigKeyItem(defaultValue = "false")
  protected boolean bindToJndi = false;

  @ConfigKeyItem(defaultValue = "true")
  protected boolean enable = true;

  @ConfigKeyItem(defaultValue = "true")
  protected boolean enableCustomTransactionIntegration;

  @ConfigKeyItem(defaultValue = "false")
  protected boolean verifyDeployment;

  /**
   * The maximum amount of time a thread may be blocked waiting for a connection. If this time
   * expires and still no connection is available, an exception is thrown. A duration of
   * {@link Duration#ZERO} means that a thread will wait indefinitely. Default is 4 seconds.
   */
  public Duration getAcquisitionTimeout() {
    return acquisitionTimeout;
  }

  /**
   * The database URL to connect to.
   */
  public String getConnectionUrl() {
    return connectionUrl;
  }

  /**
   * Return specific control configuration information, such as returning specific configuration
   * information for a specific connection pool, etc.
   */
  public Map<String, String> getCtrlProperties() {
    return ctrlProperties;
  }

  /**
   * JDBC driver class to use as a supplier of connections. Must be an implementation of
   * {@link java.sql.Driver}, {@link javax.sql.DataSource} or {@link javax.sql.XADataSource}. Can be
   * null, in which case the driver will be obtained from the URL (using the
   * {@link java.sql.DriverManager#getDriver(String)} mechanism).
   */
  public Class<?> getDriver() {
    return driver;
  }

  /**
   * Connections idle for longer than this time period are validated before being acquired
   * (foreground validation). A duration of {@link Duration#ZERO} means that this feature is
   * disabled. Default is 30 minutes.
   */
  public Duration getIdleValidationTimeout() {
    return idleValidationTimeout;
  }

  /**
   * The number of connections to be created when the pool starts. Can be smaller than min or bigger
   * than max. Default is 0.
   */
  public int getInitialSize() {
    return initialSize;
  }

  /**
   * A SQL command to be executed when a connection is created.
   */
  public String getInitialSql() {
    return initialSql;
  }

  /**
   * Isolation level for connections. The Isolation level must be one of the following:
   * <p>
   * <ul>
   * <li>Connection.TRANSACTION_NONE,
   * <li>Connection.TRANSACTION_READ_ UNCOMMITTED,
   * <li>Connection.TRANSACTION_READ_COMMITTED,
   * <li>Connection.TRANSACTION_REPEATABLE_READ,
   * <li>Connection.TRANSACTION_SERIALIZABLE
   * </ul>
   * <p>
   * Default -1 is vendor-specific.
   *
   * @since 1.1
   */
  public int getIsolationLevel() {
    return isolationLevel;
  }

  /**
   * Other unspecified properties to be passed into the JDBC driver when creating new connections.
   * NOTE: username and password properties are not allowed, these have to be set using the
   * principal / credentials mechanism.
   */
  public Map<String, String> getJdbcProperties() {
    return jdbcProperties;
  }

  /**
   * Connections acquired for longer than this time period may be reported as leaking. A duration of
   * {@link Duration#ZERO} means that this feature is disabled. Default is disabled.
   */
  public Duration getLeakTimeout() {
    return leakTimeout;
  }

  /**
   * Maximum time to wait while attempting to connect to a database. Resolution in seconds.
   */
  public Duration getLoginTimeout() {
    return loginTimeout;
  }

  /**
   * Connections that are older than this time period are flushed from the pool. A duration of
   * {@link Duration#ZERO} means that this feature is disabled. Default is disabled.
   */
  public Duration getMaxLifetime() {
    return maxLifetime;
  }

  /**
   * The maximum number of connections on the pool. When the number of acquired connections is equal
   * to this value, further requests will block. Default is 64.
   */
  public int getMaxSize() {
    return maxSize;
  }

  /**
   * The minimum number of connections on the pool. If the pool has to flush connections it may
   * create connections to keep this amount, default is 2.
   */
  public int getMinSize() {
    return minSize;
  }

  /**
   * The credentials to use in order to authenticate to the database. Default is to don't provide
   * any credentials.
   */
  public String getPassword() {
    return password;
  }

  /**
   * Connections idle for longer than this time period are flushed from the pool. A duration of
   * {@link Duration#ZERO} means that this feature is disabled. Default is 16 seconds.
   */
  public Duration getReapTimeout() {
    return reapTimeout;
  }

  /**
   * The principal to be authenticated in the database. Default is to don't perform authentication.
   */
  public String getUsername() {
    return username;
  }

  /**
   * Sets the duration of background validation interval. Default is {@link Duration#ZERO} meaning
   * that this feature is disabled. Default is 3 minutes.
   */
  public Duration getValidationTimeout() {
    return validationTimeout;
  }

  /**
   * If connections should have the auto-commit mode on by default. The transaction integration may
   * disable auto-commit when a connection in enrolled in a transaction. Default is true.
   */
  public boolean isAutoCommit() {
    return autoCommit;
  }

  /**
   * Whether this data source bind to JNDI, default is false.
   */
  public boolean isBindToJndi() {
    return bindToJndi;
  }

  public boolean isEnable() {
    return enable;
  }

  public boolean isEnableCustomTransactionIntegration() {
    return enableCustomTransactionIntegration;
  }

  /**
   * Enable collects metrics, default is false.
   */
  public boolean isEnableMetrics() {
    return enableMetrics;
  }

  /**
   * If JDBC resources ({@link java.sql.Statement} and {@link java.sql.ResultSet}) should be tracked
   * to be closed if leaked, default is true.
   */
  public boolean isEnableTrackJdbcResources() {
    return enableTrackJdbcResources;
  }

  /**
   * Since the mainstream JTA implementations support Resource Local, if the main purpose of the
   * data source corresponding to the configuration is written operation, it is necessary to set
   * this attribute to true to meet the write operation transactional. Default is true.
   *
   * <p>
   * In general, for Resource Local transaction management, the mainstream JTA coordinator approach
   * is to share a disguised(Wrapped) XAResource in a thread, the XAResource contain a Resource
   * Local related database connection. Set the auto commit of Connection to false
   * {@code Connection.setAutoCommit(true)} in the {@code XAResource.start(xid, int)} method,
   * perform {@code Connection.commit()} directly in the first stage of XA submission.
   *
   * <p>
   * NOTE: In general, the judgment of whether the transaction is Resource Local or full JTA depends
   * on whether the object of the {@link #getDriver()} returns implements the XADataSource
   * interface.
   */
  public boolean isJta() {
    return jta;
  }

  /**
   * Whether enable this data source configuration.
   */
  @Override
  public boolean isValid() {
    return driver != null && enable && (isNotBlank(connectionUrl) || isNotEmpty(jdbcProperties));
  }

  /**
   * Whether to verify the data source instance after deployment, this may try to build
   * {@link #getMinSize()} connections.
   */
  public boolean isVerifyDeployment() {
    return verifyDeployment;
  }

  /**
   * Indicates that this data source configuration for the data source supports the XA protocol, so
   * the corresponding {@link #getDriver()} should implement {@code XADataSource}. If this
   * configuration returns true and the JTA coordinator supports Recovery operations, the data
   * source corresponding to this configuration will be added to recovery process. Default is true.
   *
   * @see JDBCTransactionIntegration
   */
  public boolean isXa() {
    return xa;
  }

  @Override
  public void onPostConstruct(Config config, String key) {
    setName(key);
    if (xa && !jta) {
      jta = true;
      logger.warning(() -> String.format(
          "The XA attribute of the data source [%s] is True, and the JTA attribute will be forced from the False originally configured to True!",
          getName()));
    }
  }
}
