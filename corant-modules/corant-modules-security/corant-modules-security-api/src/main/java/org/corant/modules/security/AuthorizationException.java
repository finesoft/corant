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

  public AuthorizationException(Object messageKey) {
    super(messageKey);
  }

  public AuthorizationException(Object messageKey, Object... messageParameters) {
    super(messageKey, messageParameters);
  }

  public AuthorizationException(Object messageKey, Object[] messageParameters, Object code,
      Map<Object, Object> attributes) {
    super(messageKey, messageParameters, code, attributes);
  }

  public AuthorizationException(Throwable cause) {
    super(cause);
  }

  public AuthorizationException(Throwable cause, Object messageKey) {
    super(cause, messageKey);
  }

  public AuthorizationException(Throwable cause, Object messageKey, Object... messageParameters) {
    super(cause, messageKey, messageParameters);
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
