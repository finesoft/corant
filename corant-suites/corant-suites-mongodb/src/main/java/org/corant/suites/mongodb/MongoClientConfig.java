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

import static org.corant.shared.util.Assertions.shouldBeNull;
import static org.corant.shared.util.CollectionUtils.asList;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.MapUtils.getOptMapObject;
import static org.corant.shared.util.ObjectUtils.defaultObject;
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
import org.corant.shared.normal.Names;
import org.corant.shared.normal.Names.JndiNames;
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
  public static final String JNDI_SUBCTX_NAME = JndiNames.JNDI_COMP_NME + "/MongoClient";
  public static final int DEFAULT_PORT = 27017;
  public static final String PREFIX = "mongodb.";
  public static final String DEFAULT_HOST = "localhost";
  public static final String DEFAULT_URI = "mongodb://localhost/test";

  public static final String MC_APP_NAME = ".applicationName";
  public static final String MC_HOST_PORTS = ".host-ports";
  public static final String MC_URI = ".uri";
  public static final String MC_OPTS = ".option";
  public static final String MC_AUTH_DB = ".auth-database";
  public static final String MC_USER_NAME = ".username";
  public static final String MC_PASSWORD = ".password";
  public static final String MC_DATABASES = ".databases";

  private List<Pair<String, Integer>> hostAndPorts = new ArrayList<>();

  private String applicationName;

  private String name;

  private String uri;

  private Map<String, MongodbConfig> databases = new HashMap<>();

  private Map<String, String> options = new HashMap<>();

  private String authenticationDatabase;

  private String username;

  private char[] password = new char[0];

  public static Map<String, MongoClientConfig> from(Config config) {
    Map<String, MongoClientConfig> clients = new HashMap<>();
    // handle client
    Map<String, List<String>> clientCfgs = ConfigUtils.getGroupConfigNames(config, PREFIX, 1);
    clientCfgs.forEach((k, v) -> {
      MongoClientConfig client = of(config, k, v);
      shouldBeNull(clients.put(k, client), "Mongo client name %s dup!", k);
    });

    return clients;
  }

  static MongoClientConfig of(Config config, String client, Collection<String> propertieNames) {
    final MongoClientConfig mc = new MongoClientConfig();
    mc.setName(client);
    final String opPrefix = PREFIX + client + MC_OPTS;
    final int opPrefixLen = opPrefix.length();
    Set<String> opCfgNmes = new HashSet<>();
    propertieNames.forEach(pn -> {
      if (pn.startsWith(opPrefix) && pn.length() > opPrefixLen) {
        // handle options
        opCfgNmes.add(pn);
      } else if (pn.endsWith(MC_DATABASES)) {
        config.getOptionalValue(pn, String.class).ifPresent(dbns -> {
          for (String dn : split(dbns, ";", true, true)) {
            MongodbConfig dbc = new MongodbConfig();
            dbc.client = mc;
            dbc.setClientName(client);
            dbc.setName(dn);
            mc.databases.put(dn, dbc);
          }
        });
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
          set.forEach(hp -> mc.hostAndPorts.add(hp));
        });
      } else if (pn.endsWith(MC_URI)) {
        config.getOptionalValue(pn, String.class).ifPresent(mc::setUri);
      } else if (pn.endsWith(MC_AUTH_DB)) {
        config.getOptionalValue(pn, String.class).ifPresent(mc::setAuthenticationDatabase);
      } else if (pn.endsWith(MC_PASSWORD)) {
        config.getOptionalValue(pn, String.class).ifPresent(mc::setPassword);
      } else if (pn.endsWith(MC_USER_NAME)) {
        config.getOptionalValue(pn, String.class).ifPresent(mc::setUsername);
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
   * @return the databases
   */
  public Map<String, MongodbConfig> getDatabases() {
    return Collections.unmodifiableMap(databases);
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
   * @return the clientName
   */
  public String getName() {
    return name;
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
    return MongoCredential.createCredential(getUsername(), getAuthenticationDatabase(),
        getPassword());
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
   * @param hostAndPorts the hostAndPorts to set
   */
  protected void setHostAndPorts(List<Pair<String, Integer>> hostAndPorts) {
    this.hostAndPorts = hostAndPorts;
  }

  /**
   *
   * @param name the client name to set
   */
  protected void setName(String name) {
    this.name = name;
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

  /**
   * corant-suites-mongodb
   *
   * @author bingo 下午7:25:05
   *
   */
  public static class MongodbConfig {

    private String name;

    private String clientName;

    private MongoClientConfig client;

    /**
     *
     * @return the client
     */
    public MongoClientConfig getClient() {
      return client;
    }

    /**
     *
     * @return the clientName
     */
    public String getClientName() {
      return clientName;
    }

    /**
     *
     * @return the database
     */
    public String getName() {
      if (name != null) {
        return name;
      }
      return new MongoClientURI(defaultObject(client.getUri(), DEFAULT_URI)).getDatabase();
    }

    public String getNameSpace() {
      return getClientName() + Names.NAME_SPACE_SEPARATOR + getName();
    }

    /**
     *
     * @param clientName the clientName to set
     */
    protected void setClientName(String clientName) {
      this.clientName = clientName;
    }

    /**
     *
     * @param name the database to set
     */
    protected void setName(String name) {
      this.name = name;
    }

  }
}
