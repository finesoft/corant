/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.corant.suites.jcache.caffeine;

import javax.cache.spi.CachingProvider;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.emptySet;

public class CacheProviderBean implements Bean<CachingProvider>, PassivationCapable {
  private final Set<Type> types;
  private final Set<Annotation> qualifiers;
  private final CachingProvider provider;
  private final String id;

  public CacheProviderBean(final CachingProvider cacheManager) {
    provider = cacheManager;
    id = getClass().getName() + "-" + hashCode();

    types = new HashSet<Type>();
    types.add(CachingProvider.class);
    types.add(Object.class);

    qualifiers = new HashSet<Annotation>();
    qualifiers.add(Default.Literal.INSTANCE);
    qualifiers.add(Any.Literal.INSTANCE);
  }

  @Override
  public CachingProvider create(
      final CreationalContext<CachingProvider> cacheManagerCreationalContext) {
    return provider;
  }

  @Override
  public void destroy(
      final CachingProvider cacheProvider,
      final CreationalContext<CachingProvider> cacheManagerCreationalContext) {
    provider.close();
  }

  @Override
  public Class<?> getBeanClass() {
    return CachingProvider.class;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public Set<InjectionPoint> getInjectionPoints() {
    return emptySet();
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public Set<Annotation> getQualifiers() {
    return qualifiers;
  }

  @Override
  public Class<? extends Annotation> getScope() {
    return ApplicationScoped.class;
  }

  @Override
  public Set<Class<? extends Annotation>> getStereotypes() {
    return emptySet();
  }

  @Override
  public Set<Type> getTypes() {
    return types;
  }

  @Override
  public boolean isAlternative() {
    return false;
  }

  @Override
  public boolean isNullable() {
    return false;
  }
}
