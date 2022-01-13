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
package org.corant.context;

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.InjectionPoint;
import org.corant.shared.util.Methods.MethodSignature;
import org.corant.shared.util.Types;
import org.jboss.weld.injection.ParameterInjectionPoint;

/**
 * corant-context
 *
 * @author bingo 下午6:29:46
 *
 */
public abstract class CDIs {

  public static <U> CompletionStage<U> fireAsyncEvent(U event, Annotation... qualifiers) {
    shouldNotNull(event, "Fire async event error, the event object can not null!");
    if (qualifiers.length > 0) {
      return CDI.current().getBeanManager().getEvent().select(qualifiers).fireAsync(event);
    } else {
      return CDI.current().getBeanManager().getEvent().fireAsync(event);
    }
  }

  public static void fireEvent(Object event, Annotation... qualifiers) {
    shouldNotNull(event, "Fire event error, the event object can not null!");
    if (qualifiers.length > 0) {
      CDI.current().getBeanManager().getEvent().select(qualifiers).fire(event);
    } else {
      CDI.current().getBeanManager().getEvent().fire(event);
    }
  }

  public static Annotated getAnnotated(InjectionPoint injectionPoint) {
    if (injectionPoint instanceof ParameterInjectionPoint) {
      return ((ParameterInjectionPoint<?, ?>) injectionPoint).getAnnotated().getDeclaringCallable();
    }
    return injectionPoint.getAnnotated();
  }

  public static <T extends Annotation> T getAnnotation(InjectionPoint injectionPoint,
      Class<T> annotationType) {
    return getAnnotated(injectionPoint).getAnnotation(annotationType);
  }

  public static <T extends Annotation> Set<T> getAnnotations(InjectionPoint injectionPoint,
      Class<T> annotationType) {
    return getAnnotated(injectionPoint).getAnnotations(annotationType);
  }

  public static MethodSignature getMethodSignature(AnnotatedMethod<?> method) {
    String methodName = method.getJavaMember().getName();
    List<?> parameters = method.getParameters();
    int size = parameters.size();
    String[] parameterTypes = new String[size];
    for (int i = 0; i < size; i++) {
      parameterTypes[i] =
          Types.getRawType(((AnnotatedParameter<?>) parameters.get(i)).getBaseType()).getName();
    }
    return MethodSignature.of(methodName, parameterTypes);
  }

  public static boolean isEnabled() {
    try {
      return CDI.current() != null;
    } catch (IllegalStateException e) {
      // Noop!
    }
    return false;
  }
}
