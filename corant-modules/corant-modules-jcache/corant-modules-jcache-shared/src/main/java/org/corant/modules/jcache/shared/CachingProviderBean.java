/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.modules.jcache.shared;

import javax.cache.spi.CachingProvider;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.BeanManager;
import org.corant.context.AbstractBean;

/**
 * corant-modules-jcache-shared
 *
 * @author sushuaihao
 *
 */
public class CachingProviderBean extends AbstractBean<CachingProvider> {

  private final CachingProvider provider;

  public CachingProviderBean(BeanManager beanManager, final CachingProvider cachingProvider) {
    super(beanManager);
    provider = cachingProvider;
    types.add(CachingProvider.class);
    types.add(Object.class);
    qualifiers.add(Default.Literal.INSTANCE);
    qualifiers.add(Any.Literal.INSTANCE);
  }

  @Override
  public CachingProvider create(
      final CreationalContext<CachingProvider> cacheManagerCreationalContext) {
    return provider;
  }

  @Override
  public void destroy(final CachingProvider cacheProvider,
      final CreationalContext<CachingProvider> cacheManagerCreationalContext) {
    provider.close();
  }
}
