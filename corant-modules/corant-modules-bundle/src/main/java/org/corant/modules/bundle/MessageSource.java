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
package org.corant.modules.bundle;

import java.util.Locale;
import java.util.function.Function;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-bundle
 *
 * <p>
 * Interface used to retrieve messages, support for the internationalization of such messages.
 *
 * @author bingo 下午3:45:34
 *
 */
public interface MessageSource extends Sortable, AutoCloseable {

  @Override
  default void close() throws Exception {}

  String getMessage(Locale locale, Object key) throws NoSuchBundleException;

  String getMessage(Locale locale, Object key, Function<Locale, String> defaultMessage);

  default void refresh() {}

  /**
   * corant-modules-bundle
   *
   * @author bingo 下午7:46:22
   *
   */
  class MessageSourceRefreshedEvent {

    final MessageSource source;

    public MessageSourceRefreshedEvent(MessageSource source) {
      this.source = source;
    }

    protected MessageSource getSource() {
      return source;
    }

  }
}
