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
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.corant.kernel.exception.GeneralRuntimeException;
import org.corant.kernel.util.Retries;
import org.corant.shared.util.MethodUtils.MethodSignature;

/**
 * @author bingo 下午3:28:58
 *
 */
@Interceptor
@Retry
@Priority(Interceptor.Priority.PLATFORM_BEFORE)
public class RetryInterceptor implements Serializable {

  private static final long serialVersionUID = 3478803026927779685L;

  static final Map<MethodSignature, Map<Class<?>, String>> THROWINGS = new ConcurrentHashMap<>();

  @AroundInvoke
  public Object retryInvocation(InvocationContext ctx) throws Exception {
    final Retry ann = requireNotNull(ctx, PkgMsgCds.ERR_RETRY_CTX_NULL).getMethod()
        .getDeclaredAnnotation(Retry.class);
    final Map<Class<?>, String> throwingMap = THROWINGS
        .computeIfAbsent(new MethodSignature(ctx.getMethod()), (m) -> Arrays.stream(ann.throwing())
            .collect(Collectors.toMap(RetryThrowing::exception, RetryThrowing::code)));
    return Retries
        .retry(ctx, ann.times(), Duration.ofMillis(ann.intervalMs()),
            ec -> new GeneralRuntimeException(ec,
                throwingMap.getOrDefault(ec.getClass(), PkgMsgCds.ERR_RETRY_DFLT)),
            ann.exceptions());
  }

}
