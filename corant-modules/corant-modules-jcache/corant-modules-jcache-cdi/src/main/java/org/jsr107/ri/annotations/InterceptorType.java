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


/**
 * Possible types of cache related advice
 *
 * @author Eric Dalquist
 * @since 1.0
 */
public enum InterceptorType {
  /**
   * {@link javax.cache.annotation.CacheResult} advice
   */
  CACHE_RESULT,
  /**
   * {@link javax.cache.annotation.CachePut} advice
   */
  CACHE_PUT,
  /**
   * {@link javax.cache.annotation.CacheRemove} advice
   */
  CACHE_REMOVE_ENTRY,
  /**
   * {@link javax.cache.annotation.CacheRemoveAll} advice
   */
  CACHE_REMOVE_ALL;
}
