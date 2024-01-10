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

import javax.cache.annotation.GeneratedCacheKey;
import java.util.Arrays;

/**
 * Default cache key implementation. Keeps a reference to a copy of the entire parameter array from
 * the intercepted invocation and uses {@link Arrays#deepHashCode(Object[])} to
 * implement {@link #hashCode()} and {@link Arrays#deepEquals(Object[], Object[])} to implement
 * {@link #equals(Object)}
 * <p>
 * IMPORTANT: This implementation assumes that the entire object graph passed in as the parameters Object[]
 * is immutable. The value returned by {@link #hashCode()} is calculated in the constructor.
 * </p>
 * @author Eric Dalquist
 * @since 1.0
 */
public class DefaultGeneratedCacheKey implements GeneratedCacheKey {

  private static final long serialVersionUID = 1L;

  private final Object[] parameters;
  private final int hashCode;

  /**
   * Constructs a default cache key
   *
   * @param parameters the paramters to use
   */
  public DefaultGeneratedCacheKey(Object[] parameters) {
    this.parameters = parameters;
    this.hashCode = Arrays.deepHashCode(parameters);
  }

  @Override
  public int hashCode() {
    return this.hashCode;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    if (this.hashCode != obj.hashCode())
      return false;
    DefaultGeneratedCacheKey other = (DefaultGeneratedCacheKey) obj;
    return Arrays.deepEquals(this.parameters, other.parameters);
  }
}
