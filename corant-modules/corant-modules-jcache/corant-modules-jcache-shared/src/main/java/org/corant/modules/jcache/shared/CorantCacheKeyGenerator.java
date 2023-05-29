/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.jcache.shared;

import java.lang.annotation.Annotation;
import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheKeyInvocationContext;
import javax.enterprise.context.ApplicationScoped;

/**
 * corant-modules-jcache-shared
 *
 * @author bingo 下午8:34:12
 *
 */
@ApplicationScoped
public class CorantCacheKeyGenerator implements CacheKeyGenerator {

  /*
   * (non-Javadoc)
   *
   * @see javax.cache.annotation.CacheKeyGenerator#generateCacheKey(javax.cache.annotation.
   * CacheInvocationContext)
   */
  @Override
  public CorantGeneratedCacheKey generateCacheKey(
      CacheKeyInvocationContext<? extends Annotation> cacheKeyInvocationContext) {
    final CacheInvocationParameter[] keyParameters = cacheKeyInvocationContext.getKeyParameters();

    final Object[] parameters = new Object[keyParameters.length];
    for (int index = 0; index < keyParameters.length; index++) {
      parameters[index] = keyParameters[index].getValue();
    }

    return new CorantGeneratedCacheKey(parameters);
  }
}
