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
package org.corant.modules.bundle;

import java.util.stream.Stream;

/**
 * corant-modules-bundle
 * <p>
 * Interface use to manage or obtain all message source, for example refresh(same as loading or
 * reloading) or release all the message sources.
 *
 * @author bingo 上午10:12:28
 *
 */
public interface MessageSourceManager {

  /**
   * Refreshes (same as loading or reloading) all message sources. For example, an implementation
   * may use a caching mechanism to improve performance, and this method can be used to reload the
   * cache.
   */
  default void refresh() {}

  /**
   * Used to release all message sources, generally called when the application is about to
   * shutdown. Release can be implemented in many ways. For example, some the message sources may
   * hold an underlying resource, and calling this method can release related resources.
   */
  default void release() {}

  /**
   * Returns an ordered stream of all message sources
   */
  default Stream<MessageSource> stream() {
    return Stream.empty();
  }
}
