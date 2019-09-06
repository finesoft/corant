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
package org.corant.microprofile.jwt;

import java.security.Principal;
import javax.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * corant-suites-mp-jwt
 *
 * @author bingo 下午12:05:42
 *
 */
public class MpJWTSecurityContext implements SecurityContext {
  private SecurityContext delegate;
  private JsonWebToken principal;

  MpJWTSecurityContext(SecurityContext delegate, JsonWebToken principal) {
    this.delegate = delegate;
    this.principal = principal;
  }

  @Override
  public String getAuthenticationScheme() {
    return delegate.getAuthenticationScheme();
  }

  @Override
  public Principal getUserPrincipal() {
    return principal;
  }

  @Override
  public boolean isSecure() {
    return delegate.isSecure();
  }

  @Override
  public boolean isUserInRole(String role) {
    return principal.getGroups().contains(role);
  }
}
