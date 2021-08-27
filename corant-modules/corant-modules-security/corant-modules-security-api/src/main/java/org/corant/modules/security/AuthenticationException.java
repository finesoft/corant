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
 * @author bingo 9:55:50
 *
 */
public class AuthenticationException extends GeneralRuntimeException {

  private static final long serialVersionUID = -9200294304935728465L;

  public AuthenticationException() {}

  public AuthenticationException(Object code) {
    super(code);
  }

  public AuthenticationException(Object code, Object... variants) {
    super(code, variants);
  }

  public AuthenticationException(Object code, Object subCode, Map<Object, Object> attributes,
      Object... parameters) {
    super(code, subCode, attributes, parameters);
  }

  public AuthenticationException(Throwable cause) {
    super(cause);
  }

  public AuthenticationException(Throwable cause, Object code) {
    super(cause, code);
  }

  public AuthenticationException(Throwable cause, Object code, Object... parameters) {
    super(cause, code, parameters);
  }

  protected AuthenticationException(String message) {
    super(message);
  }

  protected AuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }

  public static AuthenticationException of(String message) {
    return new AuthenticationException(message);
  }

  public static AuthenticationException of(Throwable cause, String message) {
    return new AuthenticationException(message, cause);
  }
}
