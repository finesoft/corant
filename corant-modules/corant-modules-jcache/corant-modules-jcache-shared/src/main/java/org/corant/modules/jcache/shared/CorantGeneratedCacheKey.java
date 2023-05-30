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

import java.util.Arrays;
import java.util.function.Function;
import javax.cache.annotation.GeneratedCacheKey;

/**
 * corant-modules-jcache-shared
 *
 * @author bingo 下午8:32:24
 *
 */
public class CorantGeneratedCacheKey implements GeneratedCacheKey {

  private static final long serialVersionUID = 9179253007871444947L;

  private final Object[] parameters;
  private final int hashCode;

  public CorantGeneratedCacheKey(Object[] parameters) {
    this.parameters = parameters;
    hashCode = Arrays.deepHashCode(parameters);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    if (hashCode != obj.hashCode()) {
      return false;
    }
    CorantGeneratedCacheKey other = (CorantGeneratedCacheKey) obj;
    return Arrays.deepEquals(parameters, other.parameters);
  }

  public <T> T get(Function<Object[], T> converter) {
    return converter.apply(parameters);
  }

  @SuppressWarnings("unchecked")
  public <T> T get(int i) {
    return (T) parameters[i];
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  Object[] parameters() {
    return Arrays.copyOf(parameters, parameters.length);
  }
}
