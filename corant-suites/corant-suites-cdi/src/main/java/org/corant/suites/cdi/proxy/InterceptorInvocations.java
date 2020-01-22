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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.InvocationContext;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-suites-cdi
 *
 * @author bingo 上午11:08:57
 *
 */
public class InterceptorInvocations {

  public static List<Annotation> getInterceptorBindings(Annotation[] annotations,
      BeanManager beanManager) {
    if (annotations.length == 0) {
      return Collections.emptyList();
    }
    List<Annotation> bindings = new ArrayList<>();
    for (Annotation annotation : annotations) {
      if (beanManager.isInterceptorBinding(annotation.annotationType())) {
        bindings.add(annotation);
      }
    }
    return bindings;
  }

  public static Map<Method, List<InterceptorInvocation>> getInterceptorChains(
      BeanManager beanManager, CreationalContext<?> creationalContext, Class<?> interfaceType) {
    Map<Method, List<InterceptorInvocation>> chains = new HashMap<>();
    Map<Interceptor<?>, Object> interceptorInstances = new HashMap<>();
    List<Annotation> classLevelBindings =
        getInterceptorBindings(interfaceType.getAnnotations(), beanManager);

    for (Method method : interfaceType.getMethods()) {
      if (method.isDefault() || Modifier.isStatic(method.getModifiers())) {
        continue;
      }
      List<Annotation> methodLevelBindings =
          getInterceptorBindings(method.getAnnotations(), beanManager);
      if (!classLevelBindings.isEmpty() || !methodLevelBindings.isEmpty()) {
        List<Annotation> interceptorBindings = merge(methodLevelBindings, classLevelBindings);
        List<Interceptor<?>> interceptors =
            beanManager.resolveInterceptors(InterceptionType.AROUND_INVOKE,
                interceptorBindings.toArray(new Annotation[interceptorBindings.size()]));
        if (!interceptors.isEmpty()) {
          List<InterceptorInvocation> chain = new ArrayList<>();
          for (Interceptor<?> interceptor : interceptors) {
            chain.add(new InterceptorInvocation(interceptor,
                interceptorInstances.computeIfAbsent(interceptor,
                    i -> beanManager.getReference(i, i.getBeanClass(), creationalContext))));
          }
          chains.put(method, chain);
        }
      }
    }
    return chains;
  }

  static List<Annotation> merge(List<Annotation> methodLevelBindings,
      List<Annotation> classLevelBindings) {
    Set<Class<? extends Annotation>> types =
        methodLevelBindings.stream().map(Annotation::annotationType).collect(Collectors.toSet());
    List<Annotation> merged = new ArrayList<>(methodLevelBindings);
    for (Annotation annotation : classLevelBindings) {
      if (!types.contains(annotation.annotationType())) {
        merged.add(annotation);
      }
    }
    return merged;
  }

  /**
   * corant-suites-cdi
   *
   * @author bingo 上午10:41:54
   *
   */
  public static class InterceptorInvocation {

    @SuppressWarnings("rawtypes")
    private final Interceptor interceptor;

    private final Object interceptorInstance;

    public InterceptorInvocation(final Interceptor<?> interceptor,
        final Object interceptorInstance) {
      this.interceptor = interceptor;
      this.interceptorInstance = interceptorInstance;
    }

    @SuppressWarnings("unchecked")
    Object invoke(InvocationContext ctx) throws Exception {
      return interceptor.intercept(InterceptionType.AROUND_INVOKE, interceptorInstance, ctx);
    }
  }

  /**
   * corant-suites-cdi
   *
   * @author bingo 上午11:10:02
   *
   */
  public static class InvocationContextImpl implements InvocationContext {

    private final Object target;

    private final Method method;

    private Object[] args;

    private final int position;

    private final Map<String, Object> contextData;

    private final List<InterceptorInvocation> chain;

    /**
     * @param target
     * @param method
     * @param args
     * @param chain
     */
    public InvocationContextImpl(final Object target, final Method method, final Object[] args,
        final List<InterceptorInvocation> chain) {
      this(target, method, args, chain, 0);
    }

    private InvocationContextImpl(final Object target, final Method method, final Object[] args,
        final List<InterceptorInvocation> chain, final int position) {
      this.target = target;
      this.method = method;
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
    public Object[] getParameters() throws IllegalStateException {
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
    public void setParameters(Object[] params)
        throws IllegalStateException, IllegalArgumentException {
      args = params;
    }

    protected Object interceptorChainCompleted()
        throws InvocationTargetException, IllegalAccessException, IllegalArgumentException {
      return method.invoke(target, args);
    }

    protected Object invokeNext() throws Exception {
      return chain.get(position).invoke(nextContext());
    }

    boolean hasNextInterceptor() {
      return position < chain.size();
    }

    private InvocationContext nextContext() {
      return new InvocationContextImpl(target, method, args, chain, position + 1);
    }

  }
}
