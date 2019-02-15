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
package org.corant.suites.mongodb;

import static org.corant.shared.util.CollectionUtils.asList;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.MapUtils.getOptMapObject;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.shared.util.StringUtils.defaultString;
import static org.corant.shared.util.StringUtils.group;
import static org.corant.shared.util.StringUtils.isNoneBlank;
import static org.corant.shared.util.StringUtils.split;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.corant.kernel.util.ConfigUtils;
import org.corant.shared.util.ConversionUtils;
import org.corant.shared.util.ObjectUtils.Pair;
import org.eclipse.microprofile.config.Config;
import com.mongodb.MongoClientOptions.Builder;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;

/**
 * corant-suites-mongodb
 *
 * @author bingo 下午12:10:04
 *
 */
public class MongoClientConfig {

  public static final String PREFIX = "mongodb.";
  public static final int DEFAULT_PORT = 27017;
  public static final String DEFAULT_HOST = "localhost";
  public static final String DEFAULT_URI = "mongodb://localhost/test";

  public static final String MC_APP_NAME = ".applicationName";
  public static final String MC_HOST_PORTS = ".host-ports";
  public static final String MC_URI = ".uri";
  public static final String MC_AUTH_DB = ".auth-database";
  public static final String MC_USER_NAME = ".user-name";
  public static final String MC_PASSWORD = ".password";
  public static final String MC_OPTS = ".option";
  public static final String MC_DATABASE = ".database";
  public static final String MC_CLIENT = ".client";

  private List<Pair<String, Integer>> hostAndPorts = new ArrayList<>();

  private String applicationName;

  private String client;

  private String uri;

  private String database;

  private String authenticationDatabase;

  private String username;

  private char[] password = new char[0];

  private Map<String, String> options = new HashMap<>();

  public static Map<String, MongoClientConfig> from(Config config) {
    Map<String, MongoClientConfig> map = new HashMap<>();
    Map<String, List<String>> cfgNmes = getGroupConfigNames(config);
    cfgNmes.forEach((k, v) -> {
      String client = k.split(ConfigUtils.SEPARATOR)[0];
      ConfigUtils.getGroupConfigNames(v, PREFIX + ConfigUtils.SEPARATOR + client, 2)
          .forEach((k1, v1) -> {
            final MongoClientConfig cfg = of(config, client, k1, v1);
            if (isNoneBlank(cfg.uri)) {
              map.put(k, cfg);
            }
          });
    });
    return map;
  }

  public static MongoClientConfig of(Config config, String client, String database,
      Collection<String> propertieNames) {
    final MongoClientConfig mc = new MongoClientConfig();
    final String opPrefix = PREFIX + database + MC_OPTS;
    final int opPrefixLen = opPrefix.length();
    Set<String> opCfgNmes = new HashSet<>();
    mc.setDatabase(database);
    propertieNames.forEach(pn -> {
      if (pn.endsWith(MC_AUTH_DB)) {
        config.getOptionalValue(pn, String.class).ifPresent(mc::setAuthenticationDatabase);
      } else if (pn.endsWith(MC_CLIENT)) {
        config.getOptionalValue(pn, String.class).ifPresent(mc::setClient);
      } else if (pn.endsWith(MC_APP_NAME)) {
        config.getOptionalValue(pn, String.class).ifPresent(mc::setApplicationName);
      } else if (pn.endsWith(MC_HOST_PORTS)) {
        config.getOptionalValue(pn, String.class).ifPresent(hps -> {
          String[] hpArr = split(hps, ";", true, true);
          Set<Pair<String, Integer>> set = new LinkedHashSet<>(hpArr.length);
          for (String hp : hpArr) {
            String[] arr = split(hp, ":", true, true);
            if (arr.length > 1) {
              set.add(Pair.of(arr[0], Integer.valueOf(arr[1])));
            } else if (arr.length == 1) {
              set.add(Pair.of(arr[0], DEFAULT_PORT));
            }
          }
          set.forEach(hp -> mc.getHostAndPorts().add(hp));
        });
      } else if (pn.endsWith(MC_PASSWORD)) {
        config.getOptionalValue(pn, String.class).ifPresent(mc::setPassword);
      } else if (pn.endsWith(MC_URI)) {
        config.getOptionalValue(pn, String.class).ifPresent(mc::setUri);
      } else if (pn.endsWith(MC_USER_NAME)) {
        config.getOptionalValue(pn, String.class).ifPresent(mc::setUsername);
      } else if (pn.startsWith(opPrefix) && pn.length() > opPrefixLen) {
        // handle options
        opCfgNmes.add(pn);
      }
    });
    if (!isEmpty(opCfgNmes)) {
      int len = opPrefix.length() + 1;
      for (String opCfgNme : opCfgNmes) {
        config.getOptionalValue(opCfgNme, String.class).ifPresent(s -> {
          String opName = opCfgNme.substring(len);
          mc.options.put(opName, s);
        });
      }
    }
    return mc;
  }

  static Map<String, List<String>> getGroupConfigNames(Config config) {
    return group(config.getPropertyNames(), (s) -> defaultString(s).startsWith(PREFIX), (s) -> {
      String[] arr = split(s, ConfigUtils.SEPARATOR, true, true);
      if (arr.length > 2) {
        return new String[] {String.join(ConfigUtils.SEPARATOR, arr[1], arr[2]), s};
      }
      return new String[0];
    });
  }

  /**
   *
   * @return the applicationName
   */
  public String getApplicationName() {
    return applicationName;
  }

  /**
   *
   * @return the authenticationDatabase
   */
  public String getAuthenticationDatabase() {
    return authenticationDatabase;
  }

  /**
   *
   * @return the client
   */
  public String getClient() {
    return client;
  }

  /**
   *
   * @return the database
   */
  public String getDatabase() {
    if (database != null) {
      return database;
    }
    return new MongoClientURI(defaultObject(uri, DEFAULT_URI)).getDatabase();
  }

  /**
   *
   * @return the hostAndPorts
   */
  public List<Pair<String, Integer>> getHostAndPorts() {
    if (isEmpty(hostAndPorts)) {
      return Collections.unmodifiableList(asList(Pair.of(DEFAULT_HOST, DEFAULT_PORT)));
    }
    return Collections.unmodifiableList(hostAndPorts);
  }

  /**
   *
   * @return the options
   */
  public Map<String, String> getOptions() {
    return Collections.unmodifiableMap(options);
  }

  /**
   *
   * @return the password
   */
  public char[] getPassword() {
    return Arrays.copyOf(password, password.length);
  }

  /**
   *
   * @return the uri
   */
  public String getUri() {
    return uri;
  }

  /**
   *
   * @return the username
   */
  public String getUsername() {
    return username;
  }

  public Builder produceBuiler() {
    Builder builder = new Builder();
    builder.applicationName(applicationName);
    getOptMapObject(options, "alwaysUseMBeans", ConversionUtils::toBoolean)
        .ifPresent(builder::alwaysUseMBeans);
    getOptMapObject(options, "description", ConversionUtils::toString)
        .ifPresent(builder::description);
    getOptMapObject(options, "connectionsPerHost", ConversionUtils::toInteger)
        .ifPresent(builder::connectionsPerHost);
    getOptMapObject(options, "connectTimeout", ConversionUtils::toInteger)
        .ifPresent(builder::connectTimeout);
    getOptMapObject(options, "cursorFinalizerEnabled", ConversionUtils::toBoolean)
        .ifPresent(builder::cursorFinalizerEnabled);
    getOptMapObject(options, "heartbeatConnectTimeout", ConversionUtils::toInteger)
        .ifPresent(builder::heartbeatConnectTimeout);
    getOptMapObject(options, "heartbeatFrequency", ConversionUtils::toInteger)
        .ifPresent(builder::heartbeatFrequency);
    getOptMapObject(options, "heartbeatSocketTimeout", ConversionUtils::toInteger)
        .ifPresent(builder::heartbeatSocketTimeout);
    getOptMapObject(options, "localThreshold", ConversionUtils::toInteger)
        .ifPresent(builder::localThreshold);
    getOptMapObject(options, "maxConnectionIdleTime", ConversionUtils::toInteger)
        .ifPresent(builder::maxConnectionIdleTime);
    getOptMapObject(options, "maxConnectionLifeTime", ConversionUtils::toInteger)
        .ifPresent(builder::maxConnectionLifeTime);
    getOptMapObject(options, "maxWaitTime", ConversionUtils::toInteger)
        .ifPresent(builder::maxWaitTime);
    getOptMapObject(options, "minConnectionsPerHost", ConversionUtils::toInteger)
        .ifPresent(builder::minConnectionsPerHost);
    getOptMapObject(options, "minHeartbeatFrequency", ConversionUtils::toInteger)
        .ifPresent(builder::minHeartbeatFrequency);
    getOptMapObject(options, "requiredReplicaSetName", ConversionUtils::toString)
        .ifPresent(builder::requiredReplicaSetName);
    getOptMapObject(options, "retryWrites", ConversionUtils::toBoolean)
        .ifPresent(builder::retryWrites);
    getOptMapObject(options, "serverSelectionTimeout", ConversionUtils::toInteger)
        .ifPresent(builder::serverSelectionTimeout);
    getOptMapObject(options, "socketTimeout", ConversionUtils::toInteger)
        .ifPresent(builder::socketTimeout);
    getOptMapObject(options, "sslEnabled", ConversionUtils::toBoolean)
        .ifPresent(builder::sslEnabled);
    getOptMapObject(options, "sslInvalidHostNameAllowed", ConversionUtils::toBoolean)
        .ifPresent(builder::sslInvalidHostNameAllowed);
    getOptMapObject(options, "threadsAllowedToBlockForConnectionMultiplier",
        ConversionUtils::toInteger)
            .ifPresent(builder::threadsAllowedToBlockForConnectionMultiplier);
    return builder;
  }

  public MongoCredential produceCredential() {
    String database =
        getAuthenticationDatabase() == null ? getDatabase() : getAuthenticationDatabase();
    return MongoCredential.createCredential(getUsername(), database, getPassword());
  }

  /**
   *
   * @param applicationName the applicationName to set
   */
  protected void setApplicationName(String applicationName) {
    this.applicationName = applicationName;
  }

  /**
   *
   * @param authenticationDatabase the authenticationDatabase to set
   */
  protected void setAuthenticationDatabase(String authenticationDatabase) {
    this.authenticationDatabase = authenticationDatabase;
  }

  /**
   *
   * @param client the client to set
   */
  protected void setClient(String client) {
    this.client = client;
  }

  /**
   *
   * @param database the database to set
   */
  protected void setDatabase(String database) {
    this.database = database;
  }

  /**
   *
   * @param hostAndPorts the hostAndPorts to set
   */
  protected void setHostAndPorts(List<Pair<String, Integer>> hostAndPorts) {
    this.hostAndPorts = hostAndPorts;
  }

  /**
   *
   * @param password the password to set
   */
  protected void setPassword(char[] password) {
    this.password = password;
  }

  protected void setPassword(String password) {
    this.password = isEmpty(password) ? new char[0] : password.toCharArray();
  }

  /**
   *
   * @param uri the uri to set
   */
  protected void setUri(String uri) {
    this.uri = uri;
  }

  /**
   *
   * @param username the username to set
   */
  protected void setUsername(String username) {
    this.username = username;
  }
}
