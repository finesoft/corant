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
import org.corant.modules.security.shared.SimplePermissions;
import org.corant.modules.security.shared.SimpleRoles;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午12:35:07
 *
 */
@Interceptor
@Secured
public class SecuredInterceptor extends AbstractInterceptor {

  protected static final SecurityManager[] emptySecurityManagers = {};

  @Inject
  @Any
  protected Instance<SecurityManager> securityManagers;

  @AroundInvoke
  @AroundConstruct
  public Object secured(InvocationContext invocationContext) throws Exception {
    SecurityManager[] sms = emptySecurityManagers;
    try {
      sms = check(invocationContext);
      return invocationContext.proceed();
    } finally {
      for (SecurityManager sm : sms) {
        sm.postCheckAccess();
      }
    }
  }

  protected SecurityManager[] check(InvocationContext invocationContext) throws Exception {
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
          return emptySecurityManagers;
        }
      }
      if (SecuredType.valueOf(meta.type()) == SecuredType.ROLE) {
        return checkAccess(SimpleRoles.of(meta.allowed()));
      } else {
        return checkAccess(SimplePermissions.of(meta.allowed()));
      }
    }
    return emptySecurityManagers;
  }

  protected SecurityManager[] checkAccess(Object rolesOrPerms) {
    SecurityContext sctx = SecurityContexts.getCurrent();
    if (securityManagers.isResolvable()) {
      SecurityManager used = securityManagers.get();
      if (!used.testAccess(sctx, rolesOrPerms)) {
        throw new AuthorizationException(SecurityMessageCodes.UNAUTHZ_ACCESS);
      }
      return new SecurityManager[] {used};
    } else if (SecurityExtension.FIT_ANY_SECURITY_MANAGER) {
      SecurityManager used = securityManagers.stream().filter(sm -> sm.testAccess(sm, rolesOrPerms))
          .findFirst().orElse(null);
      if (used == null) {
        throw new AuthorizationException(SecurityMessageCodes.UNAUTHZ_ACCESS);
      }
      return new SecurityManager[] {used};
    } else {
      SecurityManager[] used = securityManagers.stream().toArray(SecurityManager[]::new);
      for (SecurityManager sm : used) {
        if (!sm.testAccess(sctx, rolesOrPerms)) {
          throw new AuthorizationException(SecurityMessageCodes.UNAUTHZ_ACCESS);
        }
      }
      return used;
    }
  }

}
