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

import static org.corant.config.ConfigUtils.getGroupConfigNames;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.ConversionUtils.toInteger;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.StreamUtils.streamOf;
import static org.corant.shared.util.StringUtils.defaultString;
import static org.corant.shared.util.StringUtils.isNoneBlank;
import static org.corant.shared.util.StringUtils.isNotBlank;
import static org.corant.shared.util.StringUtils.split;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.corant.kernel.util.Qualifiers.DefaultNamedQualifierObjectManager;
import org.corant.kernel.util.Qualifiers.NamedQualifierObjectManager;
import org.corant.shared.util.MethodUtils;
import org.corant.shared.util.ObjectUtils;
import org.corant.shared.util.ObjectUtils.Pair;
import org.corant.suites.jms.shared.AbstractJMSConfig;
import org.eclipse.microprofile.config.Config;

/**
 * corant-suites-jms-artemis
 *
 * @author bingo 上午10:08:11
 *
 */
public class ArtemisConfig extends AbstractJMSConfig {

  public static final int DFLT_PORT = 61616;

  public static final String ATM_PREFIX = "jms.artemis.";

  public static final String ATM_USER_NAME = ".username";
  public static final String ATM_PASSWOED = ".password";
  public static final String ATM_URL = ".url";
  public static final String ATM_HA = ".ha";
  public static final String ATM_HOST_PORTS = ".host-ports";

  private static final Map<String, Method> propertiesMaps = new HashMap<>();
  static {
    propertiesMaps.putAll(createSettingsMap());
  }

  private String username;
  private String password;
  private String url;
  private Set<Pair<String, Integer>> hostPorts = new LinkedHashSet<>();
  private boolean ha;
  private Map<Method, Optional<?>> properties = new LinkedHashMap<>();

  protected ArtemisConfig() {}

  /**
   * @param connectionFactoryId
   * @param username
   * @param password
   * @param url
   * @param hostPorts
   * @param ha
   * @param xa
   */
  protected ArtemisConfig(String connectionFactoryId, String username, String password, String url,
      Collection<Pair<String, Integer>> hostPorts, boolean ha, boolean xa) {
    super();
    setConnectionFactoryId(connectionFactoryId);
    setUsername(username);
    setPassword(password);
    setUrl(url);
    if (isNotEmpty(hostPorts)) {
      setHostPorts(new LinkedHashSet<>(hostPorts));
    }
    setHa(ha);
    setXa(xa);
  }

  public static NamedQualifierObjectManager<ArtemisConfig> from(Config config) {
    Set<ArtemisConfig> cfgs = new HashSet<>();
    Set<String> dfltCfgKeys = defaultPropertyNames();
    // handle named artemis configuration
    Map<String, List<String>> namedCfgKeys = getGroupConfigNames(config,
        s -> defaultString(s).startsWith(ATM_PREFIX) && !dfltCfgKeys.contains(s), 2);
    namedCfgKeys.forEach((k, v) -> {
      final ArtemisConfig cfg = of(config, k, v);
      if (cfg != null) {
        shouldBeTrue(cfgs.add(cfg), "The artemis connection factory id %s configuration dup!",
            cfg.getConnectionFactoryId());
      }
    });
    // handle default configuration
    String dfltName = config.getOptionalValue(ATM_PREFIX + "id", String.class).orElse(null);
    ArtemisConfig dfltCfg = of(config, dfltName, dfltCfgKeys);
    if (dfltCfg != null) {
      shouldBeTrue(cfgs.add(dfltCfg), "The artemis connection factory id %s configuration dup!",
          dfltName);
    }
    return new DefaultNamedQualifierObjectManager<>(cfgs);
  }

  static Map<String, Method> createSettingsMap() {
    Map<String, Method> settingsMap = new HashMap<>();
    Method[] methods = ActiveMQConnectionFactory.class.getDeclaredMethods();
    for (Method method : methods) {
      if (MethodUtils.isSetter(method)) {
        Class<?> parameterType = method.getParameterTypes()[0];
        if (String.class.equals(parameterType) || int.class.equals(parameterType)
            || long.class.equals(parameterType) || double.class.equals(parameterType)
            || boolean.class.equals(parameterType)) {
          settingsMap.put("." + method.getName().substring(3, 4).toLowerCase(Locale.ENGLISH)
              + method.getName().substring(4), method);
        }
      }
    }
    return settingsMap;
  }

  static Set<String> defaultPropertyNames() {
    String dfltPrefix = ATM_PREFIX.substring(0, ATM_PREFIX.length() - 1);
    Set<String> names = new LinkedHashSet<>();
    names.add(dfltPrefix + JMS_ENABLE);
    names.add(dfltPrefix + JMS_REC_TSK_DELAYMS);
    names.add(dfltPrefix + JMS_REC_TSK_INIT_DELAYMS);
    names.add(dfltPrefix + JMS_REC_TSK_THREADS);
    names.add(dfltPrefix + JMS_XA);
    names.add(dfltPrefix + ATM_USER_NAME);
    names.add(dfltPrefix + ATM_PASSWOED);
    names.add(dfltPrefix + ATM_URL);
    names.add(dfltPrefix + ATM_HOST_PORTS);
    names.add(dfltPrefix + ATM_HA);
    propertiesMaps.keySet().forEach(s -> {
      names.add(dfltPrefix + s);
    });
    return names;
  }

  static ArtemisConfig of(Config config, String connectionFactoryId,
      Collection<String> propertieNames) {
    final ArtemisConfig cfg = new ArtemisConfig();
    cfg.setConnectionFactoryId(connectionFactoryId);
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
      } else if (pn.endsWith(JMS_ENABLE)) {
        config.getOptionalValue(pn, Boolean.class).ifPresent(cfg::setEnable);
      } else if (pn.endsWith(JMS_XA)) {
        config.getOptionalValue(pn, Boolean.class).ifPresent(cfg::setXa);
      } else if (pn.endsWith(JMS_REC_TSK_DELAYMS)) {
        config.getOptionalValue(pn, Long.class).ifPresent(cfg::setReceiveTaskDelayMs);
      } else if (pn.endsWith(JMS_REC_TSK_INIT_DELAYMS)) {
        config.getOptionalValue(pn, Long.class).ifPresent(cfg::setReceiveTaskInitialDelayMs);
      } else if (pn.endsWith(JMS_REC_TSK_THREADS)) {
        config.getOptionalValue(pn, Integer.class).ifPresent(cfg::setReceiveTaskThreads);
      } else {
        propertiesMaps.forEach((k, m) -> {
          if (pn.endsWith(k)) {
            cfg.properties.put(m, config.getOptionalValue(pn, m.getParameterTypes()[0]));
          }
        });
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

  public Set<Pair<String, Integer>> getHostPorts() {
    return Collections.unmodifiableSet(hostPorts);
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

  protected Map<Method, Optional<?>> getProperties() {
    return Collections.unmodifiableMap(properties);
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
