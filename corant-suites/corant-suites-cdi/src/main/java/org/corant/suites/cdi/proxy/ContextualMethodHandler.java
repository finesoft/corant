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
import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-suites-cdi
 *
 * @author bingo 下午2:26:15
 *
 */
public class ContextualMethodHandler {
  final Class<?> clazz;
  final Method method;
  final Annotation[] qualifiers;
  volatile Object instance;

  protected ContextualMethodHandler(Class<?> beanClass, Method beanMethod,
      Annotation... qualifiers) {
    method = shouldNotNull(beanMethod);
    clazz = defaultObject(beanClass, beanMethod.getDeclaringClass());
    this.qualifiers = qualifiers;
  }

  protected ContextualMethodHandler(Method method) {
    this(null, method);
  }

  public static Set<ContextualMethodHandler> from(Class<?> clazz, Predicate<Method> methodPredicate,
      Annotation... qualifiers) {
    Set<ContextualMethodHandler> annotatedMethods = new LinkedHashSet<>();
    if (!clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers())) {
      for (Method m : clazz.getMethods()) {
        if (methodPredicate.test(m)) {
          annotatedMethods.add(new ContextualMethodHandler(clazz, m, qualifiers));
        }
      }
    }
    return annotatedMethods;
  }

  public static Set<ContextualMethodHandler> fromDeclared(Class<?> clazz,
      Predicate<Method> methodPredicate, Annotation... qualifiers) {
    Set<ContextualMethodHandler> annotatedMethods = new LinkedHashSet<>();
    if (!clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers())) {
      for (Method m : clazz.getDeclaredMethods()) {
        if (methodPredicate.test(m)) {
          if (!Modifier.isPublic(m.getModifiers())
              || !Modifier.isPublic(m.getDeclaringClass().getModifiers())) {
            m.setAccessible(true);
          }
          annotatedMethods.add(new ContextualMethodHandler(clazz, m, qualifiers));
        }
      }
    }
    return annotatedMethods;
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
    ContextualMethodHandler other = (ContextualMethodHandler) obj;
    if (clazz == null) {
      if (other.clazz != null) {
        return false;
      }
    } else if (!clazz.equals(other.clazz)) {
      return false;
    }
    if (method == null) {
      if (other.method != null) {
        return false;
      }
    } else if (!method.equals(other.method)) {
      return false;
    }
    return true;
  }

  /**
   *
   * @return the clazz
   */
  public Class<?> getClazz() {
    return clazz;
  }

  /**
   *
   * @return the instance
   */
  public Object getInstance() {
    return instance;
  }

  /**
   *
   * @return the method
   */
  public Method getMethod() {
    return method;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (clazz == null ? 0 : clazz.hashCode());
    result = prime * result + (method == null ? 0 : method.hashCode());
    return result;
  }

  public Object invoke(Object... parameters)
      throws IllegalAccessException, InvocationTargetException {
    initialize();
    return method.invoke(instance, parameters);
  }

  protected void initialize() {
    if (instance == null) {
      synchronized (this) {
        if (instance == null) {
          final BeanManager bm = CDI.current().getBeanManager();
          final Set<Bean<?>> beans = bm.getBeans(clazz, qualifiers);
          Bean<?> resolvedBean = null;
          if (sizeOf(beans) > 1) {
            resolvedBean =
                beans.stream().filter(b -> b.getBeanClass().equals(clazz)).findFirst().orElseThrow(
                    () -> new CorantRuntimeException("Can't resolve bean class %s", clazz));
          } else {
            resolvedBean = bm.resolve(bm.getBeans(clazz, qualifiers));
          }
          CreationalContext<?> creationalContext = bm.createCreationalContext(resolvedBean);
          instance = bm.getReference(resolvedBean, clazz, creationalContext);
        }
      }
    }
  }
}
