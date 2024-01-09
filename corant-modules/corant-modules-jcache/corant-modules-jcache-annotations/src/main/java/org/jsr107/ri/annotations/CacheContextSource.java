/**
 *  Copyright 2011-2013 Terracotta, Inc.
 *  Copyright 2011-2013 Oracle America Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.jsr107.ri.annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;


/**
 * Defines the API for looking up information about an invocation.
 *
 * @param <I> The intercepted method invocation
 * @author Eric Dalquist
 * @since 1.0
 */
public interface CacheContextSource<I> {

  /**
   * Get information about an invocation annotated {@link javax.cache.annotation.CacheResult},
   * {@link javax.cache.annotation.CachePut}, or {@link javax.cache.annotation.CacheRemove}
   *
   * @param invocation The intercepted invocation
   * @return Information about the invocation
   */
  InternalCacheKeyInvocationContext<? extends Annotation> getCacheKeyInvocationContext(I invocation);

  /**
   * Get information about an invocation annotated {@link javax.cache.annotation.CacheResult},
   * {@link javax.cache.annotation.CachePut}, {@link javax.cache.annotation.CacheRemove},
   * or {@link javax.cache.annotation.CacheRemoveAll}
   *
   * @param invocation The intercepted invocation
   * @return Information about the invocation
   */
  InternalCacheInvocationContext<? extends Annotation> getCacheInvocationContext(I invocation);

  /**
   * Get static information about a method annotated with {@link javax.cache.annotation.CacheResult},
   * {@link javax.cache.annotation.CachePut}, {@link javax.cache.annotation.CacheRemove},
   * or {@link javax.cache.annotation.CacheRemoveAll}
   *
   * @param method      The annotated method
   * @param targetClass The Class that will be targeted with invocations
   * @return Static information about the annotated method
   */
  StaticCacheInvocationContext<? extends Annotation> getMethodDetails(Method method, Class<? extends Object> targetClass);

}
