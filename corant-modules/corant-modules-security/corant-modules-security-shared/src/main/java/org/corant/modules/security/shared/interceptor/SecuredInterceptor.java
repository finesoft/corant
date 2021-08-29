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
import java.lang.annotation.Annotation;
import java.util.Set;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.corant.context.Contexts;
import org.corant.context.security.SecurityContexts;
import org.corant.modules.security.SecurityManager;
import org.corant.modules.security.annotation.Secured;
import org.corant.modules.security.annotation.SecuredType;
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
public class SecuredInterceptor {

  @Inject
  @Any
  protected Instance<SecurityManager> securityManagers;

  @AroundInvoke
  public Object secured(InvocationContext invocationContext) throws Exception {
    check(invocationContext);
    return invocationContext.proceed();
  }

  protected void check(InvocationContext invocationContext) throws Exception {
    Secured secured = extract(invocationContext);
    if (secured != null) {
      SecurityManager sm = securityManagers.get();
      if (isEmpty(secured.allowed())) {
        sm.checkAuthenticated();
      } else if (secured.type() == SecuredType.ROLE) {
        sm.checkAccess(SecurityContexts.getCurrent(), SimpleRoles.of(secured.allowed()));
      } else {
        sm.checkAccess(SecurityContexts.getCurrent(), SimplePermissions.of(secured.allowed()));
      }
    }
  }

  @SuppressWarnings("unchecked")
  protected Secured extract(InvocationContext ic) {
    if (ic.getContextData().containsKey(Contexts.WELD_INTERCEPTOR_BINDINGS_KEY)) {
      Set<Annotation> annotationBindings =
          (Set<Annotation>) ic.getContextData().get(Contexts.WELD_INTERCEPTOR_BINDINGS_KEY);
      for (Annotation annotation : annotationBindings) {
        if (annotation.annotationType() == Secured.class) {
          return (Secured) annotation;
        }
      }
    }
    return null;
  }
}
