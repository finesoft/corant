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
package org.corant.asosat.ddd.pattern.interceptor;

import static org.corant.kernel.util.Preconditions.requireNotNull;
import java.io.Serializable;
import java.util.concurrent.Future;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.corant.asosat.ddd.pattern.concurrent.AsynchronousExecutor;
import org.corant.kernel.exception.GeneralRuntimeException;

/**
 * @author bingo 下午2:05:36
 *
 */
@Interceptor
@Asynchronous
@Priority(Interceptor.Priority.PLATFORM_BEFORE)
public class AsynchronousInterceptor implements Serializable {

  private static final long serialVersionUID = -9213937740307059136L;

  @Inject
  transient AsynchronousExecutor executor;

  @AroundInvoke
  public Object asyncInvocation(final InvocationContext ctx) throws Exception {
    Class<?> returnType =
        requireNotNull(ctx, PkgMsgCds.ERR_ASYNC_CTX_NULL).getMethod().getReturnType();
    final Asynchronous async = ctx.getMethod().getDeclaredAnnotation(Asynchronous.class);
    final boolean fair = async.fair();
    if (returnType.equals(Void.TYPE)) {
      if (fair) {
        executor.submit(() -> run(ctx), true);
      } else {
        executor.runAsync(() -> run(ctx));
      }
      return null;
    } else if (Future.class.isAssignableFrom(returnType)) {
      if (fair) {
        return executor.submit(() -> complete(ctx), true);
      } else {
        return executor.supplyAsync(() -> complete(ctx));
      }
    } else {
      if (fair) {
        return executor.submit(() -> supply(ctx), true).get();
      } else {
        return executor.supplyAsync(() -> supply(ctx)).get();
      }
    }
  }

  protected Object complete(final InvocationContext ctx) {
    try {
      Object result = ctx.proceed();
      return result == null ? null : ((Future<?>) result).get();
    } catch (Exception e) {
      if (e instanceof GeneralRuntimeException) {
        throw (GeneralRuntimeException) e;
      } else {
        throw new GeneralRuntimeException(e, PkgMsgCds.ERR_ASYNC_EXE);
      }
    }
  }

  protected void run(final InvocationContext ctx) {
    try {
      ctx.proceed();
    } catch (Exception e) {
      if (e instanceof GeneralRuntimeException) {
        throw (GeneralRuntimeException) e;
      } else {
        throw new GeneralRuntimeException(e, PkgMsgCds.ERR_ASYNC_EXE);
      }
    }
  }

  protected Object supply(final InvocationContext ctx) {
    try {
      return ctx.proceed();
    } catch (Exception e) {
      if (e instanceof GeneralRuntimeException) {
        throw (GeneralRuntimeException) e;
      } else {
        throw new GeneralRuntimeException(e, PkgMsgCds.ERR_ASYNC_EXE);
      }
    }
  }

}
