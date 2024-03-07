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

import static org.corant.shared.util.Strings.split;
import java.lang.reflect.Constructor;
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
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.Classes;
import org.corant.shared.util.Configurations;
import org.corant.shared.util.Conversions;
import org.corant.shared.util.Empties;
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

/**
 * corant-modules-json
 *
 * <p>
 * An experimental simple object call function resolver using object methods or constructors,
 * typically used to perform method calls on objects or static method calls of object types or call
 * object constructors.
 *
 * <p>
 * The string formed by the complete object type name and method name with :: symbolic connection is
 * used as the key of the function, if the method name is {@code new} it means it is a constructor.
 * An array is used to represent the input parameters of the invocation. An empty array [] is used
 * to represent a method or constructor invocations without input parameters; if it is a non-static
 * method invocation, the first element of the input parameter array is the invoked object itself,
 * on the contrary, all arrays are input parameters.
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
 * <b>Expression</b>                                                    <b>Evaluated result</b>
 * {"#java.lang.String::trim":["abc "]}                                 abc
 * {"#java.lang.String::indexOf":["abc", "b"]}                          1
 * {"#java.lang.String::substring":["bingo.chen", 0, 7]}                bingo.c
 * {"#java.lang.System::currentTimeMillis":[]}                          1667269845452
 * {"#java.util.Date::new":[]}                                          Tue Nov 01 10:30:45 CST 2021
 * {"#java.util.UUID::toString":[{"#java.util.UUID::randomUUID":[]}]}   2f6e8962-2f74-4e24-b039-e2b7c8523605
 * </pre>
 *
 * @author bingo 下午3:29:41
 */
@Experimental
public class DefaultObjectFunctionResolver implements FunctionResolver {

  static final Map<String, Pair<Class<?>, String>> holder = new ConcurrentHashMap<>();
  static final Object[] emptyParams = {};
  static final Class<?>[] emptyParamTypes = new Class[0];
  static final String NEW = "new";
  static final String delimiter = "::";

  protected static Map<String, Class<?>> builtinClassAlias = new ConcurrentHashMap<>();
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
      Pair<Class<?>, String> sign = holder.get(name);
      Class<?> cls = sign.first();
      String invokerName = sign.second();
      //// FIXME Currently treated parameter type as java.lang.Object if parameter is null
      if (NEW.equals(invokerName)) {
        // constructor invoking
        return invokeConstructor(fs, cls);
      } else {
        // method invoking
        return invokeMethod(fs, cls, invokerName);
      }
    };
  }

  @Override
  public boolean supports(String name) {
    if (name != null) {
      if (holder.containsKey(name)) {
        return true;
      }
      if (Strings.contains(name, delimiter)) {
        String[] tmp = split(name, delimiter, true, true);
        if (tmp.length == 2) {
          Class<?> klass = builtinClassAlias.getOrDefault(tmp[0], Classes.tryAsClass(tmp[0]));
          if (klass != null && (Arrays.stream(klass.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals(tmp[1])) || NEW.equals(tmp[1]))) {
            holder.computeIfAbsent(name, x -> Pair.of(klass, tmp[1]));
            return true;
          }
        }
      }
    }
    return false;
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

  protected Object invokeMethod(Object[] fs, Class<?> cls, String methodName) {
    int length = fs.length;
    Object[] params = emptyParams;
    Class<?>[] paramTypes = emptyParamTypes;
    Object invokedObject = null;
    Method method = null;
    if (length > 0) {
      // match non-static method
      invokedObject = fs[0];
      params = new Object[length - 1];
      paramTypes = new Class[length - 1];
      for (int i = 1; i < length; i++) {
        params[i - 1] = fs[i];
        paramTypes[i - 1] = fs[i] == null ? Object.class : fs[i].getClass();
      }
      method = Methods.getMatchingMethod(cls, methodName, paramTypes);
      if (method != null && Modifier.isStatic(method.getModifiers())) {
        method = null;
      }
    }
    if (method == null) {
      // match static method
      invokedObject = null;
      params = fs;
      paramTypes = new Class[length];
      for (int i = 0; i < length; i++) {
        paramTypes[i] = fs[i] == null ? Object.class : fs[i].getClass();
      }
      method = Methods.getMatchingMethod(cls, methodName, paramTypes);
    }
    if (method != null) {
      try {
        return Methods.invokeMethod(invokedObject, method, params);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        throw new CorantRuntimeException(e);
      }
    }
    throw new IllegalArgumentException(String.format("Unsupported function: %s#%s(%s)", cls,
        methodName, Strings.join(",", (Object[]) paramTypes)));
  }

}
