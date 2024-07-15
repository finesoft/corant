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
package org.corant.context.proxy;

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-context
 * <p>
 * A simple invocation handler implementation.
 *
 * @author bingo 下午3:20:16
 */
public class ProxyInvocationHandler implements InvocationHandler {

  protected final Class<?> clazz;
  protected final Map<Method, MethodInvoker> invokers;

  /**
   * Construct a proxy invocation handler by given class and method invoker builder
   *
   * @param clazz the proxy class
   * @param invokerBuilder the proxy method invoker builder, used to build proxy method invoker, if
   *        the method doesn't need a proxy or can't be proxied, then return null.
   */
  public ProxyInvocationHandler(final Class<?> clazz,
      final Function<Method, MethodInvoker> invokerBuilder) {
    this.clazz = shouldNotNull(clazz);
    Map<Method, MethodInvoker> methodInvokers = new HashMap<>();
    for (Method method : clazz.getMethods()) {
      MethodInvoker methodInvoker = invokerBuilder.apply(method);
      if (methodInvoker != null) {
        methodInvokers.put(method, methodInvoker);
      }
    }
    invokers = Collections.unmodifiableMap(methodInvokers);
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
    ProxyInvocationHandler other = (ProxyInvocationHandler) obj;
    if (clazz == null) {
      return other.clazz == null;
    } else {
      return clazz.equals(other.clazz);
    }
  }

  public Class<?> getClazz() {
    return clazz;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    return prime * result + (clazz == null ? 0 : clazz.hashCode());
  }

  @Override
  public Object invoke(Object target, Method method, Object[] args) throws Throwable {
    MethodInvoker methodInvoker = invokers.get(method);
    if (methodInvoker == null) {
      // The default method and java.lang.Object methods use for hq in Collection
      if (method.isDefault()) {
        return ProxyUtils.invokeDefaultMethod(target, method, args);
      } else if ("equals".equals(method.getName()) && method.getParameterTypes()[0] == Object.class
          && args != null && args.length == 1) {
        if (args[0] == null) {
          return false;
        }
        if (target == args[0]) {
          return true;
        }
        return ProxyUtils.isProxyOfSameInterfaces(args[0], clazz)
            && equals(Proxy.getInvocationHandler(args[0]));
      } else if ("hashCode".equals(method.getName()) && (args == null || args.length == 0)) {
        return hashCode();
      } else if ("toString".equals(method.getName()) && (args == null || args.length == 0)) {
        return toString();
      } else {
        throw new CorantRuntimeException("Can not find method %s.", method);
      }
    }
    return methodInvoker.invoke(target, args);

  }

  @Override
  public String toString() {
    return clazz.getName().concat("(corant proxy)");
  }
}
