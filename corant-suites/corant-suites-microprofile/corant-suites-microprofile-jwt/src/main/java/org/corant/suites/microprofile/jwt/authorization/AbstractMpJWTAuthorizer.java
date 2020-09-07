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
package org.corant.suites.microprofile.jwt.authorization;

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Strings.trim;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import org.corant.shared.util.Objects;
import org.corant.shared.util.Strings.WildcardMatcher;
import org.corant.suites.security.shared.authorization.AuthorizationException;
import org.corant.suites.security.shared.authorization.Authorizer;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * corant-suites-microprofile-jwt
 *
 * @author bingo 12:38:11
 *
 */
public abstract class AbstractMpJWTAuthorizer
    implements Authorizer<ContainerRequestContext, String[]> {

  public static final String PERMIT_ALL = "*";

  protected final Map<String, Predicate<String>> predicates = new ConcurrentHashMap<>();

  @Override
  public void check(ContainerRequestContext principal, String[] roleOrPermit)
      throws AuthorizationException {}

  @Override
  public boolean isAllowed(ContainerRequestContext principal, String[] roleOrPermit) {
    SecurityContext securityContext = principal.getSecurityContext();
    if (isEmpty(roleOrPermit)) {
      return securityContext.getUserPrincipal() != null;
    }
    for (String rop : roleOrPermit) {
      if (PERMIT_ALL.equals(rop)) {
        if (securityContext.getUserPrincipal() != null) {
          return true;
        }
      } else if (isNotEmpty(rop)) {
        Predicate<String> p = predicates.computeIfAbsent(rop, this::getPredicate);
        JsonWebToken jwt = JsonWebToken.class.cast(securityContext.getUserPrincipal());
        if (isAllowed(p, jwt)) {
          return true;
        }
      }
    }
    return false;
  }

  protected Predicate<String> getPredicate(String t) {
    String use = trim(t);
    if (isEmpty(use)) {
      return s -> true;
    } else if (WildcardMatcher.hasWildcard(use)) {
      return WildcardMatcher.of(false, use);
    } else {
      return s -> Objects.areEqual(s, use);
    }
  }

  protected abstract boolean isAllowed(Predicate<String> p, JsonWebToken jwt);
}
