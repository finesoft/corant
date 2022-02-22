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

import static org.corant.shared.util.Lists.appendIfAbsent;
import java.lang.reflect.Modifier;
import javax.annotation.Priority;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.annotation.security.RunAs;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterTypeDiscovery;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import org.corant.config.Configs;
import org.corant.modules.security.annotation.Secure;
import org.corant.modules.security.annotation.Secured;
import org.corant.modules.security.annotation.Secured.SecuredLiteral;
import org.corant.modules.security.annotation.SecuredType;
import org.corant.modules.security.shared.interceptor.SecuredInterceptor;
import org.corant.shared.normal.Priorities;
import org.corant.shared.util.Strings;

/**
 * corant-modules-security-shared
 *
 *
 * @author bingo 下午3:45:33
 *
 */
public class SecurityExtension implements Extension {

  public static final boolean CACHE_FILTER_HANDLERS =
      Configs.getValue("corant.security.filter.cache-handler", Boolean.class, Boolean.TRUE);
  public static final boolean ENABLE_INTERCEPTOR =
      Configs.getValue("corant.security.interceptor.enable", Boolean.class, Boolean.FALSE);
  public static final boolean ENABLE_INTERCEPTOR_COMPATIBILITY = Configs
      .getValue("corant.security.interceptor.compatibility", Boolean.class, ENABLE_INTERCEPTOR);
  public static final boolean DENY_ALL_NO_SECURITY_MANAGER = Configs.getValue(
      "corant.security.interceptor.deny-all-if-no-sucrity-manager", Boolean.class, Boolean.TRUE);
  public static final boolean FIT_ANY_SECURITY_MANAGER = Configs
      .getValue("corant.security.interceptor.fit-any-sucrity-manager", Boolean.class, Boolean.TRUE);

  void onAfterTypeDiscovery(@Observes AfterTypeDiscovery afterTypeDiscovery) {
    if (ENABLE_INTERCEPTOR) {
      afterTypeDiscovery.getInterceptors().add(SecuredInterceptor.class);
    }
  }

  void onBeforeBeanDiscoveryEvent(@Observes BeforeBeanDiscovery beforeBeanDiscoveryEvent) {
    if (ENABLE_INTERCEPTOR) {
      beforeBeanDiscoveryEvent.addInterceptorBinding(Secured.class);
    }
  }

  void onProcessAnnotatedType(@Observes @Priority(Priorities.FRAMEWORK_HIGHER) @WithAnnotations({
      Secured.class, Secure.class}) ProcessAnnotatedType<?> event) {
    if (ENABLE_INTERCEPTOR_COMPATIBILITY) {
      Class<?> beanClass = event.getAnnotatedType().getJavaClass();
      if (!beanClass.isInterface() && !Modifier.isAbstract(beanClass.getModifiers())) {
        processPermitAll(event);
        processRolesAllowed(event);
        processRunAs(event);
      }
    }
  }

  void processPermitAll(ProcessAnnotatedType<?> event) {
    event.configureAnnotatedType().filterMethods(m -> m.getAnnotation(PermitAll.class) != null)
        .forEach(m -> m
            .add(new SecuredLiteral(SecuredType.ROLE.name(), Strings.EMPTY, Strings.EMPTY_ARRAY)));
  }

  void processRolesAllowed(ProcessAnnotatedType<?> event) {
    event.configureAnnotatedType().filterMethods(m -> m.isAnnotationPresent(RolesAllowed.class))
        .forEach(m -> {
          String[] roles = Strings.EMPTY_ARRAY;
          for (RolesAllowed r : m.getAnnotated().getAnnotations(RolesAllowed.class)) {
            roles = appendIfAbsent(roles, r.value());
          }
          m.add(new SecuredLiteral(SecuredType.ROLE.name(), Strings.EMPTY, roles));
        });
  }

  void processRunAs(ProcessAnnotatedType<?> event) {
    event.configureAnnotatedType().filterMethods(m -> m.getAnnotation(RunAs.class) != null)
        .forEach(m -> m.add(new SecuredLiteral(SecuredType.ROLE.name(),
            m.getAnnotated().getAnnotation(RunAs.class).value(), Strings.EMPTY_ARRAY)));
  }
}
