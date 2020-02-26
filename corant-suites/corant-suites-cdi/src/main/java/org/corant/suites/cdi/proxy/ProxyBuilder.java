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
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.enterprise.inject.spi.BeanManager;
import org.corant.suites.cdi.proxy.ProxyInvocationHandler.MethodInvoker;

/**
 * corant-suites-cdi
 *
 * @author bingo 下午5:03:55
 *
 */
public class ProxyBuilder {

  /**
   * Build normal Interface-based dynamic proxy instance.
   *
   * @see MethodInvoker
   *
   * @param <T>
   * @param interfaceType
   * @param invokerHandler
   * @return build
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
   * @param <T>
   * @param beanManager
   * @param interfaceType
   * @param invokerHandler
   * @return buildContextual
   */
  @SuppressWarnings("unchecked")
  public static <T> T buildContextual(final BeanManager beanManager, final Class<?> interfaceType,
      final Function<Method, MethodInvoker> invokerHandler) {
    return (T) Proxy.newProxyInstance(interfaceType.getClassLoader(), new Class[] {interfaceType},
        new ContextualInvocationHandler(beanManager, interfaceType, invokerHandler));
  }

  /**
   * Build contextual bean method handler instance.
   *
   * @param clazz
   * @param methodPredicate
   * @param appendQualifierss
   * @return buildMethods
   */
  public static Set<ContextualMethodHandler> buildMethods(final Class<?> clazz,
      Predicate<Method> methodPredicate, Annotation... appendQualifierss) {
    return ContextualMethodHandler.from(clazz, methodPredicate, appendQualifierss);
  }
}
