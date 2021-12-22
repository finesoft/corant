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

import java.util.Set;
import org.corant.modules.security.Principal;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * corant-modules-microprofile-jwt
 *
 * @author bingo 上午11:22:36
 *
 */
public class MpJWTPrincipal implements JsonWebToken, Principal {

  private static final long serialVersionUID = 684661108279828334L;

  private final JsonWebToken mpPrincipal;

  public MpJWTPrincipal(JsonWebToken mpPrincipal) {
    this.mpPrincipal = mpPrincipal;
  }

  @Override
  public <T> T getClaim(String claimName) {
    return mpPrincipal.getClaim(claimName);
  }

  @Override
  public Set<String> getClaimNames() {
    return mpPrincipal.getClaimNames();
  }

  public JsonWebToken getMpPrincipal() {
    return mpPrincipal;
  }

  @Override
  public String getName() {
    return mpPrincipal.getName();
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    if (MpJWTPrincipal.class.isAssignableFrom(cls)) {
      return cls.cast(this);
    }
    if (JsonWebToken.class.isAssignableFrom(cls)) {
      return cls.cast(mpPrincipal);
    }
    return Principal.super.unwrap(cls);
  }
}
