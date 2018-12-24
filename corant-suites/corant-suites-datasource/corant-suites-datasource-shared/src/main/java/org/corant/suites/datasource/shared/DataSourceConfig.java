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

import static org.corant.shared.util.ClassUtils.tryAsClass;
import static org.corant.shared.util.StreamUtils.asStream;
import static org.corant.shared.util.StringUtils.defaultString;
import static org.corant.shared.util.StringUtils.group;
import static org.corant.shared.util.StringUtils.isNoneBlank;
import static org.corant.shared.util.StringUtils.isNotBlank;
import static org.corant.shared.util.StringUtils.splitNotBlank;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.Config;

/**
 * corant-suites-datasource-shared
 *
 * @author bingo 下午3:45:45
 *
 */
public class DataSourceConfig {

  public static final String PREFIX = "datasource.";

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
    Map<String, List<String>> cfgNmes = getConfigNames(config);
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
      if (pn.endsWith("driver")) {
        config.getOptionalValue(pn, String.class).ifPresent(s -> cfg.driver = tryAsClass(s));
      } else if (pn.endsWith(".username")) {
        config.getOptionalValue(pn, String.class).ifPresent(s -> cfg.username = s);
      } else if (pn.endsWith(".password")) {
        config.getOptionalValue(pn, String.class).ifPresent(s -> cfg.password = s);
      } else if (pn.endsWith(".connection-url")) {
        config.getOptionalValue(pn, String.class).ifPresent(s -> cfg.connectionUrl = s);
      } else if (pn.endsWith(".connectable")) {
        config.getOptionalValue(pn, Boolean.class).ifPresent(s -> cfg.connectable = s);
      } else if (pn.endsWith(".jta")) {
        config.getOptionalValue(pn, Boolean.class).ifPresent(s -> cfg.jta = s);
      } else if (pn.endsWith(".xa")) {
        config.getOptionalValue(pn, Boolean.class).ifPresent(s -> cfg.xa = s);
      } else if (pn.endsWith(".initial-size")) {
        config.getOptionalValue(pn, Integer.class).ifPresent(s -> cfg.initialSize = s);
      } else if (pn.endsWith(".min-size")) {
        config.getOptionalValue(pn, Integer.class).ifPresent(s -> cfg.minSize = s);
      } else if (pn.endsWith(".max-size")) {
        config.getOptionalValue(pn, Integer.class).ifPresent(s -> cfg.maxSize = s);
      } else if (pn.endsWith(".leak-timeout")) {
        config.getOptionalValue(pn, String.class).ifPresent(s -> cfg.leakTimeout = convert(s));
      } else if (pn.endsWith(".validation-timeout")) {
        config.getOptionalValue(pn, String.class)
            .ifPresent(s -> cfg.validationTimeout = convert(s));
      } else if (pn.endsWith(".reap-timeout")) {
        config.getOptionalValue(pn, String.class).ifPresent(s -> cfg.reapTimeout = convert(s));
      } else if (pn.endsWith(".acquisition-timeout")) {
        config.getOptionalValue(pn, String.class)
            .ifPresent(s -> cfg.acquisitionTimeout = convert(s));
      } else if (pn.endsWith(".validate-connection")) {
        config.getOptionalValue(pn, Boolean.class).ifPresent(s -> cfg.validateConnection = s);
      }
    });
    if (isNoneBlank(cfg.name, cfg.connectionUrl)) {
      return cfg;
    }
    return null;
  }

  private static Map<String, List<String>> getConfigNames(Config config) {
    return group(config.getPropertyNames(), (s) -> defaultString(s).startsWith(PREFIX), (s) -> {
      String[] arr = splitNotBlank(s, ".", true);
      if (arr.length > 2) {
        return new String[] {arr[1], s};
      }
      return new String[0];
    });
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


}
