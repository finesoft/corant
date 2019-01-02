/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.suites.datasource.shared;

import static org.corant.shared.util.ClassUtils.tryAsClass;
import static org.corant.shared.util.StreamUtils.asStream;
import static org.corant.shared.util.StringUtils.isNoneBlank;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.corant.kernel.util.ConfigUtils;
import org.eclipse.microprofile.config.Config;

/**
 * corant-suites-datasource-shared
 *
 * <pre>
 * Config key like "datasource.?.driver=org.mysql.driver", the '?' is the datasource name.
 * </pre>
 *
 * @author bingo 下午3:45:45
 *
 */
public class DataSourceConfig {

  public static final String PREFIX = "datasource.";
  public static final String DS_VALIDATE_CONNECTION = ".validate-connection";
  public static final String DS_ACQUISITION_TIMEOUT = ".acquisition-timeout";
  public static final String DS_REAP_TIMEOUT = ".reap-timeout";
  public static final String DS_VALIDATION_TIMEOUT = ".validation-timeout";
  public static final String DS_LEAK_TIMEOUT = ".leak-timeout";
  public static final String DS_MAX_SIZE = ".max-size";
  public static final String DS_MIN_SIZE = ".min-size";
  public static final String DS_INITIAL_SIZE = ".initial-size";
  public static final String DS_XA = ".xa";
  public static final String DS_JTA = ".jta";
  public static final String DS_CONNECTABLE = ".connectable";
  public static final String DS_CONNECTION_URL = ".connection-url";
  public static final String DS_PASSWORD = ".password";
  public static final String DS_USER_NAME = ".username";
  public static final String DS_DRIVER = ".driver";


  private Class<?> driver;
  private String name;
  private String username;
  private String password;
  private String connectionUrl;
  private boolean jta = true;
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

  public static Duration convert(String value) {
    return value != null ? Duration.parse(value) : null;
  }

  public static Map<String, DataSourceConfig> from(Config config) {
    Map<String, DataSourceConfig> map = new LinkedHashMap<>();
    Map<String, List<String>> cfgNmes = ConfigUtils.getGroupConfigNames(config, PREFIX, 1);
    cfgNmes.forEach((k, v) -> {
      final DataSourceConfig cfg = of(config, k, v);
      if (isNoneBlank(cfg.name, cfg.connectionUrl)) {
        map.put(k, cfg);
      }
    });
    return map;
  }

  public static DataSourceConfig of(Config config, String name) {
    String prefix = PREFIX + name;
    Set<String> pns = asStream(config.getPropertyNames())
        .filter(pn -> isNotBlank(pn) && pn.startsWith(prefix)).collect(Collectors.toSet());
    return of(config, name, pns);
  }

  public static DataSourceConfig of(Config config, String name, Collection<String> propertieNames) {
    final DataSourceConfig cfg = new DataSourceConfig();
    cfg.name = name;
    propertieNames.forEach(pn -> {
      if (pn.endsWith(DS_DRIVER)) {
        config.getOptionalValue(pn, String.class).ifPresent(s -> cfg.setDriver(tryAsClass(s)));
      } else if (pn.endsWith(DS_USER_NAME)) {
        config.getOptionalValue(pn, String.class).ifPresent(cfg::setUsername);
      } else if (pn.endsWith(DS_PASSWORD)) {
        config.getOptionalValue(pn, String.class).ifPresent(cfg::setPassword);
      } else if (pn.endsWith(DS_CONNECTION_URL)) {
        config.getOptionalValue(pn, String.class).ifPresent(cfg::setConnectionUrl);
      } else if (pn.endsWith(DS_CONNECTABLE)) {
        config.getOptionalValue(pn, Boolean.class).ifPresent(cfg::setConnectable);
      } else if (pn.endsWith(DS_JTA)) {
        config.getOptionalValue(pn, Boolean.class).ifPresent(cfg::setJta);
      } else if (pn.endsWith(DS_XA)) {
        config.getOptionalValue(pn, Boolean.class).ifPresent(cfg::setXa);
      } else if (pn.endsWith(DS_INITIAL_SIZE)) {
        config.getOptionalValue(pn, Integer.class).ifPresent(cfg::setInitialSize);
      } else if (pn.endsWith(DS_MIN_SIZE)) {
        config.getOptionalValue(pn, Integer.class).ifPresent(cfg::setMinSize);
      } else if (pn.endsWith(DS_MAX_SIZE)) {
        config.getOptionalValue(pn, Integer.class).ifPresent(cfg::setMaxSize);
      } else if (pn.endsWith(DS_LEAK_TIMEOUT)) {
        config.getOptionalValue(pn, String.class).ifPresent(s -> cfg.setLeakTimeout(convert(s)));
      } else if (pn.endsWith(DS_VALIDATION_TIMEOUT)) {
        config.getOptionalValue(pn, String.class)
            .ifPresent(s -> cfg.setValidationTimeout(convert(s)));
      } else if (pn.endsWith(DS_REAP_TIMEOUT)) {
        config.getOptionalValue(pn, String.class).ifPresent(s -> cfg.setReapTimeout(convert(s)));
      } else if (pn.endsWith(DS_ACQUISITION_TIMEOUT)) {
        config.getOptionalValue(pn, String.class)
            .ifPresent(s -> cfg.setAcquisitionTimeout(convert(s)));
      } else if (pn.endsWith(DS_VALIDATE_CONNECTION)) {
        config.getOptionalValue(pn, Boolean.class).ifPresent(cfg::setValidateConnection);
      }
    });
    if (isNoneBlank(cfg.name, cfg.connectionUrl)) {
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
   * @return the connectable
   */
  public boolean isConnectable() {
    return connectable;
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

  protected void setConnectable(boolean connectable) {
    this.connectable = connectable;
  }

  protected void setConnectionUrl(String connectionUrl) {
    this.connectionUrl = connectionUrl;
  }

  protected void setDriver(Class<?> driver) {
    this.driver = driver;
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
    this.name = name;
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
