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
 * corant-shared
 *
 * The annotation utility class.
 *
 * @author bingo 下午10:06:13
 *
 */
public class AnnotationUtils {

  /**
   * Find the annotation object from given annotatedElement with given annotation type class, this
   * is base method.
   *
   * @param annEle
   * @param annTyp
   * @return the annotation, if not found will return null.
   */
  public static <A extends Annotation> A findAnnotation(AnnotatedElement annEle, Class<A> annTyp) {
    if (annEle != null) {
      Annotation[] anns = annEle.getDeclaredAnnotations();
      for (Annotation ann : anns) {
        if (ann.annotationType() == annTyp) {
          return forceCast(ann);
        }
      }
    }
    return null;
  }

  /**
   * Find the annotation object from given class with given annotation type class or optionally find
   * from the super classes of the given class.
   *
   * @param cls the class with annotated to be find
   * @param annTyp the annotation class which to be find
   * @param searchSupers If true, all super classes or interfaces are looked up.
   * @return the annotation, if not found will return null.
   */
  public static <A extends Annotation> A findAnnotation(Class<?> cls, Class<A> annTyp,
      boolean searchSupers) {
    Annotation[] anns = cls.getDeclaredAnnotations();
    for (Annotation ann : anns) {
      if (ann.annotationType() == annTyp) {
        return forceCast(ann);
      }
    }
    if (searchSupers) {
      Class<?> superCls = cls.getSuperclass();
      if (superCls == null || Object.class == superCls) {
        return null;
      }
      return findAnnotation(superCls, annTyp, searchSupers);
    }
    return null;
  }

  /**
   * Find the annotation object with the given annotation type that is present on the given method
   * or optionally on any equivalent method in super classes and interfaces. Returns null if the
   * annotation type was not present.
   *
   * @param method the class with annotated to be find
   * @param annotationCls the annotation class which to be find
   * @param searchSupers If true, all super classes or interfaces are looked up.
   * @param ignoreAccess determines if underlying method has to be accessible
   * @return the annotation if not found will return null.
   */
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
