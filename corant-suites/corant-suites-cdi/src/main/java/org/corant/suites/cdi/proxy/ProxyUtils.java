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
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-suites-cdi
 *
 * @author bingo 下午10:09:48
 *
 */
public class ProxyUtils {

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
      if ( /* method.isDefault() || */ Modifier.isStatic(method.getModifiers())) {
        continue; // FIXME for now I am not sure yet, the default method interceptor?
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

  /**
   * FIXME UNFINISH YET! NOTE: FOR JAVA 8 ONLY!
   *
   * @param o
   * @param method
   * @param args
   * @return invokeDefaultMethod
   */
  public static Object invokeDefaultMethod(Object o, Method method, Object[] args) {
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

  public static boolean isProxyOfSameInterfaces(Object arg, Class<?> proxyClass) {
    return proxyClass.isInstance(arg) || Proxy.isProxyClass(arg.getClass())
        && Arrays.equals(arg.getClass().getInterfaces(), proxyClass.getInterfaces());
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
}
