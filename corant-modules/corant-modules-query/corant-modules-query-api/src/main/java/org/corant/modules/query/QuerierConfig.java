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
package org.corant.modules.query;

import java.time.Duration;

/**
 * corant-modules-query-api
 *
 * @author bingo 下午4:06:43
 *
 */
public interface QuerierConfig {

  // Use to cover the global 'corant.query.querier' configuration, they may be assigned in query xml
  String CTX_KEY_PARALLEL_FETCH = ".parallel-fetch";
  int UN_LIMIT_SELECT_SIZE = Integer.MAX_VALUE - 16;
  String PRO_KEY_MAX_SELECT_SIZE = ".max-select-size";
  String PRO_KEY_THROWN_ON_MAX_LIMIT_SIZE = ".thrown-on-max-select-size";
  String PRO_KEY_LIMIT = ".limit";
  String PRO_KEY_STREAM_LIMIT = ".stream-limit";
  String PRO_KEY_TIMEOUT = ".timeout";

  int getDefaultLimit();

  int getDefaultSelectSize();

  int getDefaultStreamLimit();

  int getMaxLimit();

  int getMaxSelectSize();

  Duration getTimeout();

  boolean isThrownOnMaxSelectSize();
}
