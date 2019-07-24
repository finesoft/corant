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
package org.corant.asosat.ddd.security;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.SecurityContext;
import org.corant.asosat.ddd.domain.shared.Participator;

/**
 * corant-asosat-ddd
 *
 * @author bingo 上午11:27:52
 *
 */
public class DefaultSecurityContext implements SecurityContext {

  public static final DefaultSecurityContext EMPTY_INST = new DefaultSecurityContext(null, null,
      null, Participator.empty(), Participator.empty(), false, null, null);

  private final Principal userPrincipal;
  private final Participator currentUser;
  private final Participator currentOrg;
  private final boolean secure;
  private final Set<String> userRoles = new HashSet<>();
  private final String authenticationScheme;
  private final String accessToken;
  private final String refreshToken;

  /**
   * @param accessToken
   * @param refreshToken
   * @param userPrincipal
   * @param currentUser
   * @param currentOrg
   * @param secure
   * @param authenticationScheme
   * @param userRoles
   */
  public DefaultSecurityContext(String accessToken, String refreshToken, Principal userPrincipal,
      Participator currentUser, Participator currentOrg, boolean secure,
      String authenticationScheme, Set<String> userRoles) {
    super();
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.userPrincipal = userPrincipal;
    this.currentOrg = currentOrg;
    this.currentUser = currentUser;
    this.secure = secure;
    this.authenticationScheme = authenticationScheme;
    if (userRoles != null) {
      this.userRoles.addAll(userRoles);
    }
  }

  public String getAccessToken() {
    return accessToken;
  }

  @Override
  public String getAuthenticationScheme() {
    return authenticationScheme;
  }

  public Participator getCurrentOrg() {
    return currentOrg;
  }

  public Participator getCurrentUser() {
    return currentUser;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  @Override
  public Principal getUserPrincipal() {
    return userPrincipal;
  }

  @Override
  public boolean isSecure() {
    return secure;
  }

  @Override
  public boolean isUserInRole(String role) {
    return userRoles.contains(role);
  }

}
