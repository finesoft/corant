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

import static java.lang.String.format;

/**
 * corant-shared
 *
 * @author bingo 上午10:14:58
 */
public class CorantRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 5315201175867072341L;

  public CorantRuntimeException() {}

  /**
   * Use message or message format and parameters to construct a new runtime exception.
   *
   * @param msgOrFormat the message or message format
   * @param args the message format parameter
   *
   * @see String#format(String, Object...)
   */
  public CorantRuntimeException(String msgOrFormat, Object... args) {
    super(args.length == 0 ? msgOrFormat : format(msgOrFormat, args));
  }

  /**
   * Use cause to construct a new runtime exception.
   *
   * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
   *        (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
   */
  public CorantRuntimeException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a new runtime exception with cause, suppression enabled or disabled, and writable
   * stack trace enabled or disabled and the specified detail message or message format and
   * parameters.
   *
   * @param cause the cause. (A {@code null} value is permitted, and indicates that the cause is
   *        nonexistent or unknown.)
   * @param enableSuppression whether suppression is enabled or disabled
   * @param writableStackTrace whether the stack trace should be writable
   * @param msgOrFormat the detail message or message format
   * @param args the detail message format parameters
   */
  public CorantRuntimeException(Throwable cause, boolean enableSuppression,
      boolean writableStackTrace, String msgOrFormat, Object... args) {
    super(args.length == 0 ? msgOrFormat : format(msgOrFormat, args), cause, enableSuppression,
        writableStackTrace);
  }

  /**
   * Constructs a new runtime exception with the specified detail message and cause.
   * <p>
   * Note: the detail message associated with cause is not automatically incorporated in this
   * runtime exception's detail message.
   * </p>
   *
   * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
   *        (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
   * @param msgOrFormat the message or message format
   * @param args the message format parameter
   */
  public CorantRuntimeException(Throwable cause, String msgOrFormat, Object... args) {
    super(args.length == 0 ? msgOrFormat : format(msgOrFormat, args), cause);
  }

}
