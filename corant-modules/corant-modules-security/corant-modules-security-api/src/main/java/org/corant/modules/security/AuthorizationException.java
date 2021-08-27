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

import java.util.Map;
import org.corant.shared.exception.GeneralRuntimeException;

/**
 * corant-modules-security-api
 *
 * @author bingo 12:31:25
 *
 */
public class AuthorizationException extends GeneralRuntimeException {

  private static final long serialVersionUID = -4436947908543556229L;

  public AuthorizationException() {}

  public AuthorizationException(Object code) {
    super(code);
  }

  public AuthorizationException(Object code, Object... variants) {
    super(code, variants);
  }

  public AuthorizationException(Object code, Object subCode, Map<Object, Object> attributes,
      Object... parameters) {
    super(code, subCode, attributes, parameters);
  }

  public AuthorizationException(Throwable cause) {
    super(cause);
  }

  public AuthorizationException(Throwable cause, Object code) {
    super(cause, code);
  }

  public AuthorizationException(Throwable cause, Object code, Object... parameters) {
    super(cause, code, parameters);
  }

  protected AuthorizationException(String message) {
    super(message);
  }

  protected AuthorizationException(String message, Throwable cause) {
    super(message, cause);
  }

  public static AuthorizationException of(String message) {
    return new AuthorizationException(message);
  }

  public static AuthorizationException of(Throwable cause, String message) {
    return new AuthorizationException(message, cause);
  }
}
