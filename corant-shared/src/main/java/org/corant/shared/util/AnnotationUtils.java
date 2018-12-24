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
package org.corant.shared.util;

import static org.corant.shared.util.ObjectUtils.forceCast;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * @author bingo 下午10:06:13
 *
 */
public class AnnotationUtils {

  public static <A extends Annotation> A findAnnotation(AnnotatedElement ae, Class<A> at) {
    Annotation[] anns = ae.getDeclaredAnnotations();
    for (Annotation ann : anns) {
      if (ann.annotationType() == at) {
        return forceCast(ann);
      }
    }
    return null;
  }

  public static <A extends Annotation> A findAnnotation(Class<?> cls, Class<A> at,
      boolean searchSupers) {
    Annotation[] anns = cls.getDeclaredAnnotations();
    for (Annotation ann : anns) {
      if (ann.annotationType() == at) {
        return forceCast(ann);
      }
    }
    if (searchSupers) {
      Class<?> superCls = cls.getSuperclass();
      if (superCls == null || Object.class == superCls) {
        return null;
      }
      return findAnnotation(superCls, at);
    }
    return null;
  }

  public static <A extends Annotation> A findAnnotation(final Method method,
      final Class<A> annotationCls, final boolean searchSupers, final boolean ignoreAccess) {
    if (method == null || annotationCls == null) {
      return null;
    }
    if (!ignoreAccess && !(Modifier.isPublic(method.getModifiers()) && !method.isSynthetic())) {
      return null;
    }
    A annotation = method.getAnnotation(annotationCls);
    if (annotation == null && searchSupers) {
      final Class<?> mcls = method.getDeclaringClass();
      final List<Class<?>> classes = ClassUtils.getAllSuperclassesAndInterfaces(mcls);
      for (final Class<?> acls : classes) {
        Method equivalentMethod;
        try {
          equivalentMethod =
              ignoreAccess ? acls.getDeclaredMethod(method.getName(), method.getParameterTypes())
                  : acls.getMethod(method.getName(), method.getParameterTypes());
        } catch (final NoSuchMethodException e) {
          continue;
        }
        annotation = equivalentMethod.getAnnotation(annotationCls);
        if (annotation != null) {
          break;
        }
      }
    }

    return annotation;
  }

}
