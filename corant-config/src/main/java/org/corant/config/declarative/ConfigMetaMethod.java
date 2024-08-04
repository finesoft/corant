/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.config.declarative;

import java.lang.reflect.Method;

/**
 * corant-config
 *
 * @author bingo 11:17:04
 */
public class ConfigMetaMethod {

  private final ConfigMetaClass configClass;
  private final Method method;
  private final String keyItem;
  private final ConfigInjector injector;
  private final String defaultValue;
  private final String defaultKey;
  private final String defaultNull;

  protected ConfigMetaMethod(ConfigMetaClass configClass, Method method, String keyItem,
      ConfigInjector injector, String defaultValue, String defaultKey, String defaultNull) {
    this.configClass = configClass;
    this.method = method;
    this.keyItem = keyItem;
    this.injector = injector;
    this.defaultValue = defaultValue;
    this.defaultKey = defaultKey;
    this.defaultNull = defaultNull;
  }

  public ConfigMetaClass getConfigClass() {
    return configClass;
  }

  public String getDefaultKey() {
    return defaultKey;
  }

  public String getDefaultValue() {
    return defaultValue.equals(defaultNull) ? null : defaultValue;
  }

  public ConfigInjector getInjector() {
    return injector;
  }

  public String getKeyItem() {
    return keyItem;
  }

  public String getKeyRoot() {
    return configClass.getKeyRoot();
  }

  public Method getMethod() {
    return method;
  }

  @Override
  public String toString() {
    return "ConfigMetaProperty [configClass=" + configClass.getClazz() + ", method="
        + method.getName() + ", keyItem=" + keyItem + ", injector=" + injector + ", defaultValue="
        + defaultValue + ", defaultKey=" + defaultKey + ", defaultNull=" + defaultNull + "]";
  }

}
