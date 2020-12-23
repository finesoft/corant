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
package org.corant.suites.webserver.shared;

import static org.corant.shared.normal.Defaults.DFLT_CHARSET_STR;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.split;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-suites-webserver-shared
 *
 * @author bingo 下午7:58:27
 *
 */
@ApplicationScoped
public class WebServerConfig {

  @Inject
  @ConfigProperty(name = "webserver.default-charset")
  private Optional<String> defaultCharset;

  @Inject
  @ConfigProperty(name = "webserver.display-name")
  private Optional<String> displayName;

  @Inject
  @ConfigProperty(name = "webserver.host", defaultValue = "0.0.0.0")
  private String host;

  @Inject
  @ConfigProperty(name = "webserver.port", defaultValue = "8080")
  private Integer port;

  @Inject
  @ConfigProperty(name = "webserver.context-path", defaultValue = "/")
  private String contextPath;

  @Inject
  @ConfigProperty(name = "webserver.locale-charset-mappings")
  private Optional<String> localeCharsetMappings;

  @Inject
  @ConfigProperty(name = "webserver.secured.port")
  private Optional<Integer> securedPort;

  @Inject
  @ConfigProperty(name = "webserver.keystore.path")
  private Optional<String> keystorePath;

  @Inject
  @ConfigProperty(name = "webserver.keystore.type")
  private Optional<String> keystoreType;

  @Inject
  @ConfigProperty(name = "webserver.keystore.password")
  private Optional<String> keystorePassword;

  @Inject
  @ConfigProperty(name = "webserver.truststore.path")
  private Optional<String> truststorePath;

  @Inject
  @ConfigProperty(name = "webserver.truststore.type")
  private Optional<String> truststoreType;

  @Inject
  @ConfigProperty(name = "webserver.truststore.password")
  private Optional<String> truststorePassword;

  @Inject
  @ConfigProperty(name = "webserver.work-threads", defaultValue = "128")
  private Integer workThreads;

  @Inject
  @ConfigProperty(name = "webserver.session-timeout", defaultValue = "30")
  private Integer sessionTimeout;

  public String getContextPath() {
    return contextPath;
  }

  public Optional<String> getDefaultCharset() {
    return defaultCharset;
  }

  public String getDescription() {
    StringBuilder sb = new StringBuilder();
    sb.append("host:").append(getHost()).append(",");
    sb.append(" port:").append(getPort()).append(",");
    sb.append(" work-threads:").append(getWorkThreads());
    getSecuredPort().ifPresent(s -> sb.append(",").append("secured port:").append(s));
    getKeystorePath().ifPresent(s -> sb.append(",").append(" keystore path:").append(s));
    getKeystoreType().ifPresent(s -> sb.append(",").append(" keystore type:").append(s));
    getTruststorePath().ifPresent(s -> sb.append(",").append(" truststore path:").append(s));
    getTruststoreType().ifPresent(s -> sb.append(",").append(" truststore type:").append(s));
    return sb.toString();
  }

  public Optional<String> getDisplayName() {
    return displayName;
  }

  /**
   *
   * @return the host
   */
  public String getHost() {
    return host;
  }

  /**
   *
   * @return the keystorePassword
   */
  public Optional<String> getKeystorePassword() {
    return keystorePassword;
  }

  /**
   *
   * @return the keystorePath
   */
  public Optional<String> getKeystorePath() {
    return keystorePath;
  }

  /**
   *
   * @return the keystoreType
   */
  public Optional<String> getKeystoreType() {
    return keystoreType;
  }

  public Map<String, String> getLocaleCharsetMap() {
    Map<String, String> map = new HashMap<>();
    if (getLocaleCharsetMappings().isPresent()) {
      streamOf(split(getLocaleCharsetMappings().get(), ",")).forEach(lcm -> {
        String[] lc = split(lcm, ":", true, true);
        if (lc.length == 2) {
          map.put(lc[0], lc[1]);
        }
      });
    } else {
      for (Locale locale : Locale.getAvailableLocales()) {
        map.put(locale.toString(), DFLT_CHARSET_STR);
      }
    }
    return map;
  }

  public Optional<String> getLocaleCharsetMappings() {
    return localeCharsetMappings;
  }

  /**
   *
   * @return the port
   */
  public Integer getPort() {
    return port;
  }

  /**
   *
   * @return the securedPort
   */
  public Optional<Integer> getSecuredPort() {
    return securedPort;
  }

  public Integer getSessionTimeout() {
    return sessionTimeout;
  }

  /**
   *
   * @return the truststorePassword
   */
  public Optional<String> getTruststorePassword() {
    return truststorePassword;
  }

  /**
   *
   * @return the truststorePath
   */
  public Optional<String> getTruststorePath() {
    return truststorePath;
  }

  /**
   *
   * @return the truststoreType
   */
  public Optional<String> getTruststoreType() {
    return truststoreType;
  }

  /**
   *
   * @return the workThreads
   */
  public Integer getWorkThreads() {
    return workThreads;
  }

  public boolean isSecured() {
    return getSecuredPort().isPresent() && getKeystorePassword().isPresent()
        && getKeystorePath().isPresent() && getKeystoreType().isPresent();
  }
}
