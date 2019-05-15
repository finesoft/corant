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

import static org.corant.config.Configurations.getGroupConfigNames;
import static org.corant.shared.util.Assertions.shouldBeNull;
import static org.corant.shared.util.ClassUtils.tryAsClass;
import static org.corant.shared.util.StreamUtils.streamOf;
import static org.corant.shared.util.StringUtils.defaultString;
import static org.corant.shared.util.StringUtils.defaultTrim;
import static org.corant.shared.util.StringUtils.isNotBlank;
import static org.corant.shared.util.StringUtils.trim;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.corant.kernel.util.Unnamed;
import org.corant.shared.normal.Names.JndiNames;
import org.corant.shared.util.StringUtils;
import org.eclipse.microprofile.config.Config;

/**
 * corant-suites-datasource-shared
 *
 * @see DataSourceConfig#from(Config)
 * @see Unnamed
 * @author bingo 下午3:45:45
 *
 */
public class DataSourceConfig {

  public static final String JNDI_SUBCTX_NAME = JndiNames.JNDI_COMP_NME + "/Datasources";
  public static final String EMPTY_NAME = StringUtils.EMPTY;

  public static final String DSC_PREFIX = "datasource.";
  public static final String DSC_CONNECTION_URL = ".connection-url";
  public static final String DSC_DRIVER = ".driver";
  public static final String DSC_PASSWORD = ".password";
  public static final String DSC_USER_NAME = ".username";
  public static final String DSC_CONNECTABLE = ".connectable";
  public static final String DSC_VALIDATE_CONNECTION = ".validate-connection";
  public static final String DSC_ACQUISITION_TIMEOUT = ".acquisition-timeout";
  public static final String DSC_REAP_TIMEOUT = ".reap-timeout";
  public static final String DSC_VALIDATION_TIMEOUT = ".validation-timeout";
  public static final String DSC_LEAK_TIMEOUT = ".leak-timeout";
  public static final String DSC_MAX_SIZE = ".max-size";
  public static final String DSC_MIN_SIZE = ".min-size";
  public static final String DSC_INITIAL_SIZE = ".initial-size";
  public static final String DSC_XA = ".xa";
  public static final String DSC_JTA = ".jta";
  public static final String DSC_METRICS = ".enable-metrics";
  public static final String DSC_NAME = ".name";
  public static final String DSC_AUTO_COMMIT = ".auto-commit";

  private Class<?> driver;
  private String name;
  private String username;
  private String password;
  private String connectionUrl;
  private boolean jta = true;
  private boolean autoCommit = true;
  private boolean connectable;
  private boolean xa = false;

  private int initialSize = 2;
  private volatile int minSize = 0;
  private int maxSize = 16;
  private Duration leakTimeout = Duration.ZERO;
  private Duration validationTimeout = Duration.ZERO;
  private Duration reapTimeout = Duration.ZERO;
  private volatile Duration acquisitionTimeout = Duration.ZERO;
  private boolean validateConnection = false;
  private boolean enableMetrics = false;

  /**
   * @param name
   * @param driver
   * @param username
   * @param password
   * @param connectionUrl
   * @param jta
   * @param connectable
   * @param autoCommit
   * @param xa
   * @param initialSize
   * @param minSize
   * @param maxSize
   * @param leakTimeout
   * @param validationTimeout
   * @param reapTimeout
   * @param acquisitionTimeout
   * @param validateConnection
   * @param enableMetrics
   */
  public DataSourceConfig(String name, Class<?> driver, String username, String password,
      String connectionUrl, boolean jta, boolean connectable, boolean autoCommit, boolean xa,
      int initialSize, int minSize, int maxSize, Duration leakTimeout, Duration validationTimeout,
      Duration reapTimeout, Duration acquisitionTimeout, boolean validateConnection,
      boolean enableMetrics) {
    super();
    setName(name);
    setDriver(driver);
    setUsername(username);
    setPassword(password);
    setConnectionUrl(connectionUrl);
    setJta(jta);
    setConnectable(connectable);
    setAutoCommit(autoCommit);
    setXa(xa);
    setInitialSize(initialSize);
    setMinSize(minSize);
    setMaxSize(maxSize);
    setLeakTimeout(leakTimeout);
    setValidationTimeout(validationTimeout);
    setReapTimeout(reapTimeout);
    setAcquisitionTimeout(acquisitionTimeout);
    setValidateConnection(validateConnection);
    setEnableMetrics(enableMetrics);
  }

  protected DataSourceConfig() {
    super();
  }

  /**
   * Get data source configurations from application configurations.
   *
   * If do not find any data source name in application configurations, then the data source name is
   * {@link #EMPTY_NAME}.
   *
   * <pre>
   * 1. If the application configurations has only one data source configurations such like
   *
   * datasource.driver = ?
   * datasource.connection-url = ?
   * datasource.xxx = ?
   * ....
   *
   * then will return a map with one key and one value. If don't find 'datasource.name' property or
   * the value of 'datasource.name' property is blank, the data source name is {@link #EMPTY_NAME} .
   * </pre>
   *
   * <pre>
   * 2. If the application configurations has multiple data source configurations such like
   *
   * datasource.[name].driver = ?
   * datasource.[name].connection-url = ?
   * datasource.[name].xxx = ?
   * ....
   *
   * then will return a map, the key is data source name.
   * </pre>
   *
   * <pre>
   * 3. If the application configurations has multiple data source configurations that
   * mixed the first and second points mentioned above, such like
   *
   * datasource.[name].driver = ?
   * datasource.[name].connection-url = ?
   * datasource.[name].xxx = ?
   * ....
   *
   * datasource.driver = ?
   * datasource.connection-url = ?
   * datasource.xxx = ?
   * ....
   *
   * then will return a map.
   * </pre>
   *
   * @param config
   * @return from
   */
  public static Map<String, DataSourceConfig> from(Config config) {
    Map<String, DataSourceConfig> dataSources = new LinkedHashMap<>();
    Set<String> dfltCfgKeys = defaultPropertyNames();
    // handle named data source configuration
    Map<String, List<String>> namedCfgKeys = getGroupConfigNames(config,
        s -> defaultString(s).startsWith(DSC_PREFIX) && !dfltCfgKeys.contains(s), 1);
    namedCfgKeys.forEach((k, v) -> {
      final DataSourceConfig cfg = of(config, k, v);
      if (cfg != null) {
        shouldBeNull(dataSources.put(defaultTrim(cfg.getName()), cfg),
            "The data source named %s configuration dup!", cfg.getName());
      }
    });
    // handle default configuration
    String dfltName =
        config.getOptionalValue(DSC_PREFIX + DSC_NAME.substring(1), String.class).orElse(null);
    DataSourceConfig dfltCfg = of(config, dfltName, dfltCfgKeys);
    if (dfltCfg != null) {
      shouldBeNull(dataSources.put(defaultTrim(dfltCfg.getName()), dfltCfg),
          "The data source named %s configuration dup!", dfltName);
    }
    return dataSources;
  }

  public static DataSourceConfig from(Config config, String name) {
    String prefix = DSC_PREFIX + name;
    Set<String> pns = streamOf(config.getPropertyNames())
        .filter(pn -> isNotBlank(pn) && pn.startsWith(prefix)).collect(Collectors.toSet());
    return of(config, name, pns);
  }

  static Duration convert(String value) {
    return value != null ? Duration.parse(value) : null;
  }

  static Set<String> defaultPropertyNames() {
    String dfltPrefix = DSC_PREFIX.substring(0, DSC_PREFIX.length() - 1);
    Set<String> names = new LinkedHashSet<>();
    names.add(dfltPrefix + DSC_ACQUISITION_TIMEOUT);
    names.add(dfltPrefix + DSC_CONNECTABLE);
    names.add(dfltPrefix + DSC_CONNECTION_URL);
    names.add(dfltPrefix + DSC_DRIVER);
    names.add(dfltPrefix + DSC_INITIAL_SIZE);
    names.add(dfltPrefix + DSC_JTA);
    names.add(dfltPrefix + DSC_LEAK_TIMEOUT);
    names.add(dfltPrefix + DSC_MAX_SIZE);
    names.add(dfltPrefix + DSC_METRICS);
    names.add(dfltPrefix + DSC_MIN_SIZE);
    names.add(dfltPrefix + DSC_PASSWORD);
    names.add(dfltPrefix + DSC_REAP_TIMEOUT);
    names.add(dfltPrefix + DSC_USER_NAME);
    names.add(dfltPrefix + DSC_VALIDATE_CONNECTION);
    names.add(dfltPrefix + DSC_VALIDATION_TIMEOUT);
    names.add(dfltPrefix + DSC_XA);
    names.add(dfltPrefix + DSC_NAME);
    names.add(dfltPrefix + DSC_AUTO_COMMIT);
    return names;
  }

  static DataSourceConfig of(Config config, String name, Collection<String> propertieNames) {
    final DataSourceConfig cfg = new DataSourceConfig();
    cfg.setName(name);
    propertieNames.forEach(pn -> {
      if (pn.endsWith(DSC_DRIVER)) {
        config.getOptionalValue(pn, String.class).ifPresent(s -> cfg.setDriver(tryAsClass(s)));
      } else if (pn.endsWith(DSC_USER_NAME)) {
        config.getOptionalValue(pn, String.class).ifPresent(cfg::setUsername);
      } else if (pn.endsWith(DSC_PASSWORD)) {
        config.getOptionalValue(pn, String.class).ifPresent(cfg::setPassword);
      } else if (pn.endsWith(DSC_CONNECTION_URL)) {
        config.getOptionalValue(pn, String.class).ifPresent(cfg::setConnectionUrl);
      } else if (pn.endsWith(DSC_CONNECTABLE)) {
        config.getOptionalValue(pn, Boolean.class).ifPresent(cfg::setConnectable);
      } else if (pn.endsWith(DSC_AUTO_COMMIT)) {
        config.getOptionalValue(pn, Boolean.class).ifPresent(cfg::setAutoCommit);
      } else if (pn.endsWith(DSC_JTA)) {
        config.getOptionalValue(pn, Boolean.class).ifPresent(cfg::setJta);
      } else if (pn.endsWith(DSC_XA)) {
        config.getOptionalValue(pn, Boolean.class).ifPresent(cfg::setXa);
      } else if (pn.endsWith(DSC_INITIAL_SIZE)) {
        config.getOptionalValue(pn, Integer.class).ifPresent(cfg::setInitialSize);
      } else if (pn.endsWith(DSC_MIN_SIZE)) {
        config.getOptionalValue(pn, Integer.class).ifPresent(cfg::setMinSize);
      } else if (pn.endsWith(DSC_MAX_SIZE)) {
        config.getOptionalValue(pn, Integer.class).ifPresent(cfg::setMaxSize);
      } else if (pn.endsWith(DSC_LEAK_TIMEOUT)) {
        config.getOptionalValue(pn, String.class).ifPresent(s -> cfg.setLeakTimeout(convert(s)));
      } else if (pn.endsWith(DSC_VALIDATION_TIMEOUT)) {
        config.getOptionalValue(pn, String.class)
            .ifPresent(s -> cfg.setValidationTimeout(convert(s)));
      } else if (pn.endsWith(DSC_REAP_TIMEOUT)) {
        config.getOptionalValue(pn, String.class).ifPresent(s -> cfg.setReapTimeout(convert(s)));
      } else if (pn.endsWith(DSC_ACQUISITION_TIMEOUT)) {
        config.getOptionalValue(pn, String.class)
            .ifPresent(s -> cfg.setAcquisitionTimeout(convert(s)));
      } else if (pn.endsWith(DSC_VALIDATE_CONNECTION)) {
        config.getOptionalValue(pn, Boolean.class).ifPresent(cfg::setValidateConnection);
      } else if (pn.endsWith(DSC_METRICS)) {
        config.getOptionalValue(pn, Boolean.class).ifPresent(cfg::setEnableMetrics);
      }
    });
    if (isNotBlank(cfg.getConnectionUrl())) {
      return cfg;
    }
    return null;
  }

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
   * @return the initialSize
   */
  public int getInitialSize() {
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
  public int getMinSize() {
    return minSize;
  }

  /**
   *
   * @return the name
   */
  public String getName() {
    return name;
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
   * @return the connectable
   */
  public boolean isConnectable() {
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
  public boolean isJta() {
    return jta;
  }

  /**
   *
   * @return the validateConnection
   */
  public boolean isValidateConnection() {
    return validateConnection;
  }

  /**
   *
   * @return the xa
   */
  public boolean isXa() {
    return xa;
  }

  protected void setAcquisitionTimeout(Duration acquisitionTimeout) {
    this.acquisitionTimeout = acquisitionTimeout;
  }

  /**
   *
   * @param autoCommit the autoCommit to set
   */
  protected void setAutoCommit(boolean autoCommit) {
    this.autoCommit = autoCommit;
  }

  protected void setConnectable(boolean connectable) {
    this.connectable = connectable;
  }

  protected void setConnectionUrl(String connectionUrl) {
    this.connectionUrl = connectionUrl;
  }

  protected void setDriver(Class<?> driver) {
    this.driver = driver;
  }

  protected void setEnableMetrics(boolean enableMetrics) {
    this.enableMetrics = enableMetrics;
  }

  protected void setInitialSize(int initialSize) {
    this.initialSize = initialSize;
  }

  protected void setJta(boolean jta) {
    this.jta = jta;
  }

  protected void setLeakTimeout(Duration leakTimeout) {
    this.leakTimeout = leakTimeout;
  }

  protected void setMaxSize(int maxSize) {
    this.maxSize = maxSize;
  }

  protected void setMinSize(int minSize) {
    this.minSize = minSize;
  }

  protected void setName(String name) {
    this.name = trim(defaultString(name));
  }

  protected void setPassword(String password) {
    this.password = password;
  }

  protected void setReapTimeout(Duration reapTimeout) {
    this.reapTimeout = reapTimeout;
  }

  protected void setUsername(String username) {
    this.username = username;
  }

  protected void setValidateConnection(boolean validateConnection) {
    this.validateConnection = validateConnection;
  }

  protected void setValidationTimeout(Duration validationTimeout) {
    this.validationTimeout = validationTimeout;
  }

  protected void setXa(boolean xa) {
    this.xa = xa;
  }

}
