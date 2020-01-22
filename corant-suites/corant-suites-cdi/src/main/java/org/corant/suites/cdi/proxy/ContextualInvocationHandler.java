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
package org.corant.suites.cdi.proxy;

import static org.corant.shared.util.Empties.isNotEmpty;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;
import org.corant.suites.cdi.proxy.ProxyInvocationHandler.MethodInvoker;
import org.corant.suites.cdi.proxy.InterceptorInvocations.InterceptorInvocation;
import org.corant.suites.cdi.proxy.InterceptorInvocations.InvocationContextImpl;

/**
 *
 * @author bingo 下午2:12:51
 *
 */
public class ContextualInvocationHandler implements InvocationHandler {

  private final Class<?> clazz;
  private final CreationalContext<?> creationalContext;
  private final Object proxyObject;
  private final Map<Method, List<InterceptorInvocation>> interceptorChains;

  public ContextualInvocationHandler(final Class<?> clazz, final BeanManager beanManager,
      final Function<Method, MethodInvoker> invokerHandler) {
    super();
    this.clazz = clazz;
    proxyObject = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] {clazz},
        new ProxyInvocationHandler(clazz, invokerHandler));
    if (beanManager != null) {
      creationalContext = beanManager.createCreationalContext(null);
      interceptorChains =
          InterceptorInvocations.getInterceptorChains(beanManager, creationalContext, clazz);
    } else {
      creationalContext = null;
      interceptorChains = Collections.emptyMap();
    }
  }

  @Override
  public Object invoke(Object o, Method method, Object[] args) throws Throwable {
    List<InterceptorInvocation> interceptorInvocations = interceptorChains.get(method);
    if (isNotEmpty(interceptorInvocations)) {
      return new InvocationContextImpl(proxyObject, method, args, interceptorInvocations)
          .proceed();
    } else {
      return method.invoke(proxyObject, args);
    }
  }

  @Override
  public String toString() {
    return clazz.getName();
  }

}
