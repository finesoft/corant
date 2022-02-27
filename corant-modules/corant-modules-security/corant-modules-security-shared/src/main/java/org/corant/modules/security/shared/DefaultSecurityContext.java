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
package org.corant.modules.security.shared;

import org.corant.context.security.SecurityContext;
import org.corant.modules.security.Principal;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午3:14:35
 *
 */
public class DefaultSecurityContext implements SecurityContext {

  private static final long serialVersionUID = 4329263253208902621L;

  protected final String authenticationScheme;

  protected final Principal principal;

  public DefaultSecurityContext(String authenticationScheme, Principal principal) {
    this.authenticationScheme = authenticationScheme;
    this.principal = principal;
  }

  @Override
  public String getAuthenticationScheme() {
    return authenticationScheme;
  }

  @Override
  public Principal getPrincipal() {
    return principal;
  }

  @Override
  public <T> T getPrincipal(Class<T> cls) {
    return principal == null ? null : principal.unwrap(cls);
  }

  @Override
  public String toString() {
    return "DefaultSecurityContext [authenticationScheme=" + authenticationScheme + ", principal="
        + principal + "]";
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    if (DefaultSecurityContext.class.isAssignableFrom(cls)) {
      return cls.cast(this);
    }
    return SecurityContext.super.unwrap(cls);
  }

}
