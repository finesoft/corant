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
package org.corant.modules.security.shared.interceptor;

import static org.corant.shared.util.Strings.isNotBlank;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.corant.context.AbstractInterceptor;
import org.corant.context.security.SecurityContext;
import org.corant.context.security.SecurityContexts;
import org.corant.modules.security.AuthorizationException;
import org.corant.modules.security.SecurityManager;
import org.corant.modules.security.SecurityMessageCodes;
import org.corant.modules.security.annotation.Secured;
import org.corant.modules.security.annotation.SecuredMetadata;
import org.corant.modules.security.annotation.SecuredType;
import org.corant.modules.security.shared.SecurityExtension;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午12:35:07
 *
 */
@Interceptor
@Secured
public class SecuredInterceptor extends AbstractInterceptor {

  @Inject
  @Any
  protected Instance<SecurityManager> securityManagers;

  @Inject
  @Any
  protected Instance<SecuredInterceptorHepler> assistant;

  @AroundInvoke
  @AroundConstruct
  public Object secured(InvocationContext invocationContext) throws Exception {
    check(invocationContext);
    return invocationContext.proceed();
  }

  protected void check(InvocationContext invocationContext) throws Exception {
    Secured secured = getInterceptorAnnotation(invocationContext, Secured.class);
    if (secured != null) {
      SecuredMetadata meta = SecurityExtension.getSecuredMetadata(secured);
      if (meta.denyAll()) {
        throw new AuthorizationException(SecurityMessageCodes.UNAUTHZ_ACCESS);
      }
      if (securityManagers.isUnsatisfied()) {
        if (SecurityExtension.DENY_ALL_NO_SECURITY_MANAGER) {
          throw new AuthorizationException(SecurityMessageCodes.UNAUTHZ_ACCESS);
        } else {
          return;
        }
      }
      if (isNotBlank(secured.runAs())) {
        getAssistant().handleRunAs(secured.runAs());
      } else {
        final SecuredInterceptorHepler allowsResolver = getAssistant();
        if (meta.type() == SecuredType.ROLE) {
          checkAccess(allowsResolver.resolveAllowedRole(meta));
        } else {
          checkAccess(allowsResolver.resolveAllowedPermission(meta));
        }
      }
    }
  }

  protected void checkAccess(Object rolesOrPerms) {
    SecurityContext sctx = SecurityContexts.getCurrent();
    if (securityManagers.isResolvable()) {
      if (!securityManagers.get().testAccess(sctx, rolesOrPerms)) {
        throw new AuthorizationException(SecurityMessageCodes.UNAUTHZ_ACCESS);
      }
    } else if (SecurityExtension.FIT_ANY_SECURITY_MANAGER) {
      if (securityManagers.stream().sorted(Sortable::compare)
          .noneMatch(sm -> sm.testAccess(sctx, rolesOrPerms))) {
        throw new AuthorizationException(SecurityMessageCodes.UNAUTHZ_ACCESS);
      }
    } else if (securityManagers.stream().sorted(Sortable::compare)
        .anyMatch(sm -> !sm.testAccess(sctx, rolesOrPerms))) {
      throw new AuthorizationException(SecurityMessageCodes.UNAUTHZ_ACCESS);
    }
  }

  protected SecuredInterceptorHepler getAssistant() {
    return assistant.isResolvable() ? assistant.get() : SecuredInterceptorHepler.DEFAULT_INST;
  }

}
