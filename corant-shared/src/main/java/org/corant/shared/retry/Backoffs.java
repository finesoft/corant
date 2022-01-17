/*
 * Copyright (c) 2013-2022, Bingo.Chen (finesoft@gmail.com).
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

import static org.corant.shared.util.Objects.min;
import org.corant.shared.util.Randoms;

public class Backoffs {

  /**
   * Use Exponential Backoff algorithm to compute the delay. The backoff factor accepted by this
   * method must be greater than 1 or null, default is 2.0.
   *
   * @see <a href=
   *      "https://aws.amazon.com/cn/blogs/architecture/exponential-backoff-and-jitter/">Exponential
   *      Backoff And Jitter</a>
   */
  public static long computeExpoBackoff(double backoffFactor, long cap, long base, int attempts) {
    long result = min(cap, base * (long) Math.pow(backoffFactor, attempts));
    return result > 0 ? result : cap;
  }

  /**
   * Use Exponential Backoff Decorr algorithm to compute the delay. The backoff factor accepted by
   * this method must be greater than 1 or null, default is 2.0.
   *
   * @see <a href=
   *      "https://aws.amazon.com/cn/blogs/architecture/exponential-backoff-and-jitter/">Exponential
   *      Backoff And Jitter</a>
   */
  public static long computeExpoBackoffDecorr(long cap, long base, int attempts, long sleep) {
    long result = min(cap, Randoms.randomLong(base, (sleep <= 0 ? base : sleep) * 3));
    return result > 0 ? result : cap;
  }

  /**
   * Use Exponential Backoff Equal Jitter algorithm to compute the delay. The backoff factor
   * accepted by this method must be greater than 1 or null, default is 2.0.
   *
   * @see <a href=
   *      "https://aws.amazon.com/cn/blogs/architecture/exponential-backoff-and-jitter/">Exponential
   *      Backoff And Jitter</a>
   */
  public static long computeExpoBackoffEqualJitter(double backoffFactor, long cap, long base,
      int attempts) {
    long expoBackoff = computeExpoBackoff(backoffFactor, cap, base, attempts);
    long temp = expoBackoff >>> 1;
    return temp + Randoms.randomLong(temp);
  }

  /**
   * Use Exponential Backoff Full Jitter algorithm to compute the delay. The backoff factor accepted
   * by this method must be greater than 1 or null, default is 2.0.
   *
   * @see <a href=
   *      "https://aws.amazon.com/cn/blogs/architecture/exponential-backoff-and-jitter/">Exponential
   *      Backoff And Jitter</a>
   */
  public static long computeExpoBackoffFullJitter(double backoffFactor, long cap, long base,
      int attempts) {
    long expoBackoff = computeExpoBackoff(backoffFactor, cap, base, attempts);
    return Randoms.randomLong(expoBackoff);
  }

}
