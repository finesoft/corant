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
package org.corant.modules.datasource.agroal;

import static org.corant.shared.util.Strings.EMPTY;
import java.time.Duration;

/**
 * corant-modules-datasource-agroal
 *
 * @author bingo 下午4:55:42
 */
public interface AgroalCPDataSourceMetricsMBean {

  /**
   * Number of times an acquire operation succeeded.
   */
  default long acquireCount() {
    return 0;
  }

  /**
   * Number active of connections. This connections are in use and not available to be acquired.
   */
  default long activeCount() {
    return 0;
  }

  /**
   * Number of idle connections in the pool, available to be acquired.
   */
  default long availableCount() {
    return 0;
  }

  /**
   * Approximate number of threads blocked, waiting to acquire a connection.
   */
  default long awaitingCount() {
    return 0;
  }

  /**
   * Average time an application waited to acquire a connection.
   */
  default Duration blockingTimeAverage() {
    return Duration.ZERO;
  }

  /**
   * Maximum time an application waited to acquire a connection.
   */
  default Duration blockingTimeMax() {
    return Duration.ZERO;
  }

  /**
   * Total time applications waited to acquire a connection.
   */
  default Duration blockingTimeTotal() {
    return Duration.ZERO;
  }

  /**
   * Number of created connections.
   */
  default long creationCount() {
    return 0;
  }

  /**
   * Average time for a connection to be created.
   */
  default Duration creationTimeAverage() {
    return Duration.ZERO;
  }

  // --- //

  /**
   * Maximum time for a connection to be created.
   */
  default Duration creationTimeMax() {
    return Duration.ZERO;
  }

  /**
   * Total time waiting for a connections to be created.
   */
  default Duration creationTimeTotal() {
    return Duration.ZERO;
  }

  default String description() {
    return EMPTY;
  }

  /**
   * Number of destroyed connections.
   */
  default long destroyCount() {
    return 0;
  }

  /**
   * Number of connections removed from the pool, not counting invalid / idle.
   */
  default long flushCount() {
    return 0;
  }

  /**
   * Number of connections removed from the pool for being invalid.
   */
  default long invalidCount() {
    return 0;
  }

  /**
   * Number of times a leak was detected. A single connection can be detected multiple times.
   */
  default long leakDetectionCount() {
    return 0;
  }

  /**
   * Maximum number of connections active simultaneously.
   */
  default long maxUsedCount() {
    return 0;
  }

  /**
   * Number of connections removed from the pool for being idle.
   */
  default long reapCount() {
    return 0;
  }

  // --- //

  /**
   * Reset the metrics.
   */
  default void reset() {}
}
