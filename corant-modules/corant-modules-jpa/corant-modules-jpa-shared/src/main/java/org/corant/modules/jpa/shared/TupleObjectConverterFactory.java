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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import org.corant.shared.conversion.Converter;
import org.corant.shared.conversion.ConverterFactory;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Priorities;
import org.corant.shared.ubiquity.PropertyAccessor;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.Classes;
import org.corant.shared.util.Systems;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * corant-modules-ddd-shared
 *
 * @author bingo 上午10:31:41
 */
public class TupleObjectConverterFactory implements ConverterFactory<Tuple, Object> {

  static final int priority = Systems.getProperty("corant.jpa.tuple-object-converter.priority",
      Integer.class, Priorities.FRAMEWORK_LOWER + 1);
  static final int maxSize =
      Systems.getProperty("corant.jpa.tuple-object-converter.cache-type-size", Integer.class, 1024);

  // static final ConcurrentHashMap<Class<?>, Injector> map = new ConcurrentHashMap<>(512, 0.8f, 4);
  static Cache<Class<?>, Injector> map = Caffeine.newBuilder().maximumSize(maxSize).build();

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
      Injector injector = map.get(key, PojoInjector::new);
      if (injector.isUsable()) {
        return injector;
      } else {
        map.invalidate(key);
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
   */
  static class PojoInjector implements Injector {

    final Class<?> pojoClass;
    final Constructor<?> constructor;
    final PropertyAccessor propertyAccessor;
    final boolean recordClass;
    final List<Pair<String, Class<?>>> recordComponents;

    public PojoInjector(Class<?> pojoClass) {
      this.pojoClass = shouldNotNull(pojoClass);
      if (pojoClass.isRecord()) {
        recordClass = true;
        recordComponents = new ArrayList<>(pojoClass.getRecordComponents().length);
        for (RecordComponent rc : pojoClass.getRecordComponents()) {
          recordComponents.add(Pair.of(rc.getName(), rc.getType()));
        }
        constructor = lookupConstructor(pojoClass,
            recordComponents.stream().map(Pair::right).toArray(Class<?>[]::new));
        propertyAccessor = null;
      } else {
        recordClass = false;
        recordComponents = null;
        constructor = lookupConstructor(pojoClass);
        if (constructor != null) {
          propertyAccessor = new PropertyAccessor(pojoClass, true, true);
        } else {
          propertyAccessor = null;
        }
      }
    }

    @Override
    public Object injectAndGet(Tuple tuple) {
      if (tuple != null) {
        try {
          if (recordClass) {
            return constructor.newInstance(recordComponents.stream().map(rc -> {
              for (TupleElement<?> e : tuple.getElements()) {
                if (rc.left().equals(e.getAlias())) {
                  return toObject(tuple.get(e.getAlias()), rc.right());
                }
              }
              return null;
            }).toArray());
          } else {
            Object instance = constructor.newInstance();
            for (TupleElement<?> e : tuple.getElements()) {
              String name = e.getAlias();
              Object value = tuple.get(name);
              propertyAccessor.inject(instance, name, value);
            }
            return instance;
          }
        } catch (IllegalAccessException | InstantiationException | IllegalArgumentException
            | InvocationTargetException | NoSuchMethodException | SecurityException e) {
          throw new CorantRuntimeException(e);
        }
      }
      return null;
    }

    @Override
    public boolean isUsable() {
      return constructor != null
          && ((recordClass && recordComponents != null) || propertyAccessor != null);
    }

    Constructor<?> lookupConstructor(Class<?> pojoClass, Class<?>... parameterTypes) {
      try {
        return Classes.getDeclaredConstructor(pojoClass, parameterTypes);
      } catch (Exception e) {
        // Noop!
      }
      return null;
    }

  }
}
