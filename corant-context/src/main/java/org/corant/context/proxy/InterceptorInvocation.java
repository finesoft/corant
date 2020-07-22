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

import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * corant-context
 *
 * @author bingo 上午10:41:54
 *
 */
public class InterceptorInvocation {

  @SuppressWarnings("rawtypes")
  private final Interceptor interceptor;

  private final Object interceptorInstance;

  public InterceptorInvocation(final Interceptor<?> interceptor, final Object interceptorInstance) {
    this.interceptor = interceptor;
    this.interceptorInstance = interceptorInstance;
  }

  @SuppressWarnings("unchecked")
  Object invoke(InvocationContext ctx) throws Exception {
    return interceptor.intercept(InterceptionType.AROUND_INVOKE, interceptorInstance, ctx);
  }
}
