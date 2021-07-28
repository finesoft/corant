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
package org.corant.modules.microprofile.jwt.jaxrs;

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import org.corant.modules.security.AuthorizationException;
import org.corant.modules.security.Authorizer;
import org.corant.modules.security.shared.SimplePermission;
import org.corant.modules.security.shared.SimplePermissions;
import org.corant.modules.security.shared.SimpleRole;
import org.corant.modules.security.shared.SimpleRoles;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * corant-modules-microprofile-jwt
 *
 * @author bingo 上午10:23:37
 *
 */
public class MpJWTDefaultAuthorizer implements Authorizer {

  public static final SimpleRole ALL_ROLES = new SimpleRole("*");
  public static final SimplePermission ALL_PERMS = new SimplePermission("*");
  public static final MpJWTDefaultAuthorizer DFLT_INST = new MpJWTDefaultAuthorizer();

  @Override
  public void checkAccess(Object context, Object roleOrPermit) throws AuthorizationException {
    if (!testAccess(context, roleOrPermit)) {
      throw new AuthorizationException();
    }
  }

  @Override
  public boolean testAccess(Object context, Object roleOrPermit) {
    SecurityContext securityContext = ((ContainerRequestContext) context).getSecurityContext();
    if (isEmpty(roleOrPermit)) {
      return securityContext.getUserPrincipal() != null;
    }
    JsonWebToken jwt = JsonWebToken.class.cast(securityContext.getUserPrincipal());
    if (roleOrPermit instanceof SimpleRoles) {
      return hasRole(jwt, (SimpleRoles) roleOrPermit);
    } else {
      return hasPerm(jwt, (SimplePermissions) roleOrPermit);
    }
  }

  boolean hasPerm(JsonWebToken jwt, SimplePermissions perms) {
    List<SimplePermission> jwtPerms = jwt != null && isNotEmpty(jwt.getGroups())
        ? jwt.getGroups().stream().map(SimplePermission::of).collect(Collectors.toList())
        : Collections.emptyList();// FIXME extract permission from ?
    for (SimplePermission perm : perms) {
      if (perm.equals(ALL_PERMS) && jwt != null
          || isNotEmpty(jwtPerms) && jwtPerms.stream().anyMatch(perm::implies)) {
        return true;
      }
    }
    return false;
  }

  boolean hasRole(JsonWebToken jwt, SimpleRoles roles) {
    List<SimpleRole> jwtRoles = jwt != null && isNotEmpty(jwt.getGroups())
        ? jwt.getGroups().stream().map(SimpleRole::of).collect(Collectors.toList())
        : Collections.emptyList();
    for (SimpleRole role : roles) {
      if (role.equals(ALL_ROLES) && jwt != null
          || isNotEmpty(jwtRoles) && jwtRoles.stream().anyMatch(role::implies)) {
        return true;
      }
    }
    return false;
  }
}
