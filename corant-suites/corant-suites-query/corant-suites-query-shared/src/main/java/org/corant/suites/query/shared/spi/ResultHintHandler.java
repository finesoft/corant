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
package org.corant.suites.query.shared.spi;

import org.corant.suites.query.shared.mapping.QueryHint;

/**
 * corant-suites-query
 *
 * @author bingo 上午11:09:08
 *
 */
@FunctionalInterface
public interface ResultHintHandler extends AutoCloseable {

  static int compare(ResultHintHandler h1, ResultHintHandler h2) {
    return Integer.compare(h1.getOrdinal(), h2.getOrdinal());
  }

  default boolean canHandle(Class<?> resultClass, QueryHint qh) {
    return false;
  }

  @Override
  default void close() throws Exception {
    return;
  }

  default boolean exclusive() {
    return true;
  }

  default int getOrdinal() {
    return 0;
  }

  void handle(QueryHint qh, Object parameter, Object result) throws Exception;

  default void validate(QueryHint qh) {}

}
