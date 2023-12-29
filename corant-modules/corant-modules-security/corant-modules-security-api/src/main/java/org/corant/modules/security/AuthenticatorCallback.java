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
package org.corant.modules.security;

import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-security-api
 *
 * @author bingo 下午11:56:54
 */
public interface AuthenticatorCallback extends Sortable {

  /**
   * A callback after authenticated.
   *
   * @param token the consolidation of an account's principals and supporting credentials
   * @param authenticationData the authentication result, if null means authentication is failure.
   */
  default void postAuthenticated(Token token, AuthenticationData authenticationData) {}

  /**
   * A callback before authenticate, can be used for multi-factor authentication and more
   *
   * @param token the consolidation of an account's principals and supporting credentials
   */
  default void preAuthenticate(Token token) {}
}
