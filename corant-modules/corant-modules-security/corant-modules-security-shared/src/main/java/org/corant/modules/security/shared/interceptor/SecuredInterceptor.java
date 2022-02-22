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

import static org.corant.shared.util.Empties.isEmpty;
import java.util.function.Function;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.corant.context.AbstractInterceptor;
import org.corant.context.security.SecurityContexts;
import org.corant.modules.security.AuthorizationException;
import org.corant.modules.security.SecurityManager;
import org.corant.modules.security.SecurityMessageCodes;
import org.corant.modules.security.annotation.Secured;
import org.corant.modules.security.annotation.Secured.SecuredLiteral;
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

  @Inject
  @Any
  protected Instance<SecurityManager> securityManagers;

  @AroundInvoke
  @AroundConstruct
  public Object secured(InvocationContext invocationContext) throws Exception {
    check(invocationContext);
    return invocationContext.proceed();
  }

  protected void check(InvocationContext invocationContext) throws Exception {
    Secured secured = SecuredLiteral.of(getInterceptorAnnotation(invocationContext, Secured.class));
    if (secured != null) {
      if (securityManagers.isUnsatisfied()) {
        if (SecurityExtension.DENY_ALL_NO_SECURITY_MANAGER) {
          throw new AuthorizationException(SecurityMessageCodes.UNAUTHZ_ACCESS);
        } else {
          return;
        }
      }
      if (isEmpty(secured.allowed())) {
        checkAuthenticated();
      } else if (SecuredType.valueOf(secured.type()) == SecuredType.ROLE) {
        checkAccess(secured, SimpleRoles::of);
      } else {
        checkAccess(secured, SimplePermissions::of);
      }
    }
  }

  protected void checkAccess(Secured secured, Function<String[], Object> predicate) {
    if (securityManagers.isResolvable()) {
      securityManagers.get().checkAccess(SecurityContexts.getCurrent(),
          predicate.apply(secured.allowed()));
    } else if (SecurityExtension.FIT_ANY_SECURITY_MANAGER) {
      if (securityManagers.stream().noneMatch(
          sm -> sm.testAccess(SecurityContexts.getCurrent(), predicate.apply(secured.allowed())))) {
        throw new AuthorizationException(SecurityMessageCodes.UNAUTHZ_ACCESS);
      }
    } else if (!securityManagers.stream().allMatch(
        sm -> sm.testAccess(SecurityContexts.getCurrent(), predicate.apply(secured.allowed())))) {
      throw new AuthorizationException(SecurityMessageCodes.UNAUTHZ_ACCESS);
    }
  }

  protected void checkAuthenticated() {
    if (securityManagers.isResolvable()) {
      securityManagers.get().checkAuthenticated(SecurityContexts.getCurrent());
    } else if (SecurityExtension.FIT_ANY_SECURITY_MANAGER) {
      if (securityManagers.stream()
          .noneMatch(sm -> sm.authenticated(SecurityContexts.getCurrent()))) {
        throw new AuthorizationException(SecurityMessageCodes.UNAUTHZ_ACCESS);
      }
    } else if (!securityManagers.stream()
        .allMatch(sm -> sm.authenticated(SecurityContexts.getCurrent()))) {
      throw new AuthorizationException(SecurityMessageCodes.UNAUTHZ_ACCESS);
    }
  }

}
