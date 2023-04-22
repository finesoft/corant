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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import jakarta.enterprise.inject.spi.BeanManager;

/**
 * corant-context
 *
 * @author bingo 下午5:03:55
 *
 */
public class ProxyBuilder {

  /**
   * Build normal Interface-based dynamic proxy instance.
   *
   *
   * @param <T> interface type
   * @param interfaceType interface for the proxy class to implement
   * @param invokerHandler the invocation handler creator, used to create concrete implementation
   *        from a method.
   * @return a dynamic proxy instance for the specified interface that dispatches method invocations
   *         to the specified invocation handler.
   *
   * @see MethodInvoker
   */
  @SuppressWarnings("unchecked")
  public static <T> T build(final Class<?> interfaceType,
      final Function<Method, MethodInvoker> invokerHandler) {
    return (T) Proxy.newProxyInstance(interfaceType.getClassLoader(), new Class[] {interfaceType},
        new ProxyInvocationHandler(interfaceType, invokerHandler));
  }

  /**
   * Build contextual Interface-based dynamic proxy instance.
   *
   * @param <T> interface type
   * @param beanManager bean manager for contextual bean handling
   * @param interfaceType interface for the proxy class to implement
   * @param invokerHandler the invocation handler creator, used to create concrete implementation
   *        from a method.
   * @return a dynamic proxy contextual instance for the specified interface that dispatches method
   *         invocations to the specified invocation handler.
   *
   * @see MethodInvoker
   */
  @SuppressWarnings("unchecked")
  public static <T> T buildContextual(final BeanManager beanManager, final Class<?> interfaceType,
      final Function<Method, MethodInvoker> invokerHandler) {
    return (T) Proxy.newProxyInstance(interfaceType.getClassLoader(), new Class[] {interfaceType},
        new ContextualInvocationHandler(beanManager, interfaceType, invokerHandler));
  }

  /**
   * Build contextual bean method handler instance from the declared methods of the given bean
   * class.
   *
   * @param clazz bean class
   * @param methodPredicate the method predicate use for method selecting
   * @param appendQualifiers the append qualifiers for bean instance selecting
   * @return a contextual method handlers
   *
   * @see Class#getDeclaredMethods()
   */
  public static Set<ContextualMethodHandler> buildDeclaredMethods(final Class<?> clazz,
      Predicate<Method> methodPredicate, Annotation... appendQualifiers) {
    return ContextualMethodHandler.fromDeclared(clazz, methodPredicate, appendQualifiers);
  }

  /**
   * Build contextual bean method handler instance from the methods of the given bean class.
   *
   * @param clazz bean class
   * @param methodPredicate the method predicate use for method selecting
   * @param appendQualifiers the append qualifiers for bean instance selecting
   * @return a contextual method handlers
   *
   * @see Class#getMethods()
   */
  public static Set<ContextualMethodHandler> buildMethods(final Class<?> clazz,
      Predicate<Method> methodPredicate, Annotation... appendQualifiers) {
    return ContextualMethodHandler.from(clazz, methodPredicate, appendQualifiers);
  }
}
