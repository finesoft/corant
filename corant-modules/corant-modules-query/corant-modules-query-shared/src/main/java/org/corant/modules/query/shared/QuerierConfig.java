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
package org.corant.modules.query.shared;

import java.time.Duration;
import java.util.logging.Logger;
import org.corant.config.declarative.ConfigKeyItem;
import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativeConfig;
import org.eclipse.microprofile.config.Config;

/**
 * corant-modules-query-shared
 *
 * @author bingo 上午9:31:30
 *
 */
@ConfigKeyRoot(value = "corant.query.querier", keyIndex = 3, ignoreNoAnnotatedItem = false)
public class QuerierConfig implements DeclarativeConfig {

  protected static final Logger logger = Logger.getLogger(QuerierConfig.class.getName());

  public static final QuerierConfig DEFAULT =
      new QuerierConfig(10000, 128, 10000, 16, 32, true, null);
  public static final int UN_LIMIT_SELECT_SIZE = Integer.MAX_VALUE - 16;
  public static final String CTX_KEY_PARALLEL_FETCH = ".parallel-fetch";
  public static final String PRO_KEY_MAX_SELECT_SIZE = ".max-select-size";
  public static final String PRO_KEY_THROWN_ON_MAX_LIMIT_SIZE = ".thrown-on-max-select-size";
  public static final String PRO_KEY_LIMIT = ".limit";
  public static final String PRO_KEY_TIMEOUT = ".timeout";

  private static final long serialVersionUID = -2562004354294555255L;

  @ConfigKeyItem(defaultValue = "10000")
  protected int maxSelectSize;

  @ConfigKeyItem(defaultValue = "128")
  protected int defaultSelectSize;

  @ConfigKeyItem(defaultValue = "10000")
  protected int maxLimit;

  @ConfigKeyItem(defaultValue = "16")
  protected int defaultLimit;

  @ConfigKeyItem(defaultValue = "32")
  protected int defaultStreamLimit;

  @ConfigKeyItem(defaultValue = "true")
  protected boolean thrownOnMaxSelectSize;

  protected Duration timeout;

  public QuerierConfig() {}

  protected QuerierConfig(int maxSelectSize, int defaultSelectSize, int maxLimit, int defaultLimit,
      int defaultStreamLimit, boolean thrownOnMaxSelectSize, Duration timeout) {
    this.maxSelectSize = maxSelectSize;
    this.defaultSelectSize = defaultSelectSize;
    this.maxLimit = maxLimit;
    this.defaultLimit = defaultLimit;
    this.defaultStreamLimit = defaultStreamLimit;
    this.thrownOnMaxSelectSize = thrownOnMaxSelectSize;
    this.timeout = timeout;
  }

  public int getDefaultLimit() {
    return defaultLimit;
  }

  public int getDefaultSelectSize() {
    return defaultSelectSize;
  }

  public int getDefaultStreamLimit() {
    return defaultStreamLimit;
  }

  public int getMaxLimit() {
    return maxLimit;
  }

  public int getMaxSelectSize() {
    return maxSelectSize;
  }

  public Duration getTimeout() {
    return timeout;
  }

  public boolean isThrownOnMaxSelectSize() {
    return thrownOnMaxSelectSize;
  }

  @Override
  public void onPostConstruct(Config config, String key) {
    if (maxSelectSize <= 0) {
      maxSelectSize = UN_LIMIT_SELECT_SIZE;
      logger.warning(() -> String.format(
          "The value of querier config [max select size] was revised, the value is %s",
          maxSelectSize));
    }
    if (defaultSelectSize <= 0) {
      defaultSelectSize = 128;
      logger.warning(() -> String.format(
          "The value of querier config [default select size] was revised, the value is %s",
          defaultSelectSize));
    }
    if (defaultSelectSize > maxSelectSize) {
      defaultSelectSize = maxSelectSize;
      logger.warning(() -> String.format(
          "The value of querier config [default select size] is greater than the value of [max select size], the value was revised, the value is %s",
          defaultSelectSize));
    }
    if (maxLimit <= 0) {
      maxLimit = UN_LIMIT_SELECT_SIZE;
      logger.warning(() -> String.format(
          "The value of querier config [max limit] was revised, the value is %s", maxLimit));
    }
    if (defaultLimit <= 0) {
      defaultLimit = 16;
      logger.warning(() -> String.format(
          "The value of querier config [default limit] was revised, the value is %s",
          defaultLimit));
    }
    if (defaultLimit > maxLimit) {
      defaultLimit = maxLimit;
      logger.warning(() -> String.format(
          "The value of querier config [default limit] is greater than the value of [max limit], the value was revised, the value is %s",
          defaultLimit));
    }
    if (defaultStreamLimit <= 0) {
      defaultStreamLimit = 32;
      logger.warning(() -> String.format(
          "The value of querier config [default stream limit] was revised, the value is %s",
          defaultStreamLimit));
    }
  }

  public void setDefaultLimit(int defaultLimit) {
    this.defaultLimit = defaultLimit;
  }

  public void setDefaultSelectSize(int defaultSelectSize) {
    this.defaultSelectSize = defaultSelectSize;
  }

  public void setDefaultStreamLimit(int defaultStreamLimit) {
    this.defaultStreamLimit = defaultStreamLimit;
  }

  public void setMaxLimit(int maxLimit) {
    this.maxLimit = maxLimit;
  }

  public void setMaxSelectSize(int maxSelectSize) {
    this.maxSelectSize = maxSelectSize;
  }

  public void setThrownOnMaxSelectSize(boolean thrownOnMaxSelectSize) {
    this.thrownOnMaxSelectSize = thrownOnMaxSelectSize;
  }

  public void setTimeout(Duration timeout) {
    this.timeout = timeout;
  }

}
