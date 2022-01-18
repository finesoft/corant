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

import static org.corant.context.Beans.findNamed;
import static org.corant.context.concurrent.ConcurrentExtension.ENABLE_ASYNC_INTERCEPTOR_CFG;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.corant.context.AbstractInterceptor;
import org.corant.context.concurrent.AsynchronousConfig;
import org.corant.context.concurrent.ConcurrentExtension;
import org.corant.context.concurrent.annotation.Asynchronous;
import org.corant.shared.retry.AsynchronousRetryer;
import org.corant.shared.service.RequiredConfiguration;
import org.corant.shared.service.RequiredConfiguration.ValuePredicate;

/**
 * corant-context
 *
 * @author bingo 上午9:50:43
 *
 */
@Interceptor
@Asynchronous
@Priority(Interceptor.Priority.PLATFORM_BEFORE)
@RequiredConfiguration(key = ENABLE_ASYNC_INTERCEPTOR_CFG, predicate = ValuePredicate.EQ,
    type = Boolean.class, value = "true")
public class AsynchronousInterceptor extends AbstractInterceptor {

  protected static final Logger logger =
      Logger.getLogger(AsynchronousInterceptor.class.getCanonicalName());

  @Inject
  ConcurrentExtension extension;

  @AroundInvoke
  public Object asynchronousInvocation(final InvocationContext ctx) throws Exception {
    final Asynchronous async = getInterceptorAnnotation(ctx, Asynchronous.class);
    final AsynchronousConfig config = extension.getAsynchronousConfig(async);
    if (config.isRetry()) {
      return execute(createCallable(ctx),
          findNamed(ManagedScheduledExecutorService.class, async.executor()).orElseThrow(), config,
          ctx.getMethod().getReturnType());
    } else {
      return execute(createCallable(ctx),
          findNamed(ManagedExecutorService.class, async.executor()).orElseThrow(),
          ctx.getMethod().getReturnType());
    }
  }

  protected Callable<Object> createCallable(InvocationContext ctx) {
    return () -> {
      try {
        return ctx.proceed();
      } catch (Throwable t) {
        logger.log(Level.WARNING, t,
            () -> String.format("Asynchronous invocation %s occurred error!", ctx.getMethod()));
        throw t;
      }
    };
  }

  protected Object execute(Callable<Object> task, ManagedExecutorService executor,
      Class<?> returnType) {
    if (Future.class.isAssignableFrom(returnType)) {
      return executor.submit(() -> ((Future<?>) task.call()).get());
    } else {
      executor.submit(task);
      return null;
    }
  }

  protected Object execute(Callable<Object> task, ManagedScheduledExecutorService executor,
      AsynchronousConfig config, Class<?> returnType) {
    if (Future.class.isAssignableFrom(returnType)) {
      return new AsynchronousRetryer(executor).retryStrategy(config.getRetryStrategy())
          .backoffStrategy(config.getBackoffStrategy())
          .execute((Callable<?>) () -> ((Future<?>) task.call()).get());
    } else {
      new AsynchronousRetryer(executor).retryStrategy(config.getRetryStrategy())
          .backoffStrategy(config.getBackoffStrategy()).execute(task);
      return null;
    }
  }
}
