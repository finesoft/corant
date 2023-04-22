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

import static org.corant.context.Beans.select;
import static org.corant.shared.util.Empties.isNotEmpty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;
import jakarta.enterprise.inject.Instance;
import org.corant.context.security.SecurityContext;
import org.corant.modules.security.AuthorizerCallback;
import org.corant.modules.security.shared.AbstractAuthorizer;
import org.corant.modules.security.shared.SimplePermission;
import org.corant.modules.security.shared.SimplePermissions;
import org.corant.modules.security.shared.SimplePrincipal;
import org.corant.modules.security.shared.SimpleRole;
import org.corant.modules.security.shared.SimpleRoles;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-microprofile-jwt
 *
 * @author bingo 上午10:23:37
 *
 */
public class MpJWTAuthorizer extends AbstractAuthorizer {

  public static final MpJWTAuthorizer DFLT_INST = new MpJWTAuthorizer();

  @Override
  public boolean testAccess(Object context, Object roleOrPermit) {
    if (context instanceof SecurityContext) {
      SecurityContext sctx = (SecurityContext) context;
      if (roleOrPermit instanceof SimpleRoles) {
        return testRoleAccess(sctx, (SimpleRoles) roleOrPermit);
      } else if (roleOrPermit instanceof SimplePermissions) {
        return testPermAccess(sctx, (SimplePermissions) roleOrPermit);
      }
    }
    return false;
  }

  @Override
  protected Stream<AuthorizerCallback> resolveCallbacks() {
    Instance<AuthorizerCallback> cbs = select(AuthorizerCallback.class);
    if (!cbs.isUnsatisfied()) {
      cbs.stream().sorted(Sortable::compare);
    }
    return Stream.empty();
  }

  protected boolean testPermAccess(SecurityContext sctx, SimplePermissions perms) {
    if (perms.isEmpty()) {
      return sctx.getCallerPrincipal() != null;
    }
    Collection<SimplePermission> sctxPerms = sctx.getPrincipal(SimplePrincipal.class)
        .getAttribute("permits", ArrayList::new, p -> new SimplePermission(p.toString()));
    if (isNotEmpty(sctxPerms)) {
      for (SimplePermission perm : perms) {
        if (sctxPerms.stream().anyMatch(p -> p.implies(perm))) {
          return true;
        }
      }
    }
    return false;
  }

  protected boolean testRoleAccess(SecurityContext sctx, SimpleRoles roles) {
    if (roles.isEmpty()) {
      return sctx.getCallerPrincipal() != null;
    }
    Collection<SimpleRole> sctxRoles = sctx.getPrincipal(SimplePrincipal.class)
        .getAttribute("groups", ArrayList::new, r -> new SimpleRole(r.toString()));
    if (isNotEmpty(sctxRoles)) {
      for (SimpleRole role : roles) {
        if (sctxRoles.stream().anyMatch(role::implies)) {
          return true;
        }
      }
    }
    return false;
  }
}
