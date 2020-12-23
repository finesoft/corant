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
package org.corant.config;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Conversions.toObject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-config
 *
 * @author bingo 下午2:53:54
 *
 */
public class PropertyInjector {

  private final Object instance;
  private final Class<?> instanceClass;
  private final Map<String, Method> propertySetters;

  public PropertyInjector(Object instance) {
    this.instance = shouldNotNull(instance, "The object to inject can't null");
    instanceClass = getUserClass(instance);
    Map<String, Method> setters = new HashMap<>();
    for (Method method : instanceClass.getMethods()) {
      String name = method.getName();
      if (method.getParameterCount() == 1 && name.startsWith("set")) {
        setters.put(name.substring(3), method);
      } else if (method.getParameterCount() == 2 && "setProperty".equals(name)) {
        setters.put("Property", method);
      }
    }
    propertySetters = Collections.unmodifiableMap(setters);
  }

  public Map<String, Method> getPropertySetters() {
    return propertySetters;
  }

  public void inject(Map<String, ?> properties) {
    if (properties != null) {
      properties.forEach((k, v) -> {
        try {
          this.inject(k, v);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
          throw new CorantRuntimeException(e);
        }
      });
    }
  }

  public void inject(String propertyName, Object propertyValue)
      throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
    String realName =
        propertyName.substring(0, 1).toUpperCase(Locale.ROOT) + propertyName.substring(1);
    if (propertySetters.containsKey(realName)) {
      Method method = propertySetters.get(realName);
      method.invoke(instance, toObject(propertyValue, method.getParameterTypes()[0]));
    } else if (propertySetters.containsKey("Property")) {
      instanceClass.getMethod("setProperty", propertyName.getClass(), propertyValue.getClass())
          .invoke(instance, propertyName, propertyValue);
    } else {
      throw new NoSuchMethodException("No setter in class " + instanceClass.getName());
    }
  }

}
