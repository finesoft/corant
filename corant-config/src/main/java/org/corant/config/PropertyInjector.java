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

import static java.util.Collections.emptyMap;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Classes.getUserClass;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.corant.shared.conversion.Conversion;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Methods;
import org.corant.shared.util.Primitives;

/**
 * corant-config
 *
 * @author bingo 下午2:53:54
 */
public class PropertyInjector {

  private final boolean supportPropertySetter;
  private final Class<?> instanceClass;
  private final Map<String, Method> propertySetters;
  private final Map<String, Method> propertyGetters;

  public PropertyInjector(Class<?> instanceClass) {
    this(instanceClass, false);
  }

  public PropertyInjector(Class<?> instanceClass, boolean supportPropertySetter) {
    this.instanceClass =
        getUserClass(shouldNotNull(instanceClass, "The class to inject can't null"));
    this.supportPropertySetter = supportPropertySetter;
    Map<String, Method> setters = new HashMap<>();
    Map<String, Method> getters = new HashMap<>();
    for (Method method : instanceClass.getMethods()) {
      if (method.isBridge()) {
        // TODO consider the bridge method "change" visibility of base class's methods ??
        continue;
      }
      String methodName = method.getName();
      if (method.getParameterCount() == 1 && methodName.startsWith("set")) {
        String propertyName = decapitalize(methodName.substring(3));
        method.setAccessible(true);
        setters.put(propertyName, method);
        resolveGetter(method).ifPresent(m -> {
          m.setAccessible(true);
          getters.put(propertyName, m);
        });
      } else if (supportPropertySetter && method.getParameterCount() == 2
          && "setProperty".equals(methodName)) {
        method.setAccessible(true);
        setters.put("Property", method);
      }
    }
    propertySetters = Collections.unmodifiableMap(setters);
    propertyGetters = Collections.unmodifiableMap(getters);
  }

  protected static String decapitalize(String name) {
    if (name == null || name.length() == 0) {
      return name;
    }
    if (name.length() > 1 && Character.isUpperCase(name.charAt(1))
        && Character.isUpperCase(name.charAt(0))) {
      return name;
    }
    char[] chars = name.toCharArray();
    chars[0] = Character.toLowerCase(chars[0]);
    return new String(chars);
  }

  protected static Optional<Method> resolveGetter(Method setter) {
    String propertyName = setter.getName().substring(3);
    Class<?> propertyType = Primitives.wrap(setter.getParameterTypes()[0]);
    Method getter = null;
    if (propertyType.equals(Boolean.class)) {
      getter = Methods.getMatchingMethod(setter.getDeclaringClass(), "is" + propertyName);
    }
    if (getter == null) {
      getter = Methods.getMatchingMethod(setter.getDeclaringClass(), "get" + propertyName);
    }
    if (getter != null && Primitives.wrap(getter.getReturnType()).isAssignableFrom(propertyType)) {
      return Optional.of(getter);
    }
    return Optional.empty();
  }

  public Map<String, Method> getPropertyGetters() {
    return propertyGetters;
  }

  public List<PropertyMethod> getPropertyMethods() {
    List<PropertyMethod> methods = new ArrayList<>();
    propertySetters.forEach((p, w) -> {
      methods.add(new PropertyMethod(p, propertyGetters.get(p), w));
    });
    return methods;
  }

  public Map<String, Method> getPropertySetters() {
    return propertySetters;
  }

  public void inject(Object instance, Map<String, ?> properties) {
    if (properties != null) {
      properties.forEach((k, v) -> {
        try {
          this.inject(instance, k, v);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
          throw new CorantRuntimeException(e);
        }
      });
    }
  }

  public void inject(Object instance, String propertyName, Object propertyValue)
      throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
    String realName = decapitalize(propertyName);
    if (propertySetters.containsKey(realName)) {
      Method method = propertySetters.get(realName);
      // TODO FIXME complex object type
      method.invoke(instance,
          Conversion.convertType(propertyValue, method.getGenericParameterTypes()[0], emptyMap()));
    } else if (supportPropertySetter && propertySetters.containsKey("Property")) {
      instanceClass.getMethod("setProperty", propertyName.getClass(), propertyValue.getClass())
          .invoke(instance, propertyName, propertyValue);
    } else {
      throw new NoSuchMethodException("No setter in class " + instanceClass.getName());
    }
  }

  public static class PropertyMethod {
    final String propertyName;
    final Method readMethod;
    final Method writeMethod;

    public PropertyMethod(String propertyName, Method readMethod, Method writeMethod) {
      this.propertyName = propertyName;
      this.readMethod = readMethod;
      this.writeMethod = writeMethod;
    }

    public String getPropertyName() {
      return propertyName;
    }

    public Method getReadMethod() {
      return readMethod;
    }

    public Method getWriteMethod() {
      return writeMethod;
    }

  }
}
