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
import static org.corant.shared.util.ObjectUtils.defaultObject;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

/**
 * corant-suites-cdi
 *
 * @author bingo 下午2:26:15
 *
 */
public class ProxyMethod {
  final Class<?> beanClass;
  final Method beanMethod;
  final Annotation[] qualifiers;
  volatile Object beanInstance;

  public ProxyMethod(AnnotatedMethod<?> beanMethod, Annotation... qualifiers) {
    this(shouldNotNull(beanMethod).getDeclaringType().getJavaClass(), beanMethod.getJavaMember(),
        qualifiers);
  }

  public ProxyMethod(Class<?> beanClass, Method beanMethod, Annotation... qualifiers) {
    this.beanMethod = shouldNotNull(beanMethod);
    this.beanClass = defaultObject(beanClass, beanMethod.getDeclaringClass());
    this.qualifiers = qualifiers;
  }

  public ProxyMethod(Method method) {
    this(null, method);
  }

  public static Set<ProxyMethod> from(AnnotatedType<?> annotatedType,
      Predicate<AnnotatedMethod<?>> methodPredicate) {
    Set<ProxyMethod> annotatedMethods = new LinkedHashSet<>();
    for (AnnotatedMethod<?> am : annotatedType.getMethods()) {
      if (methodPredicate.test(am)) {
        annotatedMethods.add(new ProxyMethod(am));
      }
    }
    return annotatedMethods;
  }

  /**
   *
   * @return the beanClass
   */
  public Class<?> getBeanClass() {
    return beanClass;
  }

  /**
   *
   * @return the beanInstance
   */
  public Object getBeanInstance() {
    return beanInstance;
  }

  /**
   *
   * @return the beanMethod
   */
  public Method getBeanMethod() {
    return beanMethod;
  }

  public Object invoke(Object... parameters)
      throws IllegalAccessException, InvocationTargetException {
    initialize();
    return beanMethod.invoke(beanInstance, parameters);
  }

  protected void initialize() {
    if (beanInstance == null) {
      synchronized (this) {
        if (beanInstance == null) {
          final BeanManager bm = CDI.current().getBeanManager();
          final Bean<?> resolvedBean = bm.resolve(bm.getBeans(beanClass, qualifiers));
          final CreationalContext<?> creationalContext = bm.createCreationalContext(resolvedBean);
          beanInstance = bm.getReference(resolvedBean, beanClass, creationalContext);
        }
      }
    }
  }
}
