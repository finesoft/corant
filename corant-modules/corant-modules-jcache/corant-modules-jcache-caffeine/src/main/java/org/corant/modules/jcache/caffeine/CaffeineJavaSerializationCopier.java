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
package org.corant.modules.jcache.caffeine;

import static org.corant.shared.util.Sets.immutableSetBuilder;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.corant.config.Configs;
import org.corant.config.CorantConfigResolver;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.ubiquity.Tuple.Range;
import org.corant.shared.ubiquity.Tuple.Triple;
import org.corant.shared.util.Classes;
import com.github.benmanes.caffeine.jcache.copy.JavaSerializationCopier;

/**
 * corant-modules-jcache-caffeine
 *
 * <p>
 *
 * <pre>
 * corant.jcache.caffeine.*.store-by-value.strategy = org.corant.modules.jcache.caffeine.CaffeineJavaSerializationCopier
 * corant.jcache.caffeine.*.store-by-value.custom-immutable-classes = A,B,C,D
 * </pre>
 *
 *
 * @author bingo 下午6:15:34
 */
public class CaffeineJavaSerializationCopier extends JavaSerializationCopier {

  @SuppressWarnings("rawtypes")
  protected final Set<Class> customImmutableClasses = immutableSetBuilder(Configs
      .searchValues(
          CaffeineJCacheExtension.CORANT_CAFFE_PREFIX + "*store-by-value.custom-immutable-classes")
      .map(CorantConfigResolver::splitValue).flatMap(Arrays::stream).map(Classes::asClass)
      .toArray(Class[]::new)).addAll(Pair.class, Triple.class, Range.class).build();

  public CaffeineJavaSerializationCopier() {}

  public CaffeineJavaSerializationCopier(Set<Class<?>> immutableClasses,
      Map<Class<?>, Function<Object, Object>> deepCopyStrategies) {
    super(immutableClasses, deepCopyStrategies);
  }

  @Override
  protected boolean isImmutable(Class<?> clazz) {
    return customImmutableClasses.contains(clazz) || super.isImmutable(clazz);
  }

}
