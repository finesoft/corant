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
import org.corant.shared.exception.NotSupportedException;
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

  /**
   * Authenticate according to the given token. If the authentication is successful, return an
   * {@link AuthenticationData} that represents the data relevant to the principals, This returned
   * object is generally used to construct a Subject. If the authentication is failure or there is
   * any problem during the authentication process throw an authentication exception.
   *
   * @param token the consolidation of an account's principals and supporting credentials
   * @return the authenticationData represents the data relevant to the principals is generally used
   *         in turn to construct a Subject
   * @throws AuthenticationException If the authentication is failure
   */
  default AuthenticationData authenticate(Token token) throws AuthenticationException {
    throw new NotSupportedException();
  }

  /**
   * Determine whether the given context has been authenticated.
   *
   * @param context in general, the context is current SecurityContext
   * @return true if it has been authenticated otherwise false
   */
  default boolean authenticated(Object context) {
    return context instanceof SecurityContext && ((SecurityContext) context).getPrincipal() != null;
  }

  /**
   * Check whether the given context has been authenticated, if not throws an authentication
   * exception.
   *
   * @param context in general, the context is current SecurityContext
   */
  default void checkAuthenticated(Object context) throws AuthenticationException {
    if (!authenticated(context)) {
      throw new AuthenticationException((Object) SecurityMessageCodes.UNAUTHC_ACCESS);
    }
  }

}
