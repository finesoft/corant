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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.interceptor.InvocationContext;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-context
 *
 * @author bingo 上午11:10:02
 *
 */
public class InvocationContextImpl implements InvocationContext {

  private final Class<?> targetClass;

  private final Object target;

  private final Method method;

  private Object[] args;

  private final int position;

  private final Map<String, Object> contextData;

  private final List<InterceptorInvocation> chain;

  private final MethodInvoker methodInvoker;

  /**
   * @param targetClass
   * @param target
   * @param method
   * @param methodInvoker
   * @param args
   * @param chain
   */
  public InvocationContextImpl(final Class<?> targetClass, final Object target, final Method method,
      final MethodInvoker methodInvoker, final Object[] args,
      final List<InterceptorInvocation> chain) {
    this(targetClass, target, method, methodInvoker, args, chain, 0);
  }

  /**
   * @param targetClass
   * @param target
   * @param method
   * @param args
   * @param chain
   */
  public InvocationContextImpl(final Class<?> targetClass, final Object target, final Method method,
      final Object[] args, final List<InterceptorInvocation> chain) {
    this(targetClass, target, method, null, args, chain, 0);
  }

  private InvocationContextImpl(final Class<?> targetClass, final Object target,
      final Method method, final MethodInvoker methodInvoker, final Object[] args,
      final List<InterceptorInvocation> chain, final int position) {
    this.targetClass = targetClass;
    this.target = target;
    this.method = method;
    this.methodInvoker = methodInvoker;
    this.args = args;
    contextData = new HashMap<>();
    this.position = position;
    this.chain = chain;
  }

  @Override
  public Constructor<?> getConstructor() {
    return null;
  }

  @Override
  public Map<String, Object> getContextData() {
    return contextData;
  }

  @Override
  public Method getMethod() {
    return method;
  }

  @Override
  public Object[] getParameters() {
    return args;
  }

  @Override
  public Object getTarget() {
    return target;
  }

  @Override
  public Object getTimer() {
    return null;
  }

  @Override
  public Object proceed() throws Exception {
    try {
      if (hasNextInterceptor()) {
        return invokeNext();
      } else {
        return interceptorChainCompleted();
      }
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof Error) {
        throw (Error) cause;
      }
      if (cause instanceof Exception) {
        throw (Exception) cause;
      }
      throw new CorantRuntimeException(cause);
    }
  }

  @Override
  public void setParameters(Object[] params) {
    args = params;
  }

  protected Object interceptorChainCompleted() {
    if (methodInvoker != null) {
      return methodInvoker.invoke(target, args);
    } else if (method.isDefault()) {
      return ProxyUtils.invokeDefaultMethod(target, method, args);
    } else if ("equals".equals(method.getName()) && method.getParameterTypes()[0] == Object.class
        && args != null && args.length == 1) {
      if (args[0] == null) {
        return false;
      }
      return target == args[0];
    } else if ("hashCode".equals(method.getName()) && (args == null || args.length == 0)) {
      return hashCode();
    } else if ("toString".equals(method.getName()) && (args == null || args.length == 0)) {
      return "Corant proxy for ".concat(targetClass.getName());
    } else {
      throw new CorantRuntimeException("Can not find method %s.", method);
    }
  }

  protected Object invokeNext() throws Exception {
    return chain.get(position).invoke(nextContext());
  }

  boolean hasNextInterceptor() {
    return position < chain.size();
  }

  private InvocationContext nextContext() {
    return new InvocationContextImpl(targetClass, target, method, methodInvoker, args, chain,
        position + 1);
  }

}
