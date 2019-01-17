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
package org.corant.suites.elastic;

import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-suites-elastic
 *
 * @author bingo 下午4:45:54
 *
 */
public class ElasticException extends CorantRuntimeException {

  private static final long serialVersionUID = -1173039510894395840L;

  /**
   *
   */
  public ElasticException() {
    super();
    // TODO Auto-generated constructor stub
  }

  /**
   * @param msgOrFormat
   * @param args
   */
  public ElasticException(String msgOrFormat, Object... args) {
    super(msgOrFormat, args);
    // TODO Auto-generated constructor stub
  }

  /**
   * @param cause
   */
  public ElasticException(Throwable cause) {
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
  public ElasticException(Throwable cause, boolean enableSuppression, boolean writableStackTrace,
      String msgOrFormat, Object... args) {
    super(cause, enableSuppression, writableStackTrace, msgOrFormat, args);
    // TODO Auto-generated constructor stub
  }

  /**
   * @param cause
   * @param msgOrFormat
   * @param args
   */
  public ElasticException(Throwable cause, String msgOrFormat, Object... args) {
    super(cause, msgOrFormat, args);
    // TODO Auto-generated constructor stub
  }

}
