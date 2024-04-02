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
package org.corant.modules.json.expression.function;

import static org.corant.shared.normal.Names.DOMAIN_SPACE_SEPARATORS;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.split;
import static org.corant.shared.util.Strings.trim;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.corant.modules.json.Jsons;
import org.corant.modules.json.expression.FunctionResolver;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Defaults;
import org.corant.shared.normal.Names;
import org.corant.shared.ubiquity.Experimental;
import org.corant.shared.ubiquity.Tuple.Triple;
import org.corant.shared.util.Classes;
import org.corant.shared.util.Configurations;
import org.corant.shared.util.Conversions;
import org.corant.shared.util.Empties;
import org.corant.shared.util.Fields;
import org.corant.shared.util.Iterables;
import org.corant.shared.util.Lists;
import org.corant.shared.util.Maps;
import org.corant.shared.util.Methods;
import org.corant.shared.util.Primitives;
import org.corant.shared.util.Randoms;
import org.corant.shared.util.Sets;
import org.corant.shared.util.Streams;
import org.corant.shared.util.Strings;
import org.corant.shared.util.Systems;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * corant-modules-json
 *
 * <p>
 * An experimental simple object call function resolver using object methods or constructors,
 * typically used to perform method calls on objects or static method calls of object types or call
 * object constructors.
 *
 * <p>
 * The string formed by the complete object type name and method name with {@code ::} or {@code :}
 * symbolic connection is used as the key of the function, if the method name is {@code ::new} it
 * means it is a constructor, {@code ::} means invoke static method {@code :} means invoke
 * non-static method. An array is used to represent the input parameters of the invocation. An empty
 * array [] is used to represent a method or constructor invocations without input parameters; if it
 * is a non-static method invocation, the first element of the input parameter array is the invoked
 * object itself, on the contrary, all arrays are input parameters.
 *
 * <p>
 * NOTE: Since the methods of java objects can be overloaded, for example, multiple method
 * signatures with the same method name and the same number of parameters but different types of
 * parameters are allowed, when this happens, if the parameter value is null, the null parameter
 * type treated as java.lang.Object. When matching methods, non-static methods take precedence.
 *
 * <p>
 * <b>Examples:</b>
 *
 * <pre>
 * <b>Expression</b>                                                   <b>Evaluated result</b>
 * {"#java.lang.String:trim":["abc "]}                                 abc
 * {"#java.lang.String:indexOf":["abc", "b"]}                          1
 * {"#java.lang.String:substring":["bingo.chen", 0, 7]}                bingo.c
 * {"#java.lang.System::currentTimeMillis":[]}                         1667269845452
 * {"#java.util.Date::new":[]}                                         Tue Nov 01 10:30:45 CST 2021
 * {"#java.util.UUID:toString":[{"#java.util.UUID::randomUUID":[]}]}   2f6e8962-2f74-4e24-b039-e2b7c8523605
 * </pre>
 *
 * @author bingo 下午3:29:41
 */
@Experimental
public class DefaultObjectFunctionResolver implements FunctionResolver {

  protected static final Object[] emptyParams = {};
  protected static final Class<?>[] emptyParamTypes = new Class[0];
  protected static final String NEW = "new";
  protected static final String nonStaticDelimiter = DOMAIN_SPACE_SEPARATORS;
  protected static final String staticDelimiter = nonStaticDelimiter + nonStaticDelimiter;

  // 0: constructor, 1: static method, 2: non-static method, 3: static field, 4:non-static field
  protected static final Cache<String, Triple<Class<?>, String, Integer>> holderCache =
      Caffeine.newBuilder().maximumSize(2048).build();
  protected static final Map<String, Triple<Class<?>, String, Integer>> holder =
      holderCache.asMap();
  protected static Map<String, Class<?>> builtinClassAlias = new ConcurrentHashMap<>();
  protected static Cache<MethodInvokeCacheKey, Method> methods =
      Caffeine.newBuilder().maximumSize(2048).build();
  protected static Cache<FieldInvokeCacheKey, Field> fields =
      Caffeine.newBuilder().maximumSize(2048).build();

  static {
    builtinClassAlias.put(Strings.class.getSimpleName(), Strings.class);
    builtinClassAlias.put(Maps.class.getSimpleName(), Maps.class);
    builtinClassAlias.put(Lists.class.getSimpleName(), Lists.class);
    builtinClassAlias.put(Sets.class.getSimpleName(), Sets.class);
    builtinClassAlias.put(Streams.class.getSimpleName(), Streams.class);
    builtinClassAlias.put(Classes.class.getSimpleName(), Classes.class);
    builtinClassAlias.put(Conversions.class.getSimpleName(), Conversions.class);
    builtinClassAlias.put(Objects.class.getSimpleName(), Objects.class);
    builtinClassAlias.put(Systems.class.getSimpleName(), Systems.class);
    builtinClassAlias.put(Randoms.class.getSimpleName(), Randoms.class);
    builtinClassAlias.put(Iterables.class.getSimpleName(), Iterables.class);
    builtinClassAlias.put(Names.class.getSimpleName(), Names.class);
    builtinClassAlias.put(Defaults.class.getSimpleName(), Defaults.class);
    builtinClassAlias.put(Configurations.class.getSimpleName(), Configurations.class);
    builtinClassAlias.put(Empties.class.getSimpleName(), Empties.class);
    builtinClassAlias.put(Jsons.class.getSimpleName(), Jsons.class);
    // java SDK
    Primitives.PRIMITIVE_WRAPPER_MAP.values()
        .forEach(c -> builtinClassAlias.put(c.getSimpleName(), c));
    builtinClassAlias.put(System.class.getSimpleName(), System.class);
    builtinClassAlias.put(String.class.getSimpleName(), String.class);
    builtinClassAlias.put(StringBuilder.class.getSimpleName(), StringBuilder.class);
    builtinClassAlias.put(StringBuffer.class.getSimpleName(), StringBuffer.class);
    builtinClassAlias.put(Instant.class.getSimpleName(), Instant.class);
    builtinClassAlias.put(Date.class.getSimpleName(), Date.class);
    builtinClassAlias.put(Duration.class.getSimpleName(), Duration.class);
    builtinClassAlias.put(ZonedDateTime.class.getSimpleName(), ZonedDateTime.class);
    builtinClassAlias.put(LocalDate.class.getSimpleName(), LocalDate.class);
    builtinClassAlias.put(Arrays.class.getSimpleName(), Arrays.class);
    builtinClassAlias.put(Base64.class.getSimpleName(), Base64.class);
    builtinClassAlias.put(Collections.class.getSimpleName(), Collections.class);
    builtinClassAlias.put(Map.class.getSimpleName(), Map.class);
    builtinClassAlias.put(HashMap.class.getSimpleName(), HashMap.class);
    builtinClassAlias.put(LinkedHashMap.class.getSimpleName(), LinkedHashMap.class);
    builtinClassAlias.put(Collection.class.getSimpleName(), Collection.class);
    builtinClassAlias.put(List.class.getSimpleName(), List.class);
    builtinClassAlias.put(ArrayList.class.getSimpleName(), ArrayList.class);
    builtinClassAlias.put(Set.class.getSimpleName(), Set.class);
    builtinClassAlias.put(HashSet.class.getSimpleName(), HashSet.class);
    builtinClassAlias.put(LinkedHashSet.class.getSimpleName(), LinkedHashSet.class);
    builtinClassAlias.put(UUID.class.getSimpleName(), UUID.class);
    builtinClassAlias.put(Optional.class.getSimpleName(), Optional.class);
    builtinClassAlias.put(Currency.class.getSimpleName(), Currency.class);
  }

  @Override
  public Function<Object[], Object> resolve(String name) {
    return fs -> {
      Triple<Class<?>, String, Integer> sign = holder.get(name);
      if (sign == null) {
        throw new IllegalArgumentException(String.format("Unsupported function: %s", name));
      }
      Class<?> cls = sign.first();
      String invokerName = sign.second();
      Integer type = sign.third();
      //// FIXME Currently treated parameter type as java.lang.Object if parameter is null
      if (type == 0) {
        // constructor invoking
        return invokeConstructor(fs, cls);
      } else if (type == 1) {
        // static method invoking
        return invokeStaticMethod(fs, cls, invokerName);
      } else if (type == 2) {
        // non-static method invoking
        return invokeNonStaticMethod(fs, cls, invokerName);
      } else if (type == 3) {
        // static field invoking
        return invokeStaticField(fs, cls, invokerName);
      } else {
        // non-static field invoking
        return invokeNonStaticField(fs, cls, invokerName);
      }
    };
  }

  @Override
  public boolean supports(String name) {
    String useName = trim(name);
    if (useName != null) {
      if (holder.containsKey(useName)) {
        return true;
      }
      if (useName.contains(nonStaticDelimiter)) {
        // method or constructor
        String[] tmp = split(useName, nonStaticDelimiter, true, true);
        if (tmp.length == 2) {
          boolean statics = useName.contains(staticDelimiter);
          Class<?> klass = builtinClassAlias.getOrDefault(tmp[0], Classes.tryAsClass(tmp[0]));
          if (klass != null) {
            // constructor class name::new
            if (NEW.equals(tmp[1]) && statics) {
              holder.computeIfAbsent(useName, k1 -> Triple.of(klass, NEW, 0));
              return true;
            }
            // method class name:method name
            if (Arrays.stream(klass.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals(tmp[1]))) {
              holder.computeIfAbsent(useName, k1 -> Triple.of(klass, tmp[1], statics ? 1 : 2));
              return true;
            }
          }
        }
      } else {
        // field
        int last = useName.lastIndexOf(".");
        if (last > 0 && last < useName.length() - 1) {
          String fieldName = useName.substring(last + 1);
          if (isNotBlank(fieldName)) {
            String className = useName.substring(0, last);
            Class<?> klass =
                builtinClassAlias.getOrDefault(className, Classes.tryAsClass(className));
            try {
              if (klass != null) {
                Field field = klass.getDeclaredField(fieldName);
                holder.computeIfAbsent(useName, k1 -> Triple.of(klass, fieldName,
                    Modifier.isStatic(field.getModifiers()) ? 3 : 4));
                return true;
              }
            } catch (NoSuchFieldException | SecurityException e) {
              // No op
            }
          }
        }
      }
    }
    return false;
  }

  protected Field getMatchingField(Class<?> clazz, String fieldName) {
    // return Fields.getField(clazz, fieldName);
    return fields.get(new FieldInvokeCacheKey(clazz, fieldName),
        c -> Fields.getField(c.clazz, c.fieldName));
  }

  protected Method getMatchingMethod(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
    // return Methods.getMatchingMethod(clazz, methodName, parameterTypes);
    return methods.get(new MethodInvokeCacheKey(clazz, methodName, parameterTypes),
        c -> Methods.getMatchingMethod(c.clazz, c.methodName, c.parameterTypes));
  }

  protected Object invokeConstructor(Object[] fs, Class<?> cls) {
    Class<?>[] paramTypes = emptyParamTypes;
    int length = fs.length;
    if (length > 0) {
      paramTypes = new Class[length];
      for (int i = 0; i < length; i++) {
        paramTypes[i] = fs[i] == null ? Object.class : fs[i].getClass();
      }
    }
    try {
      Constructor<?> constructor = Classes.getDeclaredConstructor(cls, paramTypes);
      return constructor.newInstance(fs);
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException | NoSuchMethodException e) {
      throw new IllegalArgumentException(String.format("Unsupported function: %s#%s(%s)", cls, NEW,
          Strings.join(",", (Object[]) paramTypes)));
    }
  }

  protected Object invokeNonStaticMethod(Object[] fs, Class<?> cls, String methodName) {
    int length = fs.length;
    Class<?>[] paramTypes = emptyParamTypes;
    if (length > 0) {
      // match non-static method
      Object invokedObject = fs[0];
      if (invokedObject != null && cls.isAssignableFrom(Classes.getUserClass(invokedObject))) {
        Object[] params = new Object[length - 1];
        paramTypes = new Class[length - 1];
        for (int i = 1; i < length; i++) {
          params[i - 1] = fs[i];
          paramTypes[i - 1] = fs[i] == null ? Object.class : fs[i].getClass();
        }
        Method method = getMatchingMethod(cls, methodName, paramTypes);
        if (method != null && !Modifier.isStatic(method.getModifiers())) {
          try {
            return Methods.invokeMethod(invokedObject, method, params);
          } catch (IllegalAccessException | IllegalArgumentException
              | InvocationTargetException e) {
            throw new CorantRuntimeException(e);
          }
        }
      }
    }
    throw new IllegalArgumentException(String.format("Unsupported function: %s#%s(%s)", cls,
        methodName, Strings.join(",", (Object[]) paramTypes)));
  }

  protected Object invokeStaticField(Object[] fs, Class<?> cls, String invokerName) {
    if (fs.length != 0) {
      throw new IllegalArgumentException(String.format(
          "Unsupported function: %s.%s can't accept any parameters", cls.getName(), invokerName));
    }
    try {
      return Fields.getStaticFieldValue(getMatchingField(cls, invokerName));
    } catch (SecurityException e) {
      throw new CorantRuntimeException(e);
    }
  }

  protected Object invokeStaticMethod(Object[] fs, Class<?> cls, String methodName) {
    int length = fs.length;
    Class<?>[] paramTypes = new Class[length];
    for (int i = 0; i < length; i++) {
      paramTypes[i] = fs[i] == null ? Object.class : fs[i].getClass();
    }
    Method method = getMatchingMethod(cls, methodName, paramTypes);
    if (method != null) {
      try {
        return Methods.invokeMethod(null, method, fs);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        throw new CorantRuntimeException(e);
      }
    }
    throw new IllegalArgumentException(String.format("Unsupported function: %s#%s(%s)", cls,
        methodName, Strings.join(",", (Object[]) paramTypes)));
  }

  private Object invokeNonStaticField(Object[] fs, Class<?> cls, String invokerName) {
    if (fs.length == 1 && fs[0] != null && cls.isAssignableFrom(Classes.getUserClass(fs[0]))) {
      return Fields.getFieldValue(getMatchingField(cls, invokerName), fs[0]);
    }
    throw new IllegalArgumentException(
        String.format("Unsupported function: %s.%s must accept an instance as parameter",
            cls.getName(), invokerName));
  }

  protected static class FieldInvokeCacheKey {
    final Class<?> clazz;
    final String fieldName;
    final int hash;

    FieldInvokeCacheKey(Class<?> clazz, String fieldName) {
      this.clazz = clazz;
      this.fieldName = fieldName;
      hash = Objects.hash(this.clazz, this.fieldName);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      FieldInvokeCacheKey other = (FieldInvokeCacheKey) obj;
      return Objects.equals(clazz, other.clazz) && Objects.equals(fieldName, other.fieldName);
    }

    @Override
    public int hashCode() {
      return hash;
    }

  }

  protected static class MethodInvokeCacheKey {
    final Class<?>[] parameterTypes;
    final Class<?> clazz;
    final String methodName;
    final int hash;

    MethodInvokeCacheKey(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
      this.clazz = clazz;
      this.methodName = methodName;
      this.parameterTypes = Arrays.copyOf(parameterTypes, parameterTypes.length);
      hash = calHash(this.clazz, this.methodName, this.parameterTypes);
    }

    static int calHash(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
      final int prime = 31;
      int result = 1;
      result = prime * result + Arrays.hashCode(parameterTypes);
      return prime * result + Objects.hash(clazz, methodName);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      MethodInvokeCacheKey other = (MethodInvokeCacheKey) obj;
      return Objects.equals(clazz, other.clazz) && Objects.equals(methodName, other.methodName)
          && Arrays.equals(parameterTypes, other.parameterTypes);
    }

    @Override
    public int hashCode() {
      return hash;
    }

  }
}
