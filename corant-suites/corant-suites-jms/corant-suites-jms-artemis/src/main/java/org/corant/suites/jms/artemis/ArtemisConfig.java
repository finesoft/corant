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

import static org.corant.shared.util.Conversions.toInteger;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNoneBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.split;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.corant.config.ConfigUtils;
import org.corant.config.declarative.ConfigKeyItem;
import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativePattern;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.Methods;
import org.corant.suites.jms.shared.AbstractJMSConfig;
import org.eclipse.microprofile.config.Config;

/**
 * corant-suites-jms-artemis
 *
 * @author bingo 上午10:08:11
 *
 */
@ConfigKeyRoot(value = "jms.artemis", keyIndex = 2)
public class ArtemisConfig extends AbstractJMSConfig {

  private static final long serialVersionUID = 7757438714150588283L;

  public static final int DFLT_PORT = 61616;

  private static final Map<String, Method> propertiesMaps = new HashMap<>(); // static?
  static {
    propertiesMaps.putAll(createSettingsMap());
  }

  @ConfigKeyItem
  protected String url;

  @ConfigKeyItem(defaultValue = "false")
  protected Boolean ha = false;

  @ConfigKeyItem
  protected List<String> hostPorts = new ArrayList<>();

  @ConfigKeyItem(pattern = DeclarativePattern.PREFIX)
  protected Map<String, Object> additionProperties = new HashMap<>();

  protected final Map<Method, Optional<?>> properties = new LinkedHashMap<>();

  protected final Set<Pair<String, Integer>> hostPortPairs = new LinkedHashSet<>();

  static Map<String, Method> createSettingsMap() {
    Map<String, Method> settingsMap = new HashMap<>();
    Method[] methods = ActiveMQConnectionFactory.class.getDeclaredMethods();
    for (Method method : methods) {
      if (Methods.isSetter(method)) {
        Class<?> parameterType = method.getParameterTypes()[0];
        if (String.class.equals(parameterType) || int.class.equals(parameterType)
            || long.class.equals(parameterType) || double.class.equals(parameterType)
            || boolean.class.equals(parameterType)) {
          settingsMap
              .put(ConfigUtils.dashify(method.getName().substring(3, 4).toLowerCase(Locale.ENGLISH)
                  + method.getName().substring(4)), method);
        }
      }
    }
    return settingsMap;
  }

  public String getConnectorFactory() {
    return NettyConnectorFactory.class.getName();
  }

  public Set<Pair<String, Integer>> getHostPortPairs() {
    return Collections.unmodifiableSet(hostPortPairs);
  }

  public List<String> getHostPorts() {
    return hostPorts;
  }

  public String getUrl() {
    return url;
  }

  public boolean hasAuthentication() {
    return getUsername() != null && getPassword() != null;
  }

  public Boolean isHa() {
    return ha;
  }

  @Override
  public boolean isValid() {
    return isNotBlank(getUrl()) || isNotEmpty(getHostPorts());
  }

  @Override
  public void onPostConstruct(Config config, String key) {
    if (isBlank(connectionFactoryId)) {
      connectionFactoryId = key;
    }
    if (isNotEmpty(getHostPorts())) {
      for (String x : getHostPorts()) {
        String[] arr = split(x, ":", true, true);
        if (isNoneBlank(arr)) {
          if (arr.length > 1) {
            hostPortPairs.add(Pair.of(arr[0], toInteger(arr[1])));
          } else {
            hostPortPairs.add(Pair.of(arr[0], DFLT_PORT));
          }
        }
      }
    }
    if (isNotEmpty(additionProperties)) {
      additionProperties.forEach((p, v) -> {
        propertiesMaps.forEach((k, m) -> {
          if (p.equals(k)) {
            properties.put(m, Optional.ofNullable(toObject(v, m.getParameterTypes()[0])));
          }
        });
      });
    }
  }

  protected Map<Method, Optional<?>> getProperties() {
    return Collections.unmodifiableMap(properties);
  }
}
