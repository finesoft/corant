/**
 * Copyright 2011-2013 Terracotta, Inc. Copyright 2011-2013 Oracle America Incorporated
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
package org.jsr107.ri.annotations.cdi;

import javax.cache.annotation.CacheRemove;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.jsr107.ri.annotations.AbstractCacheRemoveEntryInterceptor;

/**
 * Interceptor for {@link javax.cache.annotation.CacheRemove}
 *
 * @author Rick Hightower
 * @author Eric Dalquist
 * @since 1.0
 */
@CacheRemove
@Interceptor
public class CacheRemoveEntryInterceptor
    extends AbstractCacheRemoveEntryInterceptor<InvocationContext> {

  @Inject
  private CacheLookupUtil lookup;

  /**
   * @param invocationContext The intercepted invocation
   * @return The result from {@link InvocationContext#proceed()}
   * @throws Exception likely {@link InvocationContext#proceed()} threw an exception
   */
  @AroundInvoke
  public Object cacheRemoveEntry(InvocationContext invocationContext) throws Exception {
    try {
      return this.cacheRemoveEntry(lookup, invocationContext);
    } catch (Throwable e) {
      if (e instanceof Exception) {
        throw (Exception) e;
      } else {
        throw new Exception(e);
      }
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.jsr107.ri.annotations.AbstractCacheInterceptor#proceed(java.lang.Object)
   */
  @Override
  protected Object proceed(InvocationContext invocation) throws Exception {
    return invocation.proceed();
  }
}