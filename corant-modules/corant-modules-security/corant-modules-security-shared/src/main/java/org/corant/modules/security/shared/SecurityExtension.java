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
package org.corant.modules.security.shared;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.Priority;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.annotation.security.RunAs;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterTypeDiscovery;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.WithAnnotations;
import org.corant.config.Configs;
import org.corant.modules.security.annotation.Secured;
import org.corant.modules.security.annotation.Secured.SecuredLiteral;
import org.corant.modules.security.annotation.SecuredMetadata;
import org.corant.modules.security.shared.interceptor.SecuredInterceptor;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Priorities;

/**
 * corant-modules-security-shared
 *
 *
 * @author bingo 下午3:45:33
 */
public class SecurityExtension implements Extension {

  public static final boolean CACHE_FILTER_HANDLERS =
      Configs.getValue("corant.security.filter.cache-handler", Boolean.class, Boolean.TRUE);
  public static final boolean ENABLE_INTERCEPTOR =
      Configs.getValue("corant.security.interceptor.enable", Boolean.class, Boolean.FALSE);
  public static final boolean ENABLE_INTERCEPTOR_COMPATIBILITY = Configs
      .getValue("corant.security.interceptor.compatibility", Boolean.class, ENABLE_INTERCEPTOR);
  public static final boolean DENY_ALL_NO_SECURITY_MANAGER = Configs.getValue(
      "corant.security.interceptor.deny-all-if-no-security-manager", Boolean.class, Boolean.FALSE);

  protected static final Map<Secured, SecuredMetadata> securedMetaDatas = new ConcurrentHashMap<>();

  protected static volatile boolean securedMetaDatasInit = false;

  public static SecuredMetadata getSecuredMetadata(Secured secured) {
    if (!securedMetaDatasInit) {
      throw new CorantRuntimeException("The secured metadata not ready yet!");
    }
    return secured == null ? SecuredMetadata.ALLOW_ALL_INST
        : securedMetaDatas.computeIfAbsent(secured, SecuredMetadata::new);
  }

  public static Map<Secured, SecuredMetadata> getSecuredMetadatas() {
    if (!securedMetaDatasInit) {
      throw new CorantRuntimeException("The secured metadata not ready yet!");
    }
    return Collections.unmodifiableMap(securedMetaDatas);
  }

  protected void onAfterBeanDiscovery(@Observes AfterBeanDiscovery beforeBeanDiscoveryEvent) {
    if (!securedMetaDatasInit) {
      synchronized (SecurityExtension.class) {
        if (!securedMetaDatasInit) {
          securedMetaDatasInit = true;
        }
      }
    }
  }

  protected void onAfterTypeDiscovery(@Observes AfterTypeDiscovery afterTypeDiscovery) {
    if (ENABLE_INTERCEPTOR) {
      afterTypeDiscovery.getInterceptors().add(SecuredInterceptor.class);
    }
  }

  protected void onBeforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscoveryEvent) {
    if (ENABLE_INTERCEPTOR) {
      beforeBeanDiscoveryEvent.addInterceptorBinding(Secured.class);
    }
  }

  protected void onProcessAnnotatedType(
      @Observes @Priority(Priorities.FRAMEWORK_HIGHER) @WithAnnotations({Secured.class,
          DenyAll.class, PermitAll.class, RolesAllowed.class,
          RunAs.class}) ProcessAnnotatedType<?> event) {
    Class<?> beanClass = event.getAnnotatedType().getJavaClass();
    if (!beanClass.isInterface() && !Modifier.isAbstract(beanClass.getModifiers())
        && ENABLE_INTERCEPTOR) {
      event.configureAnnotatedType().methods().forEach(m -> {
        Secured secured = m.getAnnotated().getAnnotation(Secured.class);
        if (secured != null) {
          securedMetaDatas.computeIfAbsent(secured, SecuredMetadata::new);
        }
      });
      event.configureAnnotatedType().filterConstructors(c -> c.isAnnotationPresent(Secured.class))
          .forEach(c -> {
            Secured secured = c.getAnnotated().getAnnotation(Secured.class);
            securedMetaDatas.computeIfAbsent(secured, SecuredMetadata::new);
          });
      if (event.configureAnnotatedType().getAnnotated().getAnnotation(Secured.class) != null) {
        securedMetaDatas.computeIfAbsent(
            event.configureAnnotatedType().getAnnotated().getAnnotation(Secured.class),
            SecuredMetadata::new);
      }
      if (ENABLE_INTERCEPTOR_COMPATIBILITY) {
        processPermitAll(event);
        processRolesAllowed(event);
        processRunAs(event);
        processDenyAll(event);
      }
    }
  }

  protected void processDenyAll(ProcessAnnotatedType<?> event) {
    event.configureAnnotatedType().filterMethods(m -> m.isAnnotationPresent(DenyAll.class))
        .forEach(m -> {
          Secured secured = SecuredLiteral.of(m.getAnnotated().getAnnotation(DenyAll.class));
          securedMetaDatas.computeIfAbsent(secured, SecuredMetadata::new);
          m.add(secured);
        });
    if (event.configureAnnotatedType().getAnnotated().getAnnotation(DenyAll.class) != null) {
      Secured secured = SecuredLiteral
          .of(event.configureAnnotatedType().getAnnotated().getAnnotation(DenyAll.class));
      securedMetaDatas.computeIfAbsent(secured, SecuredMetadata::new);
      event.configureAnnotatedType().add(secured);
    }
  }

  protected void processPermitAll(ProcessAnnotatedType<?> event) {
    event.configureAnnotatedType().filterMethods(m -> m.isAnnotationPresent(PermitAll.class))
        .forEach(m -> {
          Secured secured = SecuredLiteral.of(m.getAnnotated().getAnnotation(PermitAll.class));
          securedMetaDatas.computeIfAbsent(secured, SecuredMetadata::new);
          m.add(secured);
        });
    if (event.configureAnnotatedType().getAnnotated().getAnnotation(PermitAll.class) != null) {
      Secured secured = SecuredLiteral
          .of(event.configureAnnotatedType().getAnnotated().getAnnotation(PermitAll.class));
      securedMetaDatas.computeIfAbsent(secured, SecuredMetadata::new);
      event.configureAnnotatedType().add(secured);
    }
  }

  protected void processRolesAllowed(ProcessAnnotatedType<?> event) {
    event.configureAnnotatedType().filterMethods(m -> m.isAnnotationPresent(RolesAllowed.class))
        .forEach(m -> {
          Secured secured = SecuredLiteral.of(m.getAnnotated().getAnnotations(RolesAllowed.class));
          securedMetaDatas.computeIfAbsent(secured, SecuredMetadata::new);
          m.add(secured);
        });
    if (event.configureAnnotatedType().getAnnotated().getAnnotation(RolesAllowed.class) != null) {
      Secured secured = SecuredLiteral
          .of(event.configureAnnotatedType().getAnnotated().getAnnotations(RolesAllowed.class));
      securedMetaDatas.computeIfAbsent(secured, SecuredMetadata::new);
      event.configureAnnotatedType().add(secured);
    }
  }

  protected void processRunAs(ProcessAnnotatedType<?> event) {
    if (event.configureAnnotatedType().getAnnotated().getAnnotation(RunAs.class) != null) {
      Secured secured = SecuredLiteral
          .of(event.configureAnnotatedType().getAnnotated().getAnnotation(RunAs.class));
      securedMetaDatas.computeIfAbsent(secured, SecuredMetadata::new);
      event.configureAnnotatedType().add(secured);
    }
  }
}
