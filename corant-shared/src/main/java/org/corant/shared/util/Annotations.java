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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import org.corant.shared.ubiquity.Tuple.Pair;

/**
 * corant-shared
 * <p>
 * The annotation utility class.
 * <p>
 * NOTE: Starting with Java8, we recommend using AnnotatedElement
 *
 * @author bingo 下午10:06:13
 */
public class Annotations {

  public static final Annotation[] EMPTY_ARRAY = {};

  /**
   * Returns annotation members hash code
   *
   * @param members the member name and value pairs
   *
   * @see Annotation#hashCode()
   */
  @SafeVarargs
  public static int calculateMembersHashCode(Pair<String, Object>... members) {
    int hashCode = 0;
    for (Pair<String, Object> member : members) {
      int memberNameHashCode = 127 * member.left().hashCode();
      Object value = member.right();
      int memberValueHashCode;
      if (value instanceof boolean[]) {
        memberValueHashCode = Arrays.hashCode((boolean[]) value);
      } else if (value instanceof short[]) {
        memberValueHashCode = Arrays.hashCode((short[]) value);
      } else if (value instanceof int[]) {
        memberValueHashCode = Arrays.hashCode((int[]) value);
      } else if (value instanceof long[]) {
        memberValueHashCode = Arrays.hashCode((long[]) value);
      } else if (value instanceof float[]) {
        memberValueHashCode = Arrays.hashCode((float[]) value);
      } else if (value instanceof double[]) {
        memberValueHashCode = Arrays.hashCode((double[]) value);
      } else if (value instanceof byte[]) {
        memberValueHashCode = Arrays.hashCode((byte[]) value);
      } else if (value instanceof char[]) {
        memberValueHashCode = Arrays.hashCode((char[]) value);
      } else if (value instanceof Object[]) {
        memberValueHashCode = Arrays.hashCode((Object[]) value);
      } else {
        memberValueHashCode = value.hashCode();
      }
      hashCode += memberNameHashCode ^ memberValueHashCode;
    }
    return hashCode;
  }

  /**
   * Returns whether the given two annotation member are equal.
   *
   * @param m1 member one
   * @param m2 member two
   */
  public static boolean equalMember(Object m1, Object m2) {
    if (m1 instanceof byte[] && m2 instanceof byte[]) {
      if (!Arrays.equals((byte[]) m1, (byte[]) m2)) {
        return false;
      }
    } else if (m1 instanceof short[] && m2 instanceof short[]) {
      if (!Arrays.equals((short[]) m1, (short[]) m2)) {
        return false;
      }
    } else if (m1 instanceof int[] && m2 instanceof int[]) {
      if (!Arrays.equals((int[]) m1, (int[]) m2)) {
        return false;
      }
    } else if (m1 instanceof long[] && m2 instanceof long[]) {
      if (!Arrays.equals((long[]) m1, (long[]) m2)) {
        return false;
      }
    } else if (m1 instanceof float[] && m2 instanceof float[]) {
      if (!Arrays.equals((float[]) m1, (float[]) m2)) {
        return false;
      }
    } else if (m1 instanceof double[] && m2 instanceof double[]) {
      if (!Arrays.equals((double[]) m1, (double[]) m2)) {
        return false;
      }
    } else if (m1 instanceof char[] && m2 instanceof char[]) {
      if (!Arrays.equals((char[]) m1, (char[]) m2)) {
        return false;
      }
    } else if (m1 instanceof boolean[] && m2 instanceof boolean[]) {
      if (!Arrays.equals((boolean[]) m1, (boolean[]) m2)) {
        return false;
      }
    } else if (m1 instanceof Object[] && m2 instanceof Object[]) {
      if (!Arrays.equals((Object[]) m1, (Object[]) m2)) {
        return false;
      }
    } else if (!m1.equals(m2)) {
      return false;
    }
    return true;
  }

  /**
   * Find the annotation object from a given annotated element with given annotation type class,
   * this is a base method. NOTE: Starting with Java8, we recommend using
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
   * from the superclasses (don't include interfaces) of the given class.
   *
   * @param clazz the class with annotated to be found
   * @param annotationType the annotation class which to be found
   * @param searchSupers If true, all superclasses are looked up.
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
   * or optionally on any equivalent method in superclasses and interfaces. Returns null if the
   * annotation type was not present.
   *
   * @param method the class with annotated to be found
   * @param annotationType the annotation class which to be found
   * @param searchSupers If true, all superclasses or interfaces are looked up.
   * @param ignoreAccess determines if the underlying method has to be accessible
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
