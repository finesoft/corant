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
package org.corant.suites.datasource.agroal;

import java.time.Duration;
import io.agroal.api.AgroalDataSource;

/**
 * corant-suites-datasource-agroal
 *
 * @author bingo 下午4:57:26
 *
 */
public class AgroalCPDataSourceMetrics implements AgroalCPDataSourceMetricsMBean {

  final AgroalDataSource dataSource;

  /**
   * @param dataSource
   */
  protected AgroalCPDataSourceMetrics(AgroalDataSource dataSource) {
    super();
    this.dataSource = dataSource;
  }

  @Override
  public long acquireCount() {
    return dataSource.getMetrics().acquireCount();
  }

  @Override
  public long activeCount() {
    return dataSource.getMetrics().activeCount();
  }

  @Override
  public long availableCount() {
    return dataSource.getMetrics().availableCount();
  }

  @Override
  public long awaitingCount() {
    return dataSource.getMetrics().awaitingCount();
  }

  @Override
  public Duration blockingTimeAverage() {
    return dataSource.getMetrics().blockingTimeAverage();
  }

  @Override
  public Duration blockingTimeMax() {
    return dataSource.getMetrics().blockingTimeMax();
  }

  @Override
  public Duration blockingTimeTotal() {

    return dataSource.getMetrics().blockingTimeTotal();
  }

  @Override
  public long creationCount() {
    return dataSource.getMetrics().creationCount();
  }

  @Override
  public Duration creationTimeAverage() {
    return dataSource.getMetrics().creationTimeAverage();
  }

  @Override
  public Duration creationTimeMax() {
    return dataSource.getMetrics().creationTimeMax();
  }

  @Override
  public Duration creationTimeTotal() {
    return dataSource.getMetrics().creationTimeTotal();
  }

  @Override
  public String description() {
    return dataSource.getMetrics().toString();
  }

  @Override
  public long destroyCount() {
    return dataSource.getMetrics().destroyCount();
  }

  @Override
  public long flushCount() {
    return dataSource.getMetrics().flushCount();
  }

  @Override
  public long invalidCount() {
    return dataSource.getMetrics().invalidCount();
  }

  @Override
  public long leakDetectionCount() {
    return dataSource.getMetrics().leakDetectionCount();
  }

  @Override
  public long maxUsedCount() {
    return dataSource.getMetrics().maxUsedCount();
  }

  @Override
  public long reapCount() {
    return dataSource.getMetrics().reapCount();
  }

  @Override
  public void reset() {
    dataSource.getMetrics().reset();
  }

}
