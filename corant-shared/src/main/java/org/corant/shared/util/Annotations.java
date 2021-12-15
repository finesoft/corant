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

import static org.corant.shared.util.Lists.linkedListOf;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * corant-shared
 *
 * The annotation utility class.
 *
 * NOTE: Starting with Java8 we recommend using AnnotatedElement
 *
 * @author bingo 下午10:06:13
 *
 */
public class Annotations {

  public static final Annotation[] EMPTY_ARRAY = {};

  /**
   * Find the annotation object from given annotated element with given annotation type class, this
   * is base method. NOTE: Starting with Java8, we recommend using
   * {@link AnnotatedElement#getDeclaredAnnotation(Class)}
   *
   * @param element the AnnotatedElement object corresponding to the annotation type
   * @param annotationType the type of the annotation to query
   * @return the first matching annotation, or null if not found.
   */
  public static <A extends Annotation> A findAnnotation(AnnotatedElement element,
      Class<A> annotationType) {
    if (element != null && annotationType != null) {
      A annotation = element.getDeclaredAnnotation(annotationType);
      if (annotation == null) {
        // we need to find composed annotation
        LinkedList<Annotation> declareds = linkedListOf(element.getDeclaredAnnotations());
        if (declareds.peek() != null) {// not empty
          Set<Annotation> visiteds = new HashSet<>(); // prevents death loops
          Annotation declared;
          while ((declared = declareds.poll()) != null && visiteds.add(declared)) {
            Class<? extends Annotation> declaredType = declared.annotationType();
            // ignore @Target @Retention and other java.lang.annotation meta annotations
            if (!declaredType.getName().startsWith("java.lang.annotation")) {
              if ((annotation = declaredType.getDeclaredAnnotation(annotationType)) != null) {
                break;
              } else {
                Collections.addAll(declareds, declaredType.getDeclaredAnnotations());
              }
            }
          }
          visiteds.clear();
        }
      }
      return annotation;
    }
    return null;
  }

  /**
   * Find the annotation object from given class with given annotation type class or optionally find
   * from the super classes (don't include interfaces) of the given class.
   *
   * @param clazz the class with annotated to be found
   * @param annotationType the annotation class which to be found
   * @param searchSupers If true, all super classes are looked up.
   * @return the first matching annotation, or null if not found.
   */
  public static <A extends Annotation> A findAnnotation(Class<?> clazz, Class<A> annotationType,
      boolean searchSupers) {
    A annotation = findAnnotation(clazz, annotationType);
    if (annotation == null && searchSupers && clazz != null && annotationType != null) {
      Class<?> superClazz = clazz.getSuperclass();
      if (superClazz != null && Object.class != superClazz) {
        annotation = findAnnotation(superClazz, annotationType, true);
      }
    }
    return annotation;
  }

  /**
   * Find the annotation object with the given annotation type that is present on the given method
   * or optionally on any equivalent method in super classes and interfaces. Returns null if the
   * annotation type was not present.
   *
   * @param method the class with annotated to be found
   * @param annotationType the annotation class which to be found
   * @param searchSupers If true, all super classes or interfaces are looked up.
   * @param ignoreAccess determines if underlying method has to be accessible
   * @return the annotation if not found will return null.
   */
  public static <A extends Annotation> A findAnnotation(final Method method,
      final Class<A> annotationType, final boolean searchSupers, final boolean ignoreAccess) {
    if (method == null || annotationType == null) {
      return null;
    }
    if (!ignoreAccess && (!Modifier.isPublic(method.getModifiers()) || method.isSynthetic())) {
      return null;
    }
    A annotation = method.getAnnotation(annotationType);
    if (annotation == null && searchSupers) {
      final Class<?> declaringClazz = method.getDeclaringClass();
      final Set<Class<?>> classes = Classes.getAllSuperclassesAndInterfaces(declaringClazz);
      for (final Class<?> cls : classes) {
        Method equivalentMethod;
        try {
          equivalentMethod =
              ignoreAccess ? cls.getDeclaredMethod(method.getName(), method.getParameterTypes())
                  : cls.getMethod(method.getName(), method.getParameterTypes());
        } catch (final NoSuchMethodException e) {
          continue;
        }
        annotation = equivalentMethod.getAnnotation(annotationType);
        if (annotation != null) {
          break;
        }
      }
    }

    return annotation;
  }
}
