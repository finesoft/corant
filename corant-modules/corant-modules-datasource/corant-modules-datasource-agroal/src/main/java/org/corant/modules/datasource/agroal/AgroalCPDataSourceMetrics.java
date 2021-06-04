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

import static org.corant.context.Beans.findNamed;
import java.time.Duration;
import java.util.Optional;
import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalDataSourceMetrics;

/**
 * corant-modules-datasource-agroal
 *
 * @author bingo 下午4:57:26
 *
 */
public class AgroalCPDataSourceMetrics implements AgroalCPDataSourceMetricsMBean {

  static final AgroalDataSourceMetrics EMPTY = new AgroalDataSourceMetrics() {};

  final String dataSourceName;

  /**
   * @param dataSourceName
   */
  protected AgroalCPDataSourceMetrics(String dataSourceName) {
    this.dataSourceName = dataSourceName;
  }

  @Override
  public long acquireCount() {
    return getMetrics().acquireCount();
  }

  @Override
  public long activeCount() {
    return getMetrics().activeCount();
  }

  @Override
  public long availableCount() {
    return getMetrics().availableCount();
  }

  @Override
  public long awaitingCount() {
    return getMetrics().awaitingCount();
  }

  @Override
  public Duration blockingTimeAverage() {
    return getMetrics().blockingTimeAverage();
  }

  @Override
  public Duration blockingTimeMax() {
    return getMetrics().blockingTimeMax();
  }

  @Override
  public Duration blockingTimeTotal() {

    return getMetrics().blockingTimeTotal();
  }

  @Override
  public long creationCount() {
    return getMetrics().creationCount();
  }

  @Override
  public Duration creationTimeAverage() {
    return getMetrics().creationTimeAverage();
  }

  @Override
  public Duration creationTimeMax() {
    return getMetrics().creationTimeMax();
  }

  @Override
  public Duration creationTimeTotal() {
    return getMetrics().creationTimeTotal();
  }

  @Override
  public String description() {
    return getMetrics().toString();
  }

  @Override
  public long destroyCount() {
    return getMetrics().destroyCount();
  }

  @Override
  public long flushCount() {
    return getMetrics().flushCount();
  }

  public AgroalDataSourceMetrics getMetrics() {
    Optional<AgroalDataSource> ds = findNamed(AgroalDataSource.class, dataSourceName);
    if (ds.isPresent()) {
      return ds.get().getMetrics();
    } else {
      return EMPTY;
    }
  }

  @Override
  public long invalidCount() {
    return getMetrics().invalidCount();
  }

  @Override
  public long leakDetectionCount() {
    return getMetrics().leakDetectionCount();
  }

  @Override
  public long maxUsedCount() {
    return getMetrics().maxUsedCount();
  }

  @Override
  public long reapCount() {
    return getMetrics().reapCount();
  }

  @Override
  public void reset() {
    getMetrics().reset();
  }
}
