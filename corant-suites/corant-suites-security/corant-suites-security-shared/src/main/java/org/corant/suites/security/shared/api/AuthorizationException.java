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
package org.corant.suites.security.shared.api;

import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-suites-security-shared
 *
 * @author bingo 12:31:25
 *
 */
public class AuthorizationException extends CorantRuntimeException {

  private static final long serialVersionUID = -4436947908543556229L;

  /**
   *
   */
  public AuthorizationException() {
    super();
    // TODO Auto-generated constructor stub
  }

  /**
   * @param msgOrFormat
   * @param args
   */
  public AuthorizationException(String msgOrFormat, Object... args) {
    super(msgOrFormat, args);
    // TODO Auto-generated constructor stub
  }

  /**
   * @param cause
   */
  public AuthorizationException(Throwable cause) {
    super(cause);
    // TODO Auto-generated constructor stub
  }

  /**
   * @param cause
   * @param enableSuppression
   * @param writableStackTrace
   * @param msgOrFormat
   * @param args
   */
  public AuthorizationException(Throwable cause, boolean enableSuppression,
      boolean writableStackTrace, String msgOrFormat, Object... args) {
    super(cause, enableSuppression, writableStackTrace, msgOrFormat, args);
    // TODO Auto-generated constructor stub
  }

  /**
   * @param cause
   * @param msgOrFormat
   * @param args
   */
  public AuthorizationException(Throwable cause, String msgOrFormat, Object... args) {
    super(cause, msgOrFormat, args);
    // TODO Auto-generated constructor stub
  }

}
