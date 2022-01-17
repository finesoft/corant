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
package org.corant.shared.retry;

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.function.Predicate;

/**
 * corant-shared
 *
 * @author bingo 上午10:57:55
 *
 */
@FunctionalInterface
public interface RetryPrecondition {

  RetryPrecondition NON_PRECONDITION = new NonRetryPrecondition();

  default RetryPrecondition and(Predicate<RetryContext> other) {
    shouldNotNull(other);
    return ctx -> test(ctx) && other.test(ctx);
  }

  default RetryPrecondition and(RetryPrecondition other) {
    shouldNotNull(other);
    return ctx -> test(ctx) && other.test(ctx);
  }

  default RetryPrecondition negate() {
    return ctx -> !test(ctx);
  }

  default RetryPrecondition or(Predicate<RetryContext> other) {
    shouldNotNull(other);
    return ctx -> test(ctx) || other.test(ctx);
  }

  default RetryPrecondition or(RetryPrecondition other) {
    shouldNotNull(other);
    return ctx -> test(ctx) || other.test(ctx);
  }

  boolean test(RetryContext ctx);

  class NonRetryPrecondition implements RetryPrecondition {

    @Override
    public boolean test(RetryContext ctx) {
      return true;
    }

  }
}
