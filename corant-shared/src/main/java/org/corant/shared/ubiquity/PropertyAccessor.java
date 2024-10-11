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
package org.corant.shared.ubiquity;

import static java.lang.String.format;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Maps.immutableMap;
import static org.corant.shared.util.Objects.forceCast;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.corant.shared.conversion.Conversion;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Fields;
import org.corant.shared.util.Sets;

/**
 * corant-shared
 *
 * @author bingo 下午2:53:54
 */
public class PropertyAccessor {

  protected final boolean supportNamedProperty;
  protected final Class<?> instanceClass;
  protected final Map<String, Method> propertySetters;
  protected final Map<String, Method> propertyGetters;

  public PropertyAccessor(Class<?> instanceClass) {
    this(instanceClass, false);
  }

  public PropertyAccessor(Class<?> instanceClass, boolean supportNamedProperty) {
    this(instanceClass, supportNamedProperty, false);
  }

  public PropertyAccessor(Class<?> instanceClass, boolean supportNamedProperty,
      boolean supportFluent) {
    this.instanceClass = getUserClass(shouldNotNull(instanceClass, "The class can't null"));
    this.supportNamedProperty = supportNamedProperty;

    Map<String, Method> setters = new LinkedHashMap<>();
    Map<String, Method> getters = new LinkedHashMap<>();
    if (instanceClass.isRecord()) {
      for (RecordComponent rc : instanceClass.getRecordComponents()) {
        getters.put(rc.getName(), rc.getAccessor());
      }
    } else {
      for (Method method : instanceClass.getMethods()) {
        if (method.isBridge() || Modifier.isStatic(method.getModifiers())) {
          // TODO consider the bridge method "change" visibility of base class's methods ??
          continue;
        }
        Class<?> returnType = method.getReturnType();
        String name = method.getName();
        switch (method.getParameterCount()) {
          case 0:
            if (returnType.equals(boolean.class) && isPrefix(name, "is")) {
              getters.put(decapitalize(name.substring(2)), method);
            } else if (!returnType.equals(void.class) && isPrefix(name, "get")) {
              getters.put(decapitalize(name.substring(3)), method);
            } else if (supportFluent && !returnType.equals(void.class)) {
              Field field = Fields.getField(instanceClass, name);
              if (field != null && field.getType().equals(returnType)
                  && !getters.containsKey(name)) {
                // FIXME check field type & method return type
                getters.put(name, method);
              }
            }
            break;
          case 1:
            if (isPrefix(name, "set")) {
              setters.put(decapitalize(name.substring(3)), method);
            } else if (supportNamedProperty && !returnType.equals(void.class)
                && method.getParameterTypes()[0].equals(String.class)
                && "getProperty".equals(name)) {
              getters.put("Property", method);
            } else if (supportFluent
                && (returnType.equals(instanceClass) || returnType.equals(void.class))) {
              Field field = Fields.getField(instanceClass, name);
              if (field != null && field.getType().equals(method.getParameterTypes()[0])
                  && !setters.containsKey(name)) {
                // FIXME check field type & method parameter type
                setters.put(name, method);
              }
            }
            break;
          case 2:
            if (supportNamedProperty && method.getParameterTypes()[0].equals(String.class)
                && "setProperty".equals(name)) {
              setters.put("Property", method);
            }
            break;
          default:
            break;
        }
      }
    }
    propertySetters = Collections.unmodifiableMap(setters);
    propertyGetters = Collections.unmodifiableMap(getters);
  }

  protected PropertyAccessor(boolean supportNamedProperty, Class<?> instanceClass,
      Map<String, Method> propertySetters, Map<String, Method> propertyGetters) {
    this.supportNamedProperty = supportNamedProperty;
    this.instanceClass = shouldNotNull(instanceClass, "The class can't null");
    this.propertySetters = immutableMap(propertySetters);
    this.propertyGetters = immutableMap(propertyGetters);
  }

  public static PropertyAccessor introspect(Class<?> beanClass) {
    Class<?> clazz = getUserClass(shouldNotNull(beanClass, "The class can't null"));
    try {
      Map<String, Method> setters = new HashMap<>();
      Map<String, Method> getters = new HashMap<>();
      for (PropertyDescriptor desc : Introspector.getBeanInfo(clazz).getPropertyDescriptors()) {
        if (desc.getWriteMethod() != null) {
          setters.put(desc.getName(), desc.getWriteMethod());
        }
        if (desc.getReadMethod() != null) {
          getters.put(desc.getName(), desc.getReadMethod());
        }
      }
      return new PropertyAccessor(false, clazz, setters, getters);
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static <R> R newRecord(Class<R> recordClass, Map<String, ?> properties) throws Exception {
    RecordComponent[] components =
        shouldNotNull(recordClass.getRecordComponents(), "Class must be a record class");
    Constructor<R> constructor = recordClass.getDeclaredConstructor(
        Arrays.stream(components).map(RecordComponent::getType).toArray(Class<?>[]::new));
    Object[] parameters;
    if (isNotEmpty(properties)) {
      parameters = Arrays.stream(components)
          .map(r -> toObject(properties.get(r.getName()), r.getGenericType())).toArray();
    } else {
      parameters = Arrays.stream(components).map(r -> {
        if (r.getType().isPrimitive()) {
          if (r.getType() == Boolean.TYPE) {
            return false;
          } else if (r.getType() == Long.TYPE || r.getType() == Integer.TYPE
              || r.getType() == Short.TYPE || r.getType() == Byte.TYPE) {
            return 0;
          } else if (r.getType() == Double.TYPE) {
            return 0d;
          } else if (r.getType() == Float.TYPE) {
            return 0f;
          }
        }
        return null;
      }).toArray();
    }
    return constructor.newInstance(parameters);
  }

  public static <R> R tryNewRecord(Class<R> recordClass, Map<String, ?> properties) {
    try {
      return newRecord(recordClass, properties);
    } catch (Exception ex) {
      return null;
    }
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

  protected static boolean isPrefix(String name, String prefix) {
    return name.length() > prefix.length() && name.startsWith(prefix);
  }

  public Class<?> getInstanceClass() {
    return instanceClass;
  }

  public Map<String, Method> getPropertyGetters() {
    return propertyGetters;
  }

  public List<PropertyMetadata> getPropertyMetadata() {
    return Sets.union(propertyGetters.keySet(), propertySetters.keySet()).stream()
        .map(p -> new PropertyMetadata(p, propertyGetters.get(p), propertySetters.get(p))).toList();
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
    inject(instance, propertyName, propertyValue, Conversion::convertType);
  }

  public void inject(Object instance, String propertyName, Object propertyValue,
      BiFunction<Object, Type, Object> converter)
      throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
    String realName = decapitalize(propertyName);
    if (propertySetters.containsKey(realName)) {
      Method method = propertySetters.get(realName);
      method.invoke(instance, converter.apply(propertyValue, method.getGenericParameterTypes()[0]));
    } else if (supportNamedProperty && propertySetters.containsKey("Property")) {
      propertySetters.get("Property").invoke(instance, propertyName, propertyValue);
    } else {
      throw new NoSuchMethodException(format("No setter for property [%s] in class [%s]",
          propertyName, instanceClass.getName()));
    }
  }

  public boolean isSupportNamedProperty() {
    return supportNamedProperty;
  }

  public <T> T obtain(Object instance, String propertyName)
      throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
      NoSuchMethodException, SecurityException {
    String realName = decapitalize(propertyName);
    if (propertyGetters.containsKey(realName)) {
      Method method = propertyGetters.get(realName);
      return forceCast(method.invoke(instance));
    } else if (supportNamedProperty && propertyGetters.containsKey("Property")) {
      return forceCast(propertyGetters.get("Property").invoke(instance, propertyName));
    } else {
      throw new NoSuchMethodException(format("No getter for property [%s] in class [%s]",
          propertyName, instanceClass.getName()));
    }
  }

  public void tryInject(Object instance, String propertyName, Object propertyValue) {
    try {
      inject(instance, propertyName, propertyValue);
    } catch (Exception ex) {
      // Noop
    }
  }

  public <T> T tryObtain(Object instance, String propertyName) {
    try {
      return obtain(instance, propertyName);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * corant-shared
   *
   * @author bingo 13:58:55
   */
  public static class PropertyMetadata {
    final String name;
    final Method readMethod;
    final Method writeMethod;

    public PropertyMetadata(String name, Method readMethod, Method writeMethod) {
      this.name = name;
      this.readMethod = readMethod;
      this.writeMethod = writeMethod;
    }

    public String getName() {
      return name;
    }

    public Method getReadMethod() {
      return readMethod;
    }

    public Method getWriteMethod() {
      return writeMethod;
    }
  }
}
