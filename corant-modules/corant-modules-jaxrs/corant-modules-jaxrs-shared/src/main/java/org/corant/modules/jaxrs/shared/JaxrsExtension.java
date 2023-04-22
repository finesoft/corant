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
package org.corant.modules.jaxrs.shared;

import static org.corant.context.Beans.isSessionBean;
import static org.corant.shared.util.Classes.getUserClass;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.WithAnnotations;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ext.Provider;
import org.corant.context.proxy.ProxyUtils;
import org.corant.context.required.RequiredExtension;

/**
 * corant-modules-jaxrs-shared
 *
 * @author bingo 下午4:25:16
 *
 */
public class JaxrsExtension implements Extension {

  static Logger logger = Logger.getLogger(JaxrsExtension.class.getCanonicalName());

  private final Set<Class<?>> providers = new HashSet<>();
  private final Set<Class<?>> resources = new HashSet<>();

  public boolean containsProvider(Class<?> providerClass) {
    return providers.contains(getUserClass(providerClass));
  }

  public boolean containsResource(Class<?> resourceClass) {
    return resources.contains(getUserClass(resourceClass));
  }

  public Set<Class<?>> getProviders() {
    return Collections.unmodifiableSet(providers);
  }

  public Set<Class<?>> getResources() {
    return Collections.unmodifiableSet(resources);
  }

  protected <T> void processProviders(
      @WithAnnotations({Provider.class}) @Observes ProcessAnnotatedType<T> event,
      BeanManager beanManager) {
    final AnnotatedType<T> annotatedType = event.getAnnotatedType();
    final Class<T> clazz = annotatedType.getJavaClass();
    if (!clazz.isInterface() && !isSessionBean(annotatedType)
        && !ProxyUtils.isCDIUnproxyableClass(clazz)) {
      if (!RequiredExtension.isVetoed(clazz)) {
        providers.add(clazz);
        logger.fine(() -> String.format("Find a jaxrs provider %s", clazz));
      } else {
        logger.info(() -> String.format("Veto a jaxrs provider %s", clazz));
      }
    } else {
      logger.info(() -> String.format("Give up the jaxrs provider %s", clazz));
    }
  }

  protected <T> void proessResources(
      @WithAnnotations({Path.class}) @Observes ProcessAnnotatedType<T> event,
      BeanManager beanManager) {
    final AnnotatedType<T> annotatedType = event.getAnnotatedType();
    final Class<T> clazz = annotatedType.getJavaClass();
    if (!clazz.isInterface() && !isSessionBean(annotatedType)) {
      if (!RequiredExtension.isVetoed(clazz)) {
        resources.add(clazz);
        logger.fine(() -> String.format("Find a jaxrs resource %s", clazz));
      } else {
        logger.info(() -> String.format("Veto a jaxrs resource %s", clazz));
      }
    } else {
      logger.info(() -> String.format("Give up the jaxrs resource %s", clazz));
    }
  }

}
