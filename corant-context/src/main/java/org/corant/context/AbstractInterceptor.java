/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.context;

import static org.corant.shared.util.Classes.getUserClass;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.inject.Intercepted;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.interceptor.InvocationContext;
import org.corant.shared.exception.NotSupportedException;

/**
 * corant-context
 *
 * @author bingo 上午9:41:39
 *
 */
public abstract class AbstractInterceptor {

  @Inject
  @Intercepted
  protected Bean<?> interceptedBean;

  @Inject
  protected BeanManager beanManager;

  protected AnnotatedType<?> currentAnnotatedType(InvocationContext ic) {
    if (ic != null) {
      if (ic.getMethod() != null) {
        return beanManager.createAnnotatedType(ic.getMethod().getDeclaringClass());
      } else if (ic.getConstructor() != null) {
        return beanManager.createAnnotatedType(ic.getConstructor().getDeclaringClass());
      }
    }
    throw new NotSupportedException();
  }

  @SuppressWarnings("unchecked")
  protected <T extends Annotation> T getInterceptorAnnotation(InvocationContext ic,
      Class<T> interceptorAnnotationType) {
    if (ic.getContextData().containsKey(Contexts.WELD_INTERCEPTOR_BINDINGS_KEY)) {
      // for weld CDI
      Set<Annotation> annotationBindings =
          (Set<Annotation>) ic.getContextData().get(Contexts.WELD_INTERCEPTOR_BINDINGS_KEY);
      for (Annotation annotation : annotationBindings) {
        if (annotation.annotationType() == interceptorAnnotationType) {
          return (T) annotation;
        }
      }
    }
    T interceptorAnnotation;
    Set<ElementType> targets = getTarget(interceptorAnnotationType);
    if (interceptedBean != null) {
      // for normal CDI
      AnnotatedType<?> currentAnnotatedType = currentAnnotatedType(ic);
      Set<Annotation> invocationPointAnnotations = Collections.emptySet();
      if (ic.getMethod() != null) {
        for (AnnotatedMethod<?> methodInSearch : currentAnnotatedType.getMethods()) {
          if (methodInSearch.getJavaMember().equals(ic.getMethod())) {
            invocationPointAnnotations = methodInSearch.getAnnotations();
            break;
          }
        }
      } else if (ic.getConstructor() != null) {
        for (AnnotatedConstructor<?> constructorInSearch : currentAnnotatedType.getConstructors()) {
          if (constructorInSearch.getJavaMember().equals(ic.getConstructor())) {
            invocationPointAnnotations = constructorInSearch.getAnnotations();
            break;
          }
        }
      }

      if ((targets.contains(ElementType.METHOD) || targets.contains(ElementType.CONSTRUCTOR))
          && (interceptorAnnotation = getInterceptorAnnotationRecursive(interceptorAnnotationType,
              invocationPointAnnotations)) != null) {
        return interceptorAnnotation;
      }
      if (targets.contains(ElementType.TYPE)
          && (interceptorAnnotation = getInterceptorAnnotationRecursive(interceptorAnnotationType,
              currentAnnotatedType.getAnnotations())) != null) {
        return interceptorAnnotation;
      }
      for (Class<? extends Annotation> stereotype : interceptedBean.getStereotypes()) {
        interceptorAnnotation = stereotype.getAnnotation(interceptorAnnotationType);
        if (interceptorAnnotation != null) {
          return interceptorAnnotation;
        }
      }
    } else // for EE components
    if (targets.contains(ElementType.METHOD) || targets.contains(ElementType.CONSTRUCTOR)) {
      if (ic.getMethod() != null && (interceptorAnnotation =
          ic.getMethod().getAnnotation(interceptorAnnotationType)) != null) {
        return interceptorAnnotation;
      }
      if (ic.getConstructor() != null && (interceptorAnnotation =
          ic.getConstructor().getAnnotation(interceptorAnnotationType)) != null) {
        return interceptorAnnotation;
      }
    } else if (targets.contains(ElementType.TYPE) && ic.getTarget() != null) {
      Class<?> targetClass = getUserClass(ic.getTarget());
      interceptorAnnotation = targetClass.getAnnotation(interceptorAnnotationType);
      if (interceptorAnnotation != null) {
        return interceptorAnnotation;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  protected <T extends Annotation> T getInterceptorAnnotationRecursive(
      Class<T> interceptorAnnotationType, Annotation... annotationsOnMember) {
    if (annotationsOnMember == null) {
      return null;
    }
    Set<Class<? extends Annotation>> stereotypeAnnotations = new HashSet<>();
    for (Annotation annotation : annotationsOnMember) {
      if (annotation.annotationType().equals(interceptorAnnotationType)) {
        return (T) annotation;
      }
      if (beanManager.isStereotype(annotation.annotationType())) {
        stereotypeAnnotations.add(annotation.annotationType());
      }
    }
    for (Class<? extends Annotation> stereotypeAnnotation : stereotypeAnnotations) {
      return getInterceptorAnnotationRecursive(interceptorAnnotationType,
          beanManager.getStereotypeDefinition(stereotypeAnnotation));
    }
    return null;
  }

  protected <T extends Annotation> T getInterceptorAnnotationRecursive(
      Class<T> interceptorAnnotationType, Set<Annotation> annotationsOnMember) {
    return getInterceptorAnnotationRecursive(interceptorAnnotationType,
        annotationsOnMember.toArray(new Annotation[0]));
  }

  protected Set<ElementType> getTarget(Class<?> interceptorAnnotationType) {
    Set<ElementType> targets = EnumSet.noneOf(ElementType.class);
    for (Target t : interceptorAnnotationType.getAnnotationsByType(Target.class)) {
      Collections.addAll(targets, t.value());
    }
    return targets;
  }
}
