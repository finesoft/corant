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

import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.corant.shared.exception.GeneralExceptionSeverity;
import org.corant.shared.exception.GeneralRuntimeException;

/**
 * corant-modules-security-api
 *
 * @author bingo 9:55:50
 */
public class AuthenticationException extends GeneralRuntimeException {

  private static final long serialVersionUID = -9200294304935728465L;

  public AuthenticationException() {}

  public AuthenticationException(Object messageKey) {
    super(messageKey);
  }

  public AuthenticationException(Object messageKey, Object... messageParameters) {
    super(messageKey, messageParameters);
  }

  public AuthenticationException(Throwable cause) {
    super(cause);
  }

  public AuthenticationException(Throwable cause, Object messageKey) {
    super(cause, messageKey);
  }

  public AuthenticationException(Throwable cause, Object messageKey, Object... messageParameters) {
    super(cause, messageKey, messageParameters);
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

  @Override
  public AuthenticationException attribute(Object name, Object value) {
    super.attribute(name, value);
    return this;
  }

  @Override
  public AuthenticationException attributes(Map<Object, Object> attributes) {
    super.attributes(attributes);
    return this;
  }

  @Override
  public AuthenticationException attributes(UnaryOperator<Map<Object, Object>> func) {
    super.attributes(func);
    return this;
  }

  @Override
  public AuthenticationException code(Object code) {
    super.code(code);
    return this;
  }

  @Override
  public AuthenticationException messageKey(Object messageKey) {
    super.messageKey(messageKey);
    return this;
  }

  @Override
  public AuthenticationException messageParameters(Object[] messageParameters) {
    super.messageParameters(messageParameters);
    return this;
  }

  @Override
  public AuthenticationException messageParameters(UnaryOperator<List<Object>> func) {
    super.messageParameters(func);
    return this;
  }

  @Override
  public AuthenticationException severity(GeneralExceptionSeverity severity) {
    super.severity(severity);
    return this;
  }
}
