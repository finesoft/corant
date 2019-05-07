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
package org.corant.suites.jms.artemis;

import static org.corant.kernel.util.Configurations.getGroupConfigNames;
import static org.corant.shared.util.Assertions.shouldBeNull;
import static org.corant.shared.util.ConversionUtils.toEnum;
import static org.corant.shared.util.ConversionUtils.toInteger;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.StreamUtils.streamOf;
import static org.corant.shared.util.StringUtils.defaultString;
import static org.corant.shared.util.StringUtils.defaultTrim;
import static org.corant.shared.util.StringUtils.isNoneBlank;
import static org.corant.shared.util.StringUtils.isNotBlank;
import static org.corant.shared.util.StringUtils.split;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.activemq.artemis.api.jms.JMSFactoryType;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.corant.shared.util.ObjectUtils;
import org.corant.shared.util.ObjectUtils.Pair;
import org.eclipse.microprofile.config.Config;

/**
 * corant-suites-jms-artemis
 *
 * @author bingo 上午10:08:11
 *
 */
public class ArtemisConfig {

  public static final int DFLT_PORT = 61616;
  public static final String ATM_PREFIX = "jms.artemis.";
  public static final String ATM_USER_NAME = ".username";
  public static final String ATM_PASSWOED = ".password";
  public static final String ATM_URL = ".url";
  public static final String ATM_HA = ".ha";
  public static final String ATM_FT = ".factory-type";
  public static final String ATM_HOST_PORTS = ".host-ports";

  private String name; // the connection factory name means a artemis server or cluster
  private String username;
  private String password;
  private String url;
  private JMSFactoryType factoryType;
  private Set<Pair<String, Integer>> hostPorts = new LinkedHashSet<>();
  private boolean ha;

  protected ArtemisConfig() {}

  /**
   * @param name
   * @param username
   * @param password
   * @param url
   * @param hostPorts
   * @param ha
   * @param factoryType
   */
  protected ArtemisConfig(String name, String username, String password, String url,
      Collection<Pair<String, Integer>> hostPorts, boolean ha, JMSFactoryType factoryType) {
    super();
    this.name = name;
    this.username = username;
    this.password = password;
    this.url = url;
    if (isNotEmpty(hostPorts)) {
      this.hostPorts.addAll(hostPorts);
    }
    this.ha = ha;
    this.factoryType = factoryType;
  }

  public static Map<String, ArtemisConfig> from(Config config) {
    Map<String, ArtemisConfig> artMisCfgs = new LinkedHashMap<>();
    Set<String> dfltCfgKeys = defaultPropertyNames();
    // handle named artemis configuration
    Map<String, List<String>> namedCfgKeys = getGroupConfigNames(config,
        s -> defaultString(s).startsWith(ATM_PREFIX) && !dfltCfgKeys.contains(s), 2);
    namedCfgKeys.forEach((k, v) -> {
      final ArtemisConfig cfg = of(config, k, v);
      if (cfg != null) {
        shouldBeNull(artMisCfgs.put(defaultTrim(cfg.getName()), cfg),
            "The artemis named %s configuration dup!", cfg.getName());
      }
    });
    // handle default configuration
    String dfltName = config.getOptionalValue(ATM_PREFIX + "name", String.class).orElse(null);
    ArtemisConfig dfltCfg = of(config, dfltName, dfltCfgKeys);
    if (dfltCfg != null) {
      shouldBeNull(artMisCfgs.put(defaultTrim(dfltCfg.getName()), dfltCfg),
          "The artemis named %s configuration dup!", dfltName);
    }
    return artMisCfgs;
  }

  static Set<String> defaultPropertyNames() {
    String dfltPrefix = ATM_PREFIX.substring(0, ATM_PREFIX.length() - 1);
    Set<String> names = new LinkedHashSet<>();
    names.add(dfltPrefix + ATM_USER_NAME);
    names.add(dfltPrefix + ATM_PASSWOED);
    names.add(dfltPrefix + ATM_URL);
    names.add(dfltPrefix + ATM_HOST_PORTS);
    names.add(dfltPrefix + ATM_HA);
    names.add(dfltPrefix + ATM_FT);
    return names;
  }

  static ArtemisConfig of(Config config, String name, Collection<String> propertieNames) {
    final ArtemisConfig cfg = new ArtemisConfig();
    cfg.setName(name);
    propertieNames.forEach(pn -> {
      if (pn.endsWith(ATM_USER_NAME)) {
        config.getOptionalValue(pn, String.class).ifPresent(cfg::setUsername);
      } else if (pn.endsWith(ATM_PASSWOED)) {
        config.getOptionalValue(pn, String.class).ifPresent(cfg::setPassword);
      } else if (pn.endsWith(ATM_URL)) {
        config.getOptionalValue(pn, String.class).ifPresent(cfg::setUrl);
      } else if (pn.endsWith(ATM_HOST_PORTS)) {
        config.getOptionalValue(pn, String.class)
            .ifPresent(hps -> cfg.setHostPorts(streamOf(split(hps, ",", true, true)).map(x -> {
              String[] arr = split(x, ":", true, true);
              if (isNoneBlank(arr)) {
                if (arr.length > 1) {
                  return Pair.of(arr[0], toInteger(arr[1]));
                } else {
                  return Pair.of(arr[0], DFLT_PORT);
                }
              }
              return null;
            }).filter(ObjectUtils::isNotNull).collect(Collectors.toSet())));
      } else if (pn.endsWith(ATM_HA)) {
        config.getOptionalValue(pn, Boolean.class).ifPresent(cfg::setHa);
      } else if (pn.endsWith(ATM_FT)) {
        String ft = config.getOptionalValue(pn, String.class).orElse("CF");
        cfg.setFactoryType(toEnum(ft, JMSFactoryType.class));
      }
    });
    if (isNotBlank(cfg.getUrl()) || isNotEmpty(cfg.getHostPorts())) {
      return cfg;
    }
    return null;
  }

  public String getConnectorFactory() {
    return NettyConnectorFactory.class.getName();
  }

  public JMSFactoryType getFactoryType() {
    return factoryType;
  }

  public Set<Pair<String, Integer>> getHostPorts() {
    return Collections.unmodifiableSet(hostPorts);
  }

  public String getName() {
    return name;
  }

  public String getPassword() {
    return password;
  }

  public String getUrl() {
    return url;
  }

  public String getUsername() {
    return username;
  }

  public boolean hasAuthentication() {
    return getUsername() != null && getPassword() != null;
  }

  public boolean isHa() {
    return ha;
  }

  protected void setFactoryType(JMSFactoryType factoryType) {
    this.factoryType = factoryType;
  }

  protected void setHa(boolean ha) {
    this.ha = ha;
  }

  protected void setHostPorts(Set<Pair<String, Integer>> hostPorts) {
    this.hostPorts.clear();
    if (isNotEmpty(hostPorts)) {
      this.hostPorts.addAll(hostPorts);
    }
  }

  protected void setName(String name) {
    this.name = name;
  }

  protected void setPassword(String password) {
    this.password = password;
  }

  protected void setUrl(String url) {
    this.url = url;
  }

  protected void setUsername(String username) {
    this.username = username;
  }

}
