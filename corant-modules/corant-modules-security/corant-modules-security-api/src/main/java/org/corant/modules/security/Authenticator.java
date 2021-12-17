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
package org.corant.modules.security;

import org.corant.context.security.SecurityContext;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-security-api
 *
 * <p>
 * An Authenticator is responsible for authenticating accounts in an application.
 *
 * @author bingo 12:25:41
 *
 */
public interface Authenticator extends Sortable {

  AuthenticationData authenticate(Token token) throws AuthenticationException;

  default boolean authenticated(Object object) {
    return object instanceof SecurityContext && object != null
        && ((SecurityContext) object).getPrincipal() != null;
  }

  default void checkAuthenticated(Object object) throws AuthenticationException {
    if (!authenticated(object)) {
      throw new AuthenticationException((Object) SecurityMessageCodes.UNAUTHC_ACCESS);
    }
  }
}
