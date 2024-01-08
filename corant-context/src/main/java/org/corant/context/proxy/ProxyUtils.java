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

import static java.util.Collections.emptyList;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.inject.spi.Interceptor;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Classes;

/**
 * corant-context
 *
 * @author bingo 下午10:09:48
 */
public class ProxyUtils {

  static final Map<Method, MethodHandle> defaultMethodHandleCache = new ConcurrentHashMap<>();

  /**
   * Returns interceptor bindings from the given annotations and the given bean manager.
   *
   * @param annotations the annotations to find out interceptor bindings
   * @param beanManager the bean manager use to filter
   * @return an annotations list in the given annotation that match the interceptor binding
   */
  public static List<Annotation> getInterceptorBindings(Annotation[] annotations,
      BeanManager beanManager) {
    if (annotations.length == 0) {
      return Collections.emptyList();
    }
    List<Annotation> bindings = new ArrayList<>(annotations.length);
    for (Annotation annotation : annotations) {
      if (beanManager.isInterceptorBinding(annotation.annotationType())) {
        bindings.add(annotation);
      }
    }
    return bindings;
  }

  /**
   * Resolve bean methods and their interceptor invocations from the given bean type with the given
   * bean manager and the given creational context.
   * <p>
   * Note: Currently we do not support resolving static methods.
   * <p>
   * FIXME for now I am not sure yet, the default method interceptor?
   *
   * @param beanManager bean manager to handle interceptors
   * @param creationalContext the creational context use for interceptor instantiation
   * @param beanType the target bean interface type with some interceptors
   * @return a maps that key is the method of target bean interface and value is the interceptor
   *         invocations of the method， the interceptor invocation contains interceptor instance and
   *         the metaobject of the interceptor.
   *
   * @see BeanManager#resolveInterceptors(InterceptionType, Annotation...)
   * @see InterceptorInvocation
   */
  public static Map<Method, List<InterceptorInvocation>> getInterceptorChains(
      BeanManager beanManager, CreationalContext<?> creationalContext, Class<?> beanType) {
    Map<Method, List<InterceptorInvocation>> chains = new HashMap<>();
    Map<Interceptor<?>, Object> interceptorInstances = new HashMap<>();
    List<Annotation> classLevelBindings =
        getInterceptorBindings(beanType.getAnnotations(), beanManager);

    for (Method method : beanType.getMethods()) {
      if ( /* method.isDefault() || */ Modifier.isStatic(method.getModifiers())) {
        continue;
      }
      List<Annotation> methodLevelBindings =
          getInterceptorBindings(method.getAnnotations(), beanManager);
      if (!classLevelBindings.isEmpty() || !methodLevelBindings.isEmpty()) {
        List<Annotation> interceptorBindings = merge(methodLevelBindings, classLevelBindings);
        List<Interceptor<?>> interceptors = beanManager.resolveInterceptors(
            InterceptionType.AROUND_INVOKE, interceptorBindings.toArray(new Annotation[0]));
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
   * Resolve bean method and it's interceptor invocations from the given bean method with the given
   * bean manager and the given creational context.
   * <p>
   * Note: Currently we do not support resolving static methods.
   * <p>
   * FIXME for now I am not sure yet, the default method interceptor?
   *
   * @param beanManager bean manager to handle interceptors
   * @param creationalContext the creational context use for interceptor instantiation
   * @param method the target bean method type with some interceptors
   * @return a list contains the interceptor invocations of the method， the interceptor invocation
   *         contains interceptor instance and the metaobject of the interceptor.
   *
   * @see BeanManager#resolveInterceptors(InterceptionType, Annotation...)
   * @see InterceptorInvocation
   */
  public static List<InterceptorInvocation> getInterceptorChains(BeanManager beanManager,
      CreationalContext<?> creationalContext, Method method) {
    if (!Modifier.isStatic(method.getModifiers())) {
      Map<Interceptor<?>, Object> interceptorInstances = new HashMap<>();
      List<Annotation> classLevelBindings =
          getInterceptorBindings(method.getDeclaringClass().getAnnotations(), beanManager);
      List<Annotation> methodLevelBindings =
          getInterceptorBindings(method.getAnnotations(), beanManager);
      if (!classLevelBindings.isEmpty() || !methodLevelBindings.isEmpty()) {
        List<Annotation> interceptorBindings = merge(methodLevelBindings, classLevelBindings);
        List<Interceptor<?>> interceptors = beanManager.resolveInterceptors(
            InterceptionType.AROUND_INVOKE, interceptorBindings.toArray(new Annotation[0]));
        if (!interceptors.isEmpty()) {
          List<InterceptorInvocation> chain = new ArrayList<>();
          for (Interceptor<?> interceptor : interceptors) {
            chain.add(new InterceptorInvocation(interceptor,
                interceptorInstances.computeIfAbsent(interceptor,
                    i -> beanManager.getReference(i, i.getBeanClass(), creationalContext))));
          }
          return chain;
        }
      }
    }
    return emptyList();
  }

  /**
   * Returns the result of calling the given default method on the given target object with the
   * given method arguments.
   *
   * <p>
   * FIXME UNFINISHED YET!
   *
   * @param target target object
   * @param method target object default method
   * @param args target object default method parameters
   * @return invoke default method result
   */
  public static Object invokeDefaultMethod(Object target, Method method, Object[] args) {
    try {
      return defaultMethodHandleCache.computeIfAbsent(method, m -> {
        try {
          if (Classes.CLASS_VERSION <= 52) {
            Class<?> declaringClass = method.getDeclaringClass();
            Constructor<MethodHandles.Lookup> constructor =
                MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
            constructor.setAccessible(true);
            return constructor.newInstance(declaringClass, MethodHandles.Lookup.PRIVATE)
                .in(declaringClass).unreflectSpecial(method, declaringClass);
          } else {
            MethodType methodType =
                MethodType.methodType(method.getReturnType(), method.getParameterTypes());
            return MethodHandles.lookup().findSpecial(method.getDeclaringClass(), method.getName(),
                methodType, method.getDeclaringClass());
          }
        } catch (Throwable e) {
          throw new CorantRuntimeException(e);
        }
      }).bindTo(target).invokeWithArguments(args);
    } catch (Throwable e) {
      throw new CorantRuntimeException(e);
    }
  }

  /**
   * Returns whether the given class is CDI un-proxyable bean class.
   * <p>
   * Unproxyable bean class: classes which are declared final,or expose final methods, or have no
   * non-private no-args constructor. (see CDI 2.0 spec, section 3.11)
   *
   * @param clazz the class to check
   * @return true if the given class is CDI un-proxyable bean class otherwise false.
   */
  public static boolean isCDIUnproxyableClass(Class<?> clazz) {
    return Modifier.isFinal(clazz.getModifiers()) || hasNonPrivateNonStaticFinalMethod(clazz)
        || hasNoNonPrivateNoArgsConstructor(clazz);
  }

  public static boolean isProxyOfSameInterfaces(Object arg, Class<?> proxyClass) {
    return proxyClass.isInstance(arg) || Proxy.isProxyClass(arg.getClass())
        && Arrays.equals(arg.getClass().getInterfaces(), proxyClass.getInterfaces());
  }

  static boolean hasNoNonPrivateNoArgsConstructor(Class<?> clazz) {
    Constructor<?> constructor;
    try {
      constructor = clazz.getConstructor();
    } catch (NoSuchMethodException exception) {
      return true;
    }
    return Modifier.isPrivate(constructor.getModifiers());
  }

  static boolean hasNonPrivateNonStaticFinalMethod(Class<?> type) {
    for (Class<?> clazz = type; clazz != null && clazz != Object.class; clazz =
        clazz.getSuperclass()) {
      for (Method method : clazz.getDeclaredMethods()) {
        if (Modifier.isFinal(method.getModifiers()) && !Modifier.isPrivate(method.getModifiers())
            && !Modifier.isStatic(method.getModifiers())) {
          return true;
        }
      }
    }
    return false;
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
