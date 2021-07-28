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

import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-security-api
 *
 * @author bingo 9:55:50
 *
 */
public class AuthenticationException extends CorantRuntimeException {

  private static final long serialVersionUID = -9200294304935728465L;

  public AuthenticationException() {}

  public AuthenticationException(String msgOrFormat, Object... args) {
    super(msgOrFormat, args);
  }

  public AuthenticationException(Throwable cause) {
    super(cause);
  }

  public AuthenticationException(Throwable cause, boolean enableSuppression,
      boolean writableStackTrace, String msgOrFormat, Object... args) {
    super(cause, enableSuppression, writableStackTrace, msgOrFormat, args);
  }

  public AuthenticationException(Throwable cause, String msgOrFormat, Object... args) {
    super(cause, msgOrFormat, args);
  }

}
