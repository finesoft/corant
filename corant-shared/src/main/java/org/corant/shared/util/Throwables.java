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
package org.corant.shared.util;

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.StreamUtils.asStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

/**
 * corant-kernel
 *
 * @author bingo 下午6:01:18
 *
 */
public class Throwables {

  private Throwables() {}

  public static Stream<Throwable> causes(final Throwable throwable) {
    return asStream(() -> new Iterator<Throwable>() {
      volatile Throwable cause = throwable;

      @Override
      public boolean hasNext() {
        return cause != null && cause.getCause() != null && cause != cause.getCause();
      }

      @Override
      public Throwable next() {
        cause = cause.getCause();
        if (cause == null) {
          throw new NoSuchElementException();
        }
        return cause;
      }
    });
  }

  public static Throwable rootCause(final Throwable throwable) {
    return throwable != null && throwable.getCause() != null && throwable != throwable.getCause()
        ? rootCause(throwable.getCause())
        : throwable;
  }

  public static String stackTraceAsString(Throwable throwable) {
    StringWriter stringWriter = new StringWriter();
    throwable.printStackTrace(new PrintWriter(stringWriter));
    return stringWriter.toString();
  }

  public static Stream<Throwable> suppresses(final Throwable throwable) {
    return throwable == null || isEmpty(throwable.getSuppressed()) ? Stream.empty()
        : asStream(throwable.getSuppressed());
  }
}
