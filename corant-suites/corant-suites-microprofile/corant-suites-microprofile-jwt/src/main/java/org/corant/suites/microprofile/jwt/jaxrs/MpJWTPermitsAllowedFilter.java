/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.suites.microprofile.jwt.jaxrs;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import org.corant.suites.microprofile.jwt.impl.MpJWTPermitsAuthorizer;

/**
 * corant-suites-mp-jwt
 *
 * @author bingo 下午7:52:30
 *
 */
@Priority(Priorities.AUTHORIZATION)
public class MpJWTPermitsAllowedFilter implements ContainerRequestFilter {

  public static final String PERMIT_ALL_ROLES = "*";

  private final String[] allowedPermits;

  @Inject
  MpJWTPermitsAuthorizer authorizer;

  public MpJWTPermitsAllowedFilter(String... allowedPermits) {
    this.allowedPermits = allowedPermits;
  }

  @Override
  public void filter(ContainerRequestContext requestContext) {
    if (!authorizer.isAllowed(requestContext, allowedPermits)) {
      if (requestContext.getSecurityContext().getUserPrincipal() == null) {
        Object ex = requestContext.getProperty(MpJWTAuthenticationFilter.JTW_EXCEPTION_KEY);
        if (ex instanceof Exception) {
          requestContext.removeProperty(MpJWTAuthenticationFilter.JTW_EXCEPTION_KEY);
          throw new NotAuthorizedException((Exception) ex, "Bearer");
        } else {
          throw new NotAuthorizedException("Bearer");
        }
      } else {
        throw new ForbiddenException();
      }
    }
  }
}
