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
package org.corant.kernel.util;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.StringUtils.isBlank;
import static org.corant.shared.util.StringUtils.trim;
import java.lang.annotation.Annotation;
import java.util.Set;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import org.corant.shared.util.MethodUtils.MethodSignature;
import org.corant.shared.util.TypeUtils;
import org.jboss.weld.injection.ParameterInjectionPoint;

/**
 * corant-kernel
 *
 * @author bingo 下午6:29:46
 *
 */
public abstract class Cdis {

  public static Annotated getAnnotated(InjectionPoint injectionPoint) {
    if (injectionPoint instanceof ParameterInjectionPoint) {
      return ((ParameterInjectionPoint<?, ?>) injectionPoint).getAnnotated().getDeclaringCallable();
    }
    return injectionPoint.getAnnotated();
  }

  public static final Annotation resolveNamed(String name) {
    return isBlank(name) ? Unnamed.INST : NamedLiteral.of(trim(name));
  }

  public MethodSignature getMethodSignature(AnnotatedMethod<?> method) {
    String methodName = method.getJavaMember().getName();
    String[] parameterTypes = new String[method.getParameters().size()];
    for (int i = 0; i < method.getParameters().size(); i++) {
      parameterTypes[i] =
          TypeUtils.getRawType(method.getParameters().get(i).getBaseType()).getName();
    }
    return MethodSignature.of(methodName, parameterTypes);
  }

  public static class InjectionTargetWrapper<X> implements InjectionTarget<X> {

    private final InjectionTarget<X> delegate;

    /**
     * @param delegate
     */
    public InjectionTargetWrapper(InjectionTarget<X> delegate) {
      super();
      this.delegate = shouldNotNull(delegate);
    }

    @Override
    public void dispose(X instance) {
      delegate.dispose(instance);
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
      return delegate.getInjectionPoints();
    }

    @Override
    public void inject(X instance, CreationalContext<X> ctx) {
      delegate.inject(instance, ctx);
    }

    @Override
    public void postConstruct(X instance) {
      delegate.postConstruct(instance);
    }

    @Override
    public void preDestroy(X instance) {
      delegate.preDestroy(instance);
    }

    @Override
    public X produce(CreationalContext<X> ctx) {
      return delegate.produce(ctx);
    }
  }
}
