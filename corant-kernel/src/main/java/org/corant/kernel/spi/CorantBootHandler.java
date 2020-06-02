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
package org.corant.kernel.spi;

import org.corant.Corant;

/**
 * corant-kernel
 *
 * @author bingo 下午2:30:23
 *
 */
public interface CorantBootHandler extends AutoCloseable {

  static int compare(CorantBootHandler h1, CorantBootHandler h2) {
    return Integer.compare(h1.getOrdinal(), h2.getOrdinal());
  }

  @Override
  default void close() throws Exception {
    return;
  }

  default int getOrdinal() {
    return 0;
  }

  void handleAfterStarted(Corant corant, String... args);

  default void handleAfterStopped(ClassLoader classLoader, String... args) {}

  void handleBeforeStart(ClassLoader classLoader, String... args);

}
