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

import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Priority;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;
import org.corant.shared.util.StringUtils.WildcardMatcher;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * corant-suites-mp-jwt
 *
 * @author bingo 下午7:52:30
 *
 */
@Priority(Priorities.AUTHORIZATION)
public class MpRolesAllowedFilter implements ContainerRequestFilter {

  public static final String PERMIT_ALL_ROLES = "*";

  private final Set<String> allowedRoles = new HashSet<>();
  private final Set<WildcardMatcher> allowedRoleWildcards = new HashSet<>();
  private final boolean permitAll;

  public MpRolesAllowedFilter(String... allowedRoles) {
    boolean permitAll = false;
    for (String allowedRole : allowedRoles) {
      if (isNotBlank(allowedRole)) {
        if ("*".equals(allowedRole)) {
          permitAll = true;
          break;
        } else if (WildcardMatcher.hasWildcard(allowedRole)) {
          allowedRoleWildcards.add(WildcardMatcher.of(true, allowedRole));
        } else {
          this.allowedRoles.add(allowedRole);
        }
      }
    }
    this.permitAll = permitAll;
  }

  @Override
  public void filter(ContainerRequestContext requestContext) {
    SecurityContext securityContext = requestContext.getSecurityContext();
    boolean isForbidden;
    if (permitAll) {
      isForbidden = securityContext.getUserPrincipal() == null;
    } else {
      isForbidden = allowedRoles.stream().noneMatch(securityContext::isUserInRole);
      if (isForbidden && securityContext.getUserPrincipal() instanceof JsonWebToken
          && !allowedRoleWildcards.isEmpty()) {
        JsonWebToken jwt = JsonWebToken.class.cast(securityContext.getUserPrincipal());
        if (isNotEmpty(jwt.getGroups())) {
          isForbidden = jwt.getGroups().stream()
              .noneMatch(g -> allowedRoleWildcards.stream().anyMatch(p -> p.test(g)));
        }
      }
    }
    if (isForbidden) {
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
