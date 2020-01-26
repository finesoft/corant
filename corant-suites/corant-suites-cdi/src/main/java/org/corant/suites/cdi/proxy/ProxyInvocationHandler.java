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
package org.corant.suites.cdi.proxy;

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-suites-cdi
 *
 * @author bingo 下午3:20:16
 *
 */
public class ProxyInvocationHandler implements InvocationHandler {

  protected final Class<?> clazz;
  protected final Map<Method, MethodInvoker> invokers;

  public ProxyInvocationHandler(final Class<?> clazz,
      final Function<Method, MethodInvoker> invokerHandler) {
    this.clazz = shouldNotNull(clazz);
    Map<Method, MethodInvoker> methodInvokers = new HashMap<>();
    for (Method method : clazz.getDeclaredMethods()) {
      if (method.isDefault() || Modifier.isStatic(method.getModifiers())) {
        continue;
      }
      methodInvokers.put(method, invokerHandler.apply(method));
    }
    invokers = Collections.unmodifiableMap(methodInvokers);
  }

  /**
   * FIXME UNFINISH YET! NOTE: FOR JAVA 8 ONLY!
   *
   * @param o
   * @param method
   * @param args
   * @return invokeDefaultMethod
   */
  static Object invokeDefaultMethod(Object o, Method method, Object[] args) {
    try {
      Class<?> declaringClass = method.getDeclaringClass();
      Constructor<MethodHandles.Lookup> constructor =
          MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
      constructor.setAccessible(true);
      return constructor.newInstance(declaringClass, MethodHandles.Lookup.PRIVATE)
          .unreflectSpecial(method, declaringClass).bindTo(o).invokeWithArguments(args);
    } catch (Throwable e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ProxyInvocationHandler)) {
      return false;
    }
    ProxyInvocationHandler other = (ProxyInvocationHandler) obj;
    if (other == this) {
      return true;
    }
    if (other.clazz != clazz) {
      return false;
    }
    return super.equals(obj);
  }

  public Class<?> getClazz() {
    return clazz;
  }

  @Override
  public int hashCode() {
    return clazz.hashCode();
  }

  @Override
  public Object invoke(Object o, Method method, Object[] args) throws Throwable {
    MethodInvoker methodInvoker = invokers.get(method);
    if (methodInvoker == null) {
      // The default method and java.lang.Object methods use for hq in Collection
      if (method.isDefault()) {
        return invokeDefaultMethod(o, method, args);
      } else if (method.getName().equals("equals")) {
        return o == args[0];
      } else if (method.getName().equals("hashCode")) {
        return hashCode();
      } else if (method.getName().equals("toString") && (args == null || args.length == 0)) {
        return toString();
      } else {
        throw new CorantRuntimeException("Can not find method %s.", method);
      }
    }
    return methodInvoker.invoke(o, args);

  }

  @Override
  public String toString() {
    return "Corant proxy for ".concat(clazz.getName());
  }

  /**
   * corant-suites-cdi
   *
   * @author bingo 上午10:38:27
   *
   */
  @FunctionalInterface
  public interface MethodInvoker {
    Object invoke(Object target, Object[] args);
  }
}
