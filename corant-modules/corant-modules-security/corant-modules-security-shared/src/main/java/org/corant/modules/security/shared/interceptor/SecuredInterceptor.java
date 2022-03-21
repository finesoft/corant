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
  protected Instance<SecuredInterceptorCallback> callbacks;

  @Inject
  @Any
  protected Instance<SecuredInterceptorHepler> helpers;

  @AroundInvoke
  @AroundConstruct
  public Object secured(InvocationContext invocationContext) throws Exception {
    boolean success = false;
    try {
      check(invocationContext);
      success = true;
      return invocationContext.proceed();
    } finally {
      if (!callbacks.isUnsatisfied()) {
        final boolean sus = success;
        callbacks.stream().sorted(Sortable::compare).forEach(cb -> cb.postSecuredCheck(sus));
      }
    }
  }

  protected void check(InvocationContext invocationContext) throws Exception {
    Secured secured = getInterceptorAnnotation(invocationContext, Secured.class);
    if (secured != null) {
      SecuredMetadata meta = SecurityExtension.getSecuredMetadata(secured);
      if (meta.denyAll()) {
        throw new AuthorizationException(SecurityMessageCodes.UNAUTHZ_ACCESS);
      }
      final SecuredInterceptorHepler helper = getHelper();
      if (isNotBlank(secured.runAs())) {
        helper.handleRunAs(secured.runAs());
      } else if (securityManagers.isUnsatisfied()) {
        if (SecurityExtension.DENY_ALL_NO_SECURITY_MANAGER) {
          throw new AuthorizationException(SecurityMessageCodes.UNAUTHZ_ACCESS);
        }
      } else {
        securityManagers.get().checkAccess(SecurityContexts.getCurrent(),
            meta.type() == SecuredType.ROLE ? helper.resolveAllowedRole(meta)
                : helper.resolveAllowedPermission(meta));
      }
    }
  }

  protected SecuredInterceptorHepler getHelper() {
    if (helpers.isUnsatisfied()) {
      return SecuredInterceptorHepler.DEFAULT_INST;
    } else {
      return helpers.stream().sorted(Sortable::compare).findFirst().get();
    }
  }

}
