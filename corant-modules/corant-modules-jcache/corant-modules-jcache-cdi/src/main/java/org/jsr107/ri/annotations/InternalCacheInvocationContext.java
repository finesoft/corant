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

import javax.cache.annotation.CacheInvocationContext;
import java.lang.annotation.Annotation;

/**
 * RI Internal extension of {@link CacheInvocationContext} that provides access to
 * an {@link AbstractStaticCacheInvocationContext}.
 *
 * @param <A> The type of annotation this context information is for. One of {@link javax.cache.annotation.CacheResult},
 *            {@link javax.cache.annotation.CachePut}, {@link javax.cache.annotation.CacheRemove}, or
 *            {@link javax.cache.annotation.CacheRemoveAll}.
 * @author Eric Dalquist
 * @since 1.0
 */
public interface InternalCacheInvocationContext<A extends Annotation> extends CacheInvocationContext<A> {
  /**
   * @return The static cache invocation details for the invoked method
   */
  StaticCacheInvocationContext<A> getStaticCacheInvocationContext();
}
