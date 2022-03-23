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
package org.corant.modules.microprofile.jwt;

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;
import org.corant.context.security.SecurityContext;
import org.corant.modules.security.AuthorizerCallback;
import org.corant.modules.security.annotation.Secured;
import org.corant.modules.security.shared.AbstractAuthorizer;
import org.corant.modules.security.shared.SimplePermission;
import org.corant.modules.security.shared.SimplePermissions;
import org.corant.modules.security.shared.SimplePrincipal;
import org.corant.modules.security.shared.SimpleRole;
import org.corant.modules.security.shared.SimpleRoles;

/**
 * corant-modules-microprofile-jwt
 *
 * @author bingo 上午10:23:37
 *
 */
public class MpJWTAuthorizer extends AbstractAuthorizer {

  public static final SimpleRole ALL_ROLES = new SimpleRole(Secured.ALLOWED_ALL_SIGN);
  public static final SimplePermission ALL_PERMS = new SimplePermission(Secured.ALLOWED_ALL_SIGN);
  public static final MpJWTAuthorizer DFLT_INST = new MpJWTAuthorizer();

  @Override
  public boolean testAccess(Object context, Object roleOrPermit) {
    if (context instanceof SecurityContext) {
      SecurityContext sctx = (SecurityContext) context;
      if (isEmpty(roleOrPermit)) {
        return sctx.getPrincipal() != null;
      } else if (roleOrPermit instanceof SimpleRoles) {
        return testRoleAccess(sctx, (SimpleRoles) roleOrPermit);
      } else if (roleOrPermit instanceof SimplePermissions) {
        return testPermAccess(sctx, (SimplePermissions) roleOrPermit);
      }
    }
    return false;
  }

  @Override
  protected Stream<AuthorizerCallback> resolveCallbacks() {
    return Stream.empty();
  }

  protected boolean testPermAccess(SecurityContext sctx, SimplePermissions perms) {
    SimplePrincipal principal = null;
    Collection<SimplePermission> sctxPerms = null;
    if (sctx != null && sctx.getPrincipal() instanceof SimplePrincipal) {
      principal = (SimplePrincipal) sctx.getPrincipal();
      sctxPerms = principal.getAttribute("permits", ArrayList::new, SimplePermission.class);
    }
    for (SimplePermission perm : perms) {
      if (perm.equals(ALL_PERMS) && principal != null
          || isNotEmpty(sctxPerms) && sctxPerms.stream().anyMatch(perm::implies)) {
        return true;
      }
    }
    return false;
  }

  protected boolean testRoleAccess(SecurityContext sctx, SimpleRoles roles) {
    SimplePrincipal principal = null;
    Collection<SimpleRole> sctxRoles = null;
    if (sctx != null && sctx.getPrincipal() instanceof SimplePrincipal) {
      principal = (SimplePrincipal) sctx.getPrincipal();
      sctxRoles = principal.getAttribute("groups", ArrayList::new, SimpleRole.class);
    }
    for (SimpleRole role : roles) {
      if (role.equals(ALL_ROLES) && principal != null
          || isNotEmpty(sctxRoles) && sctxRoles.stream().anyMatch(role::implies)) {
        return true;
      }
    }
    return false;
  }
}
