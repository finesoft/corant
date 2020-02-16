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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
public class AnnotatedMethodInvoker {
  final AnnotatedType<?> annotatedType;
  final AnnotatedMethod<?> annotatedMethod;
  final Class<?> beanClass;
  final Method beanMethod;
  final Object beanInstance;

  public AnnotatedMethodInvoker(AnnotatedMethod<?> annotatedMethod) {
    this(null, annotatedMethod);
  }

  public AnnotatedMethodInvoker(AnnotatedType<?> annotatedType, AnnotatedMethod<?> annotatedMethod) {
    this.annotatedType = annotatedType;
    this.annotatedMethod = shouldNotNull(annotatedMethod);
    beanClass = annotatedType != null ? annotatedType.getJavaClass()
        : annotatedMethod.getJavaMember().getDeclaringClass();
    beanMethod = annotatedMethod.getJavaMember();
    final BeanManager bm = CDI.current().getBeanManager();
    final Bean<?> propertyResolverBean = bm.resolve(bm.getBeans(beanClass));
    final CreationalContext<?> creationalContext = bm.createCreationalContext(propertyResolverBean);
    beanInstance = bm.getReference(propertyResolverBean, beanClass, creationalContext);
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
    AnnotatedMethodInvoker other = (AnnotatedMethodInvoker) obj;
    if (beanInstance == null) {
      if (other.beanInstance != null) {
        return false;
      }
    } else if (!beanInstance.equals(other.beanInstance)) {
      return false;
    }
    if (beanMethod == null) {
      if (other.beanMethod != null) {
        return false;
      }
    } else if (!beanMethod.equals(other.beanMethod)) {
      return false;
    }
    return true;
  }

  /**
   *
   * @return the annotatedMethod
   */
  public AnnotatedMethod<?> getAnnotatedMethod() {
    return annotatedMethod;
  }

  /**
   *
   * @return the annotatedType
   */
  public AnnotatedType<?> getAnnotatedType() {
    return annotatedType;
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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (beanInstance == null ? 0 : beanInstance.hashCode());
    result = prime * result + (beanMethod == null ? 0 : beanMethod.hashCode());
    return result;
  }

  public Object invoke(Object... parameters)
      throws IllegalAccessException, InvocationTargetException {
    return beanMethod.invoke(beanInstance, parameters);
  }

}
