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
package org.corant.modules.mongodb;

import static org.corant.config.CorantConfigResolver.getGroupConfigKeys;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.defaultString;
import static org.corant.shared.util.Strings.defaultStrip;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNoneBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.split;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import org.corant.context.qualifier.Qualifiers;
import org.corant.context.qualifier.Qualifiers.DefaultNamedQualifierObjectManager;
import org.corant.context.qualifier.Qualifiers.NamedObject;
import org.corant.context.qualifier.Qualifiers.NamedQualifierObjectManager;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Names;
import org.corant.shared.normal.Names.JndiNames;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.Chars;
import org.eclipse.microprofile.config.Config;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientOptions.Builder;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;

/**
 * corant-modules-mongodb
 *
 * @author bingo 下午12:10:04
 *
 */
public class MongoClientConfig implements NamedObject {
  public static final String JNDI_SUBCTX_NAME = JndiNames.JNDI_COMP_NME + "/MongoClient";
  public static final int DEFAULT_PORT = 27017;
  public static final String DEFAULT_HOST = "localhost";
  public static final String DEFAULT_URI = "mongodb://localhost/test";

  public static final String MC_PREFIX = "corant.mongodb.";
  public static final String MC_APP_NAME = ".applicationName";
  public static final String MC_HOST_PORTS = ".host-ports";
  public static final String MC_URI = ".uri";
  public static final String MC_OPTS = ".option";
  public static final String MC_AUTH_DB = ".auth-database";
  public static final String MC_USER_NAME = ".username";
  public static final String MC_PASSWORD = ".password";
  public static final String MC_DATABASES = ".databases";
  public static final String MC_BIND_TO_JNDI = ".bind-to-jndi";

  private List<Pair<String, Integer>> hostAndPorts = new ArrayList<>();

  private String applicationName;

  private String name;

  private String uri;

  private Map<String, MongodbConfig> databases = new HashMap<>();

  private Map<String, String> options = new HashMap<>();

  private String authenticationDatabase;

  private String username;

  private char[] password = Chars.EMPTY_ARRAY;

  private boolean bindToJndi = false;

  public static NamedQualifierObjectManager<MongoClientConfig> from(Config config) {
    Set<MongoClientConfig> cfgs = new HashSet<>();
    Set<String> dfltCfgKeys = defaultPropertyNames(config);
    // handle named client
    Map<String, List<String>> clientCfgs = getGroupConfigKeys(config,
        s -> defaultString(s).startsWith(MC_PREFIX) && !dfltCfgKeys.contains(s), 2);
    clientCfgs.forEach((k, v) -> {
      MongoClientConfig cfg = of(config, k, v);
      shouldBeTrue(cfgs.add(cfg), "Mongo client databaseName %s dup!", k);
    });
    // find default configuration
    String dfltName =
        config.getOptionalValue(MC_PREFIX + "databaseName", String.class).orElse(null);
    MongoClientConfig dfltCfg = of(config, dfltName, dfltCfgKeys);
    if (isNotEmpty(dfltCfg.getHostAndPorts())) {
      cfgs.add(dfltCfg);
    }
    return new DefaultNamedQualifierObjectManager<>(cfgs);
  }

  static Set<String> defaultPropertyNames(Config config) {
    String dfltPrefix = MC_PREFIX.substring(0, MC_PREFIX.length() - 1);
    String dfltOptPrefix = dfltPrefix + MC_OPTS + Names.NAME_SPACE_SEPARATORS;
    Set<String> names = new LinkedHashSet<>();
    names.add(dfltPrefix + MC_APP_NAME);
    names.add(dfltPrefix + MC_AUTH_DB);
    names.add(dfltPrefix + MC_DATABASES);
    names.add(dfltPrefix + MC_HOST_PORTS);
    names.add(dfltPrefix + MC_PASSWORD);
    names.add(dfltPrefix + MC_URI);
    names.add(dfltPrefix + MC_USER_NAME);
    names.add(dfltPrefix + MC_BIND_TO_JNDI);
    // opt property
    for (String proNme : config.getPropertyNames()) {
      if (proNme.startsWith(dfltOptPrefix)) {
        names.add(proNme);
      }
    }
    return names;
  }

  static MongoClientConfig of(Config config, String client, Collection<String> propertieNames) {
    final MongoClientConfig mc = new MongoClientConfig();
    mc.setName(client);
    final String opPrefix =
        isBlank(client) ? MC_PREFIX + MC_OPTS.substring(1) : MC_PREFIX + client + MC_OPTS;
    final int opPrefixLen = opPrefix.length();
    Set<String> opCfgNmes = new HashSet<>();
    propertieNames.forEach(pn -> {
      if (pn.startsWith(opPrefix) && pn.length() > opPrefixLen) {
        // handle options
        opCfgNmes.add(pn);
      } else if (pn.endsWith(MC_DATABASES)) {
        config.getOptionalValue(pn, String.class).ifPresent(dbns -> {
          for (String dn : split(dbns, ",", true, true)) {
            mc.databases.put(dn, new MongodbConfig(mc, dn));
          }
        });
      } else if (pn.endsWith(MC_APP_NAME)) {
        config.getOptionalValue(pn, String.class).ifPresent(mc::setApplicationName);
      } else if (pn.endsWith(MC_HOST_PORTS)) {
        config.getOptionalValue(pn, String.class).ifPresent(hps -> {
          String[] hpArr = split(hps, ",", true, true);
          Set<Pair<String, Integer>> set = new LinkedHashSet<>(hpArr.length);
          for (String hp : hpArr) {
            String[] arr = split(hp, ":", true, true);
            if (arr.length > 1) {
              set.add(Pair.of(arr[0], Integer.valueOf(arr[1])));
            } else if (arr.length == 1) {
              set.add(Pair.of(arr[0], DEFAULT_PORT));
            }
          }
          mc.hostAndPorts.addAll(set);
        });
      } else if (pn.endsWith(MC_URI)) {
        config.getOptionalValue(pn, String.class).ifPresent(mc::setUri);
      } else if (pn.endsWith(MC_AUTH_DB)) {
        config.getOptionalValue(pn, String.class).ifPresent(dn -> {
          mc.setAuthenticationDatabase(dn);
          if (isNotBlank(dn)) {
            mc.databases.put(dn, new MongodbConfig(mc, dn));
          }
        });
      } else if (pn.endsWith(MC_PASSWORD)) {
        config.getOptionalValue(pn, String.class).ifPresent(mc::setPassword);
      } else if (pn.endsWith(MC_USER_NAME)) {
        config.getOptionalValue(pn, String.class).ifPresent(mc::setUsername);
      } else if (pn.endsWith(MC_BIND_TO_JNDI)) {
        config.getOptionalValue(pn, Boolean.class).ifPresent(mc::setBindToJndi);
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
    // FIXME
    // if (isEmpty(hostAndPorts)) {
    // return Collections.unmodifiableList(listOf(Pair.of(DEFAULT_HOST, DEFAULT_PORT)));
    // }
    return Collections.unmodifiableList(hostAndPorts);
  }

  /**
   *
   * @return the clientName
   */
  @Override
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

  /**
   *
   * @return the bindToJndi
   */
  public boolean isBindToJndi() {
    return bindToJndi;
  }

  public Builder produceBuiler() {
    Map<String, Pair<Method, Class<?>[]>> settingsMap = MongoClientConfigurator.createSettingsMap();
    MongoClientOptions.Builder optionsBuilder = MongoClientOptions.builder();
    for (Map.Entry<String, Pair<Method, Class<?>[]>> entry : settingsMap.entrySet()) {
      Class<?> type = entry.getValue().right()[0];
      if (int.class.equals(type)) {
        type = Integer.class;
      }
      if (boolean.class.equals(type)) {
        type = Boolean.class;
      }
      if (String.class.equals(type)) {
        type = String.class;
      }
      Object value = toObject(options.get(entry.getKey()), type);
      if (value == null) {
        continue;
      }
      try {
        entry.getValue().left().invoke(optionsBuilder, value);
      } catch (InvocationTargetException | IllegalAccessException e) {
        throw new CorantRuntimeException(e, "Unable to build mongo client options [%s]",
            entry.getKey());
      }
    }
    return optionsBuilder;
  }

  public MongoCredential produceCredential() {
    if (isNoneBlank(getUsername(), getAuthenticationDatabase()) && isNotEmpty(getPassword())) {
      return MongoCredential.createCredential(getUsername(), getAuthenticationDatabase(),
          getPassword());
    } else {
      return null;
    }
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
   * @param bindToJndi the bindToJndi to set
   */
  protected void setBindToJndi(boolean bindToJndi) {
    this.bindToJndi = bindToJndi;
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
   * @param name the client databaseName to set
   */
  protected void setName(String name) {
    this.name = Qualifiers.resolveName(name);
  }

  /**
   *
   * @param password the password to set
   */
  protected void setPassword(char[] password) {
    this.password = password;
  }

  protected void setPassword(String password) {
    this.password = isEmpty(password) ? Chars.EMPTY_ARRAY : password.toCharArray();
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
   * corant-modules-mongodb
   *
   * @author bingo 下午7:25:05
   *
   */
  public static class MongodbConfig implements NamedObject {

    private final String clientName;
    private final MongoClientConfig client;
    private final String name;
    private final String databaseName;

    public MongodbConfig(MongoClientConfig client, String databaseName) {
      this.client = shouldNotNull(client);
      clientName = client.getName();
      this.databaseName = isBlank(databaseName)
          ? new MongoClientURI(defaultObject(client.getUri(), DEFAULT_URI)).getDatabase()
          : defaultStrip(databaseName);
      name = isBlank(clientName) ? this.databaseName
          : clientName + Names.NAME_SPACE_SEPARATOR + this.databaseName;
    }

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
     * @return the databaseName
     */
    public String getDatabaseName() {
      return databaseName;
    }

    /**
     *
     * @return the database
     */
    @Override
    public String getName() {
      return name;
    }

  }
}
