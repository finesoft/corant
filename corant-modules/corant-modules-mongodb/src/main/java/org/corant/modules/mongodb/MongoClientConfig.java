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
import static org.corant.shared.util.Lists.immutableList;
import static org.corant.shared.util.Objects.isNotNull;
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
import java.util.logging.Logger;
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
import com.mongodb.AuthenticationMechanism;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientSettings.Builder;
import com.mongodb.MongoCredential;

/**
 * corant-modules-mongodb
 *
 * @author bingo 下午12:10:04
 *
 */
public class MongoClientConfig implements NamedObject {

  public static final Logger logger = Logger.getLogger(MongoClientConfig.class.getCanonicalName());

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
  public static final String MC_AUTH_ME = "auth-mechanism";
  public static final String MC_USER_NAME = ".username";
  public static final String MC_PASSWORD = ".password";
  public static final String MC_DATABASES = ".databases";
  public static final String MC_BIND_TO_JNDI = ".bind-to-jndi";

  private List<Pair<String, Integer>> hostAndPorts = new ArrayList<>();

  private String applicationName;

  private String name;

  private ConnectionString uri;

  private Map<String, MongodbConfig> databases = new HashMap<>();

  private Map<String, String> options = new HashMap<>();

  private String authenticationDatabase;

  private String authenticationMechanism;

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
      if (cfg != null) {
        shouldBeTrue(cfgs.add(cfg), "Mongo client databaseName %s dup!", k);
      }
    });
    // find default configuration
    String dfltName =
        config.getOptionalValue(MC_PREFIX + "databaseName", String.class).orElse(null);
    MongoClientConfig dfltCfg = of(config, dfltName, dfltCfgKeys);
    if (dfltCfg != null && (isNotEmpty(dfltCfg.getHostAndPorts()) || isNotNull(dfltCfg.getUri()))) {
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
    names.add(dfltPrefix + MC_AUTH_ME);
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
      } else if (pn.endsWith(MC_AUTH_DB)) {
        config.getOptionalValue(pn, String.class).ifPresent(dn -> {
          mc.setAuthenticationDatabase(dn);
          if (isNotBlank(dn)) {
            mc.databases.put(dn, new MongodbConfig(mc, dn));
          }
        });
      } else if (pn.endsWith(MC_AUTH_ME)) {
        config.getOptionalValue(pn, String.class).ifPresent(mc::setAuthenticationMechanism);
      } else if (pn.endsWith(MC_PASSWORD)) {
        config.getOptionalValue(pn, String.class).ifPresent(mc::setPassword);
      } else if (pn.endsWith(MC_USER_NAME)) {
        config.getOptionalValue(pn, String.class).ifPresent(mc::setUsername);
      } else if (pn.endsWith(MC_BIND_TO_JNDI)) {
        config.getOptionalValue(pn, Boolean.class).ifPresent(mc::setBindToJndi);
      } else if (pn.endsWith(MC_DATABASES)) {
        config.getOptionalValue(pn, String.class).ifPresent(dbns -> {
          for (String dn : split(dbns, ",", true, true)) {
            mc.databases.put(dn, new MongodbConfig(mc, dn));
          }
        });
      } else if (pn.endsWith(MC_URI)) {
        config.getOptionalValue(pn, String.class).map(ConnectionString::new).ifPresent(mc::setUri);
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
    if (isNotEmpty(mc.hostAndPorts) || isNotNull(mc.uri)) {
      return mc;
    } else {
      logger.warning("Can't not find any hosts or uris in mongodb configurations");
      return null;
    }
  }

  /**
   * @return the applicationName
   */
  public String getApplicationName() {
    return applicationName;
  }

  /**
   * @return the authenticationDatabase
   */
  public String getAuthenticationDatabase() {
    return authenticationDatabase;
  }

  /**
   * @return getAuthenticationMechanism
   */
  public String getAuthenticationMechanism() {
    return authenticationMechanism;
  }

  /**
   * @return the databases
   */
  public Map<String, MongodbConfig> getDatabases() {
    return Collections.unmodifiableMap(databases);
  }

  /**
   * @return the hostAndPorts
   */
  public List<Pair<String, Integer>> getHostAndPorts() {
    return immutableList(hostAndPorts);
  }

  /**
   * @return the clientName
   */
  @Override
  public String getName() {
    return name;
  }

  /**
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
   * @return the uri
   */
  public ConnectionString getUri() {
    return uri;
  }

  /**
   * @return the username
   */
  public String getUsername() {
    return username;
  }

  /**
   * @return the bindToJndi
   */
  public boolean isBindToJndi() {
    return bindToJndi;
  }

  public Builder produceBuiler() {
    Map<String, Pair<Method, Class<?>[]>> settingsMap = MongoClientConfigurator.createSettingsMap();
    MongoClientSettings.Builder optionsBuilder = MongoClientSettings.builder();
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
    if (isNotBlank(getAuthenticationMechanism())) {
      AuthenticationMechanism am =
          AuthenticationMechanism.fromMechanismName(getAuthenticationMechanism());
      switch (am) {
        case PLAIN:
          return MongoCredential.createPlainCredential(getUsername(), getAuthenticationDatabase(),
              getPassword());
        case GSSAPI:
          return MongoCredential.createGSSAPICredential(getUsername());
        case SCRAM_SHA_1:
          return MongoCredential.createScramSha1Credential(getUsername(),
              getAuthenticationDatabase(), getPassword());
        case MONGODB_X509:
          return MongoCredential.createMongoX509Credential(getUsername());
        case SCRAM_SHA_256:
          return MongoCredential.createScramSha256Credential(getUsername(),
              getAuthenticationDatabase(), getPassword());
        default:
          throw new CorantRuntimeException(
              "Can't support mongodb authentication mechanism: %s" + getAuthenticationMechanism());
      }
    } else if (isNoneBlank(getUsername(), getAuthenticationDatabase())
        && isNotEmpty(getPassword())) {
      return MongoCredential.createCredential(getUsername(), getAuthenticationDatabase(),
          getPassword());
    } else {
      return null;
    }
  }

  protected void setApplicationName(String applicationName) {
    this.applicationName = applicationName;
  }

  protected void setAuthenticationDatabase(String authenticationDatabase) {
    this.authenticationDatabase = authenticationDatabase;
  }

  protected void setAuthenticationMechanism(String authenticationMechanism) {
    this.authenticationMechanism = authenticationMechanism;
  }

  protected void setBindToJndi(boolean bindToJndi) {
    this.bindToJndi = bindToJndi;
  }

  protected void setHostAndPorts(List<Pair<String, Integer>> hostAndPorts) {
    this.hostAndPorts = hostAndPorts;
  }

  protected void setName(String name) {
    this.name = Qualifiers.resolveName(name);
  }

  protected void setPassword(char[] password) {
    this.password = password;
  }

  protected void setPassword(String password) {
    this.password = isEmpty(password) ? Chars.EMPTY_ARRAY : password.toCharArray();
  }

  protected void setUri(ConnectionString uri) {
    this.uri = shouldNotNull(uri);
    databases.put(getName(), new MongodbConfig(this));
  }

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

    public MongodbConfig(MongoClientConfig client) {
      this.client = shouldNotNull(client);
      clientName = this.client.getName();
      databaseName = shouldNotNull(client.getUri()).getDatabase();
      name = isBlank(clientName) ? databaseName
          : clientName + Names.NAME_SPACE_SEPARATOR + databaseName;
    }

    public MongodbConfig(MongoClientConfig client, String databaseName) {
      this.client = shouldNotNull(client);
      clientName = client.getName();
      this.databaseName = defaultStrip(databaseName);
      name = isBlank(clientName) ? this.databaseName
          : clientName + Names.NAME_SPACE_SEPARATOR + this.databaseName;
    }

    /**
     * @return the client
     */
    public MongoClientConfig getClient() {
      return client;
    }

    /**
     * @return the clientName
     */
    public String getClientName() {
      return clientName;
    }

    /**
     * @return the databaseName
     */
    public String getDatabaseName() {
      return databaseName;
    }

    /**
     * @return the database
     */
    @Override
    public String getName() {
      return name;
    }

  }
}
