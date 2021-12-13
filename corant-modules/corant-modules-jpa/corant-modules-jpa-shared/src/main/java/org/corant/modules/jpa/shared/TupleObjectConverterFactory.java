/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.jpa.shared;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Objects.setAccessible;
import static org.corant.shared.util.Primitives.isSimpleClass;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;
import org.corant.shared.conversion.Converter;
import org.corant.shared.conversion.ConverterFactory;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Priorities;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.Classes;
import org.corant.shared.util.Fields;
import org.corant.shared.util.Methods;
import org.corant.shared.util.Systems;

/**
 * corant-modules-ddd-shared
 *
 * @author bingo 上午10:31:41
 *
 */
public class TupleObjectConverterFactory implements ConverterFactory<Tuple, Object> {

  static final int priority = Systems.getProperty("corant.jpa.tuple-object-converter.priority",
      Integer.class, Priorities.FRAMEWORK_LOWER + 1);
  static final int maxSize =
      Systems.getProperty("corant.jpa.tuple-object-converter.cache-type-size", Integer.class, 1024);
  static final ConcurrentHashMap<Class<?>, Injector> map = new ConcurrentHashMap<>(512, 0.8f, 4);

  @Override
  public Converter<Tuple, Object> create(Class<Object> targetClass, Object defaultValue,
      boolean throwException) {
    final Injector injector = shouldNotNull(computeIfAbsent(targetClass));
    return (t, h) -> injector.injectAndGet(t);
  }

  @Override
  public int getPriority() {
    return priority;
  }

  @Override
  public boolean isSupportSourceClass(Class<?> sourceClass) {
    return Tuple.class.isAssignableFrom(sourceClass);
  }

  @Override
  public boolean isSupportTargetClass(Class<?> targetClass) {
    return computeIfAbsent(targetClass) != null;
  }

  Injector computeIfAbsent(Class<?> key) {
    if (key != null) {
      if (Map.class.isAssignableFrom(key)) {
        return MapInjector.INSTANCE;
      }
      if (map.size() >= maxSize) {
        synchronized (this) {
          if (map.size() >= maxSize) {
            map.clear();
          }
        }
      }
      Injector injector = map.computeIfAbsent(key, PojoInjector::new);
      if (injector.isUsable()) {
        return injector;
      } else {
        map.remove(key);
      }
    }
    return null;
  }

  /**
   * corant-modules-ddd-shared
   *
   * @author bingo 下午4:22:04
   *
   */
  interface Injector {

    Object injectAndGet(Tuple tuple);

    boolean isUsable();
  }

  /**
   * corant-modules-ddd-shared
   *
   * @author bingo 下午4:21:58
   *
   */
  static class MapInjector implements Injector {

    static final MapInjector INSTANCE = new MapInjector();

    @Override
    public Object injectAndGet(Tuple tuple) {
      if (tuple == null) {
        return null;
      }
      Map<String, Object> instance = new LinkedHashMap<>(tuple.getElements().size());
      for (TupleElement<?> e : tuple.getElements()) {
        String name = e.getAlias();
        Object value = tuple.get(name);
        instance.put(name, value);
      }
      return instance;
    }

    @Override
    public boolean isUsable() {
      return true;
    }
  }

  /**
   * corant-modules-ddd-shared
   *
   * @author bingo 下午4:21:54
   *
   */
  static class PojoInjector implements Injector {

    final Class<?> pojoClass;
    final Constructor<?> constructor;
    final Map<String, Pair<Method, Class<?>[]>> propertySetters = new HashMap<>();

    public PojoInjector(Class<?> pojoClass) {
      this.pojoClass = shouldNotNull(pojoClass);
      constructor = lookupConstructor(pojoClass);
      if (constructor != null) {
        setAccessible(constructor, true);
        List<Field> fields = Fields.getAllFields(pojoClass);
        for (Method method : pojoClass.getMethods()) {
          setAccessible(method, true);
          String name = method.getName();
          Class<?>[] parameterTypes = method.getParameterTypes();
          if (parameterTypes.length == 1) {
            if (Methods.isSetter(method)) { // for common setter
              String useName = name.substring(3);
              useName = useName.substring(0, 1).toLowerCase(Locale.ROOT) + useName.substring(1);
              propertySetters.put(useName, Pair.of(method, parameterTypes));
            } else { // for fluent setter
              for (Field field : fields) {
                if (field.getGenericType() instanceof Class && field.getName().equals(name)
                    && Methods.isParameterTypesMatching(
                        new Class[] {(Class<?>) field.getGenericType()}, parameterTypes, true)) {
                  propertySetters.put(name, Pair.of(method, parameterTypes));
                }
              }
            }
          } else if (parameterTypes.length == 2 && "setProperty".equals(name)
              && parameterTypes[0].equals(String.class) && isSimpleClass(parameterTypes[1])) {
            propertySetters.put("Property", Pair.of(method, parameterTypes));
          }
        }
      }
    }

    @Override
    public Object injectAndGet(Tuple tuple) {
      if (tuple != null) {
        try {
          Object instance = constructor.newInstance();
          for (TupleElement<?> e : tuple.getElements()) {
            String name = e.getAlias();
            Object value = tuple.get(name);
            if (propertySetters.containsKey(name)) {
              Pair<Method, Class<?>[]> methods = propertySetters.get(name);
              Method method = methods.key();
              method.invoke(instance, toObject(value, methods.value()[0]));
            } else if (propertySetters.containsKey("Property")) {
              Method method = pojoClass.getMethod("setProperty", String.class, value.getClass());
              method.invoke(instance, name, value);
            }
          }
          return instance;
        } catch (IllegalAccessException | InstantiationException | IllegalArgumentException
            | InvocationTargetException | NoSuchMethodException | SecurityException e) {
          throw new CorantRuntimeException(e);
        }
      }
      return null;
    }

    @Override
    public boolean isUsable() {
      return !propertySetters.isEmpty();
    }

    Constructor<?> lookupConstructor(Class<?> pojoClass) {
      try {
        return Classes.getDeclaredConstructor(pojoClass);
      } catch (Exception e) {
        // Noop!
      }
      return null;
    }

  }
}
