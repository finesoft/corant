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

import static org.corant.shared.util.ClassUtils.tryAsClass;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.MapUtils.getOptMapObject;
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
import org.corant.shared.util.ObjectUtils;
import org.corant.shared.util.ObjectUtils.Pair;
import org.eclipse.microprofile.config.Config;
import com.mongodb.MongoClientOptions.Builder;

/**
 * corant-suites-mongodb
 *
 * @author bingo 下午12:10:04
 *
 */
public class MongoClientConfig {

  public static final String PREFIX = "mongodb.";
  public static final int DEFAULT_PORT = 27017;
  public static final String DEFAULT_URI = "mongodb://localhost/test";

  public static final String MC_HOST_PORTS = ".host-ports";
  public static final String MC_URI = ".uri";
  public static final String MC_DB = ".database";
  public static final String MC_AUTH_DB = ".auth-database";
  public static final String MC_GRIDFS_DB = ".gridfs-database";
  public static final String MC_USER_NAME = ".user-name";
  public static final String MC_PASSWORD = ".password";
  public static final String MC_FIELD_NME_STRATEGY = ".field-naming-strategy";
  public static final String MC_OPTS = ".option";

  private List<Pair<String, Integer>> hostAndPorts = new ArrayList<>();

  private String applicationName;

  private String uri;

  private String database;

  private String authenticationDatabase;

  private String gridFsDatabase;

  private String username;

  private char[] password = new char[0];

  private Class<?> fieldNamingStrategy;

  private Map<String, String> options = new HashMap<>();

  public static Map<String, MongoClientConfig> from(Config config) {
    Map<String, MongoClientConfig> map = new HashMap<>();
    Map<String, List<String>> cfgNmes = ConfigUtils.getGroupConfigNames(config, PREFIX, 1);
    cfgNmes.forEach((k, v) -> {
      final MongoClientConfig cfg = of(config, k, v);
      if (isNoneBlank(cfg.uri)) {
        map.put(k, cfg);
      }
    });
    return map;
  }

  public static MongoClientConfig of(Config config, String name,
      Collection<String> propertieNames) {
    final MongoClientConfig mc = new MongoClientConfig();
    final String opPrefix = PREFIX + name + MC_OPTS;
    final int opPrefixLen = opPrefix.length();
    Set<String> opCfgNmes = new HashSet<>();
    mc.setApplicationName(name);
    propertieNames.forEach(pn -> {
      if (pn.endsWith(MC_AUTH_DB)) {
        config.getOptionalValue(pn, String.class).ifPresent(mc::setAuthenticationDatabase);
      } else if (pn.endsWith(MC_DB)) {
        config.getOptionalValue(pn, String.class).ifPresent(mc::setDatabase);
      } else if (pn.endsWith(MC_FIELD_NME_STRATEGY)) {
        config.getOptionalValue(pn, String.class)
            .ifPresent(cls -> mc.setFieldNamingStrategy(tryAsClass(cls)));
      } else if (pn.endsWith(MC_GRIDFS_DB)) {
        config.getOptionalValue(pn, String.class).ifPresent(mc::setGridFsDatabase);
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
   * @return the database
   */
  public String getDatabase() {
    return database;
  }

  /**
   *
   * @return the fieldNamingStrategy
   */
  public Class<?> getFieldNamingStrategy() {
    return fieldNamingStrategy;
  }

  /**
   *
   * @return the gridFsDatabase
   */
  public String getGridFsDatabase() {
    return gridFsDatabase;
  }

  /**
   *
   * @return the hostAndPorts
   */
  public List<Pair<String, Integer>> getHostAndPorts() {
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
    getOptMapObject(options, "description", ObjectUtils::asString).ifPresent(builder::description);
    return builder;
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
   * @param database the database to set
   */
  protected void setDatabase(String database) {
    this.database = database;
  }

  /**
   *
   * @param fieldNamingStrategy the fieldNamingStrategy to set
   */
  protected void setFieldNamingStrategy(Class<?> fieldNamingStrategy) {
    this.fieldNamingStrategy = fieldNamingStrategy;
  }

  /**
   *
   * @param gridFsDatabase the gridFsDatabase to set
   */
  protected void setGridFsDatabase(String gridFsDatabase) {
    this.gridFsDatabase = gridFsDatabase;
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
