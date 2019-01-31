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
package org.corant.suites.security.shared;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.SecurityContext;

/**
 * corant-suites-security-jaxrs
 *
 * @author bingo 下午5:05:35
 *
 */
public class JaxrsSecurityContext implements SecurityContext {

  private final Principal principal;
  private final Set<String> roles = new HashSet<>();
  private final boolean secure;
  private final String principalName;

  public JaxrsSecurityContext(Principal principal, String principalName, Set<String> roles,
      boolean secure) {
    super();
    this.principal = principal;
    this.principalName = principalName;
    if (roles != null) {
      this.roles.addAll(roles);
    }
    this.secure = secure;
  }

  @Override
  public String getAuthenticationScheme() {
    return "OAUTH";
  }

  @Override
  public Principal getUserPrincipal() {
    return principal;
  }

  public String getUserPrincipalName() {
    return principalName;
  }

  @Override
  public boolean isSecure() {
    return secure;
  }

  @Override
  public boolean isUserInRole(String role) {
    return roles.contains(role);
  }

}
