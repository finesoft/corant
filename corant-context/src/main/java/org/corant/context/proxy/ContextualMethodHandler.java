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
import static org.corant.shared.util.Objects.defaultObject;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import org.corant.context.Beans;
import org.corant.shared.util.Methods.MethodSignature;

/**
 * corant-context
 *
 * @author bingo 下午2:26:15
 *
 */
public class ContextualMethodHandler implements Serializable {

  private static final long serialVersionUID = -195173432418927348L;

  protected Annotation[] qualifiers;
  protected Class<?> clazz;
  protected transient Method method; // ?? use java.lang.invoke.MethodHandle
  protected MethodSignature methodSignature;

  public ContextualMethodHandler(Class<?> beanClass, Method beanMethod, Annotation... qualifiers) {
    method = shouldNotNull(beanMethod);
    methodSignature = MethodSignature.of(method);
    clazz = defaultObject(beanClass, beanMethod::getDeclaringClass);
    this.qualifiers = qualifiers;
  }

  public ContextualMethodHandler(Method method) {
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
    // FIXME the class qualifiers
    if (!clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers())) {
      for (Method m : clazz.getDeclaredMethods()) {
        if (methodPredicate.test(m)) {
          if (!Modifier.isPublic(m.getModifiers())
              || !Modifier.isPublic(m.getDeclaringClass().getModifiers())) {
            AccessController.doPrivileged((PrivilegedAction<Method>) () -> {
              m.setAccessible(true);
              return null;
            });
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
      return other.method == null;
    } else {
      return method.equals(other.method);
    }
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
   * @return the method
   */
  public Method getMethod() {
    return method;
  }

  public MethodSignature getMethodSignature() {
    return methodSignature;
  }

  public Annotation[] getQualifiers() {
    return Arrays.copyOf(qualifiers, qualifiers.length);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (clazz == null ? 0 : clazz.hashCode());
    return prime * result + (method == null ? 0 : method.hashCode());
  }

  public Object invoke(Object... parameters)
      throws IllegalAccessException, InvocationTargetException {
    return method.invoke(Beans.resolve(clazz, qualifiers), parameters);
  }

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    method = Arrays.stream(clazz.getDeclaredMethods()).filter(methodSignature::matches).findFirst()
        .orElseThrow();
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
  }
}
