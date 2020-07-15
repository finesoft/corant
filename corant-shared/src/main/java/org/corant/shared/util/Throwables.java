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

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Streams.streamOf;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-shared
 *
 * @author bingo 下午6:01:18
 *
 */
public class Throwables {

  private Throwables() {}

  public static <S> Attempt<S> attempt(Supplier<S> supplier) {
    return new Attempt<>(supplier);
  }

  public static Stream<Throwable> causes(final Throwable throwable) {
    return streamOf(() -> new Iterator<Throwable>() {
      volatile Throwable cause = throwable;

      @Override
      public boolean hasNext() {
        return cause != null && cause.getCause() != null && cause != cause.getCause();
      }

      @Override
      public Throwable next() {
        if (cause == null || (cause = cause.getCause()) == null) {
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
        : streamOf(throwable.getSuppressed());
  }

  public static class Attempt<S> {

    final Supplier<S> supplier;
    final LinkedList<Case<S>> cases = new LinkedList<>();

    private Attempt(Supplier<S> supplier) {
      this.supplier = shouldNotNull(supplier);
    }

    public S attempt() {
      try {
        return supplier.get();
      } catch (Exception e) {
        causes(e).forEach(ce -> {
          Iterator<Case<S>> it = cases.iterator();
          for (; it.hasNext();) {
            Case<S> cas = it.next();
            if (cas.ifThrow != null && cas.ifThrow.test(ce) && cas.rethrow != null) {
              throw cas.rethrow.get();
            }
          }
        });
        throw new CorantRuntimeException(e);
      } finally {
        cases.clear();
      }
    }

    public Case<S> ifThrow(final Class<? extends Throwable> causeClass) {
      return ifThrow((t) -> causeClass.isInstance(t));
    }

    public Case<S> ifThrow(final Predicate<Throwable> p) {
      Case<S> cas = new Case<>(this);
      cas.ifThrow = p;
      cases.add(cas);
      return cas;
    }
  }

  public static class Case<S> {
    final Attempt<S> attempt;
    Predicate<Throwable> ifThrow;
    Supplier<RuntimeException> rethrow;

    private Case(Attempt<S> attempt) {
      this.attempt = attempt;
    }

    public Attempt<S> rethrow(final RuntimeException rethrow) {
      return rethrow(() -> rethrow);
    }

    public Attempt<S> rethrow(final Supplier<RuntimeException> rethrow) {
      this.rethrow = rethrow;
      return attempt;
    }
  }
}
