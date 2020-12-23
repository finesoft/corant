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

import static org.corant.shared.util.Empties.isNotEmpty;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.enterprise.inject.spi.BeanManager;

/**
 * corant-context
 *
 *
 * @author bingo 下午2:12:51
 *
 */
public class ContextualInvocationHandler extends ProxyInvocationHandler {

  private final Map<Method, List<InterceptorInvocation>> interceptorChains;

  public ContextualInvocationHandler(final BeanManager beanManager, final Class<?> clazz,
      final Function<Method, MethodInvoker> invokerHandler) {
    super(clazz, invokerHandler);
    if (beanManager != null) {
      interceptorChains = Collections.unmodifiableMap(ProxyUtils.getInterceptorChains(beanManager,
          beanManager.createCreationalContext(null), clazz));
    } else {
      interceptorChains = Collections.emptyMap();
    }
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public Object invoke(Object o, Method method, Object[] args) throws Throwable {
    List<InterceptorInvocation> interceptorInvocations = interceptorChains.get(method);
    if (isNotEmpty(interceptorInvocations)) {
      return new InvocationContextImpl(clazz, o, method, invokers.get(method), args,
          interceptorInvocations).proceed();
    } else {
      return new InvocationContextImpl(clazz, o, method, invokers.get(method), args,
          Collections.emptyList()).proceed();
    }
  }

  @Override
  public String toString() {
    return clazz.getName();
  }

}
