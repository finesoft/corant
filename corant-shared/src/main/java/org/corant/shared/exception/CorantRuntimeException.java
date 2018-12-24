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
package org.corant.shared.exception;

/**
 * corant-shared
 *
 * @author bingo 上午10:14:58
 *
 */
public class CorantRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 5315201175867072341L;

  /**
   *
   */
  public CorantRuntimeException() {
    super();
  }

  /**
   * @param msgOrFormat
   * @param args
   */
  public CorantRuntimeException(String msgOrFormat, Object... args) {
    super(args.length == 0 ? msgOrFormat : String.format(msgOrFormat, args));
  }

  /**
   * @param cause
   */
  public CorantRuntimeException(Throwable cause) {
    super(cause);
  }

  /**
   * @param cause
   * @param enableSuppression
   * @param writableStackTrace
   * @param msgOrFormat
   * @param args
   */
  public CorantRuntimeException(Throwable cause, boolean enableSuppression,
      boolean writableStackTrace, String msgOrFormat, Object... args) {
    super(args.length == 0 ? msgOrFormat : String.format(msgOrFormat, args), cause,
        enableSuppression, writableStackTrace);
  }

  /**
   * @param cause
   * @param msgOrFormat
   * @param args
   */
  public CorantRuntimeException(Throwable cause, String msgOrFormat, Object... args) {
    super(args.length == 0 ? msgOrFormat : String.format(msgOrFormat, args), cause);
  }


}
