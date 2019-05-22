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
package org.corant.suites.jta.shared;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.transaction.Transactional;
import org.corant.Corant;

/**
 * corant-suites-jta-shared
 *
 * @author bingo 下午7:22:15
 *
 */
public class TransactionalMetaUtils {

  public static Transactional getTransactionalAnnotationRecursive(AnnotatedMethod<?> method) {
    AnnotatedType<?> currentAnnotatedType = method.getDeclaringType();
    AnnotatedMethod<?> currentAnnotatedMethod = method;
    Transactional transactionalMethod =
        getTransactionalAnnotationRecursive(currentAnnotatedMethod.getAnnotations());
    if (transactionalMethod != null) {
      return transactionalMethod;
    }
    Transactional transactionalExtension =
        getTransactionalAnnotationRecursive(currentAnnotatedType.getAnnotations());
    if (transactionalExtension != null) {
      return transactionalExtension;
    }
    return null;
  }

  public static Transactional getTransactionalAnnotationRecursive(
      Annotation... annotationsOnMember) {
    if (annotationsOnMember == null) {
      return null;
    }
    Set<Class<? extends Annotation>> stereotypeAnnotations = new HashSet<>();
    for (Annotation annotation : annotationsOnMember) {
      if (annotation.annotationType().equals(Transactional.class)) {
        return (Transactional) annotation;
      }
      if (Corant.me().getBeanManager().isStereotype(annotation.annotationType())) {
        stereotypeAnnotations.add(annotation.annotationType());
      }
    }
    for (Class<? extends Annotation> stereotypeAnnotation : stereotypeAnnotations) {
      return getTransactionalAnnotationRecursive(
          Corant.me().getBeanManager().getStereotypeDefinition(stereotypeAnnotation));
    }
    return null;
  }

  public static Transactional getTransactionalAnnotationRecursive(
      Set<Annotation> annotationsOnMember) {
    return getTransactionalAnnotationRecursive(
        annotationsOnMember.toArray(new Annotation[annotationsOnMember.size()]));
  }
}
