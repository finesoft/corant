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
package org.corant.context.concurrent.interceptor;

import static org.corant.context.concurrent.ConcurrentExtension.ENABLE_CONCURRENT_THROTTLE_INTERCEPTOR_CFG;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import javax.annotation.Priority;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.corant.context.AbstractInterceptor;
import org.corant.context.concurrent.annotation.ConcurrencyThrottle;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.service.RequiredConfiguration;
import org.corant.shared.service.RequiredConfiguration.ValuePredicate;
import org.corant.shared.util.Methods.MethodSignature;

/**
 * corant-context
 *
 * @author bingo 上午9:50:43
 *
 */
@Interceptor
@ConcurrencyThrottle
@Priority(Interceptor.Priority.PLATFORM_BEFORE)
@RequiredConfiguration(key = ENABLE_CONCURRENT_THROTTLE_INTERCEPTOR_CFG,
    predicate = ValuePredicate.EQ, type = Boolean.class, value = "true")
public class ConcurrencyThrottleInterceptor extends AbstractInterceptor {

  static final Map<MethodSignature, Semaphore> THROTTLES = new ConcurrentHashMap<>();

  @AroundInvoke
  @AroundConstruct
  public Object concurrencyThrottleInvocation(final InvocationContext ctx) throws Exception {
    ConcurrencyThrottle ann = getInterceptorAnnotation(ctx, ConcurrencyThrottle.class);
    final int max = Integer.max(ann.max(), ConcurrencyThrottle.DFLT_THRON);
    final boolean fair = ann.fair();
    Semaphore counting = ConcurrencyThrottleInterceptor.THROTTLES
        .computeIfAbsent(new MethodSignature(ctx.getMethod()), k -> new Semaphore(max, fair));
    boolean acquireSuccess = false;
    try {
      counting.acquire();
      acquireSuccess = true;
      return ctx.proceed();
    } catch (Exception ex) {
      throw new CorantRuntimeException(ex);
    } finally {
      if (acquireSuccess) {
        counting.release();
      }
    }
  }
}
