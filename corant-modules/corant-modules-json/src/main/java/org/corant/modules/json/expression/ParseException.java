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
package org.corant.modules.json.expression;

import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-json
 *
 * @author bingo 下午3:17:54
 *
 */
public class ParseException extends CorantRuntimeException {

  private static final long serialVersionUID = -3085053879489647592L;

  public ParseException() {}

  public ParseException(String msgOrFormat, Object... args) {
    super(msgOrFormat, args);
  }

  public ParseException(Throwable cause) {
    super(cause);
  }

  public ParseException(Throwable cause, boolean enableSuppression, boolean writableStackTrace,
      String msgOrFormat, Object... args) {
    super(cause, enableSuppression, writableStackTrace, msgOrFormat, args);
  }

  public ParseException(Throwable cause, String msgOrFormat, Object... args) {
    super(cause, msgOrFormat, args);
  }

}
