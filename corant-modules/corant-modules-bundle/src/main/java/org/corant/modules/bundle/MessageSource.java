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

import static java.util.Collections.emptySet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-bundle
 *
 * <p>
 * Interface used to retrieve messages, support for the internationalization of such messages.
 *
 * @author bingo 下午3:45:34
 */
public interface MessageSource extends Sortable, AutoCloseable {

  @Override
  default void close() throws Exception {}

  /**
   * Returns all message keys related to the given locale for this resource.
   *
   * @param locale the locale to retrieve, can't null
   * @return all the message keys related to the given locale
   */
  default Set<String> getKeys(Locale locale) {
    return emptySet();
  }

  /**
   * Returns the locales supported by this resource.
   */
  default Set<Locale> getLocales() {
    return emptySet();
  }

  /**
   * Try to resolve the message.
   *
   * @param locale the locale in which to do the lookup
   * @param key key the message key to lookup
   * @return the resolved message if the lookup was successful otherwise throws
   *         NoSuchBundleException
   */
  String getMessage(Locale locale, Object key) throws NoSuchBundleException;

  /**
   * Try to resolve the message. Return default message if no message was found.
   *
   * @param locale the locale in which to do the lookup
   * @param key key the message key to lookup
   * @param defaultMessage a default message callback is used to return default message if the
   *        lookup fails
   * @return the resolved message if the lookup was successful, otherwise return the default
   *         message.
   */
  String getMessage(Locale locale, Object key, Function<Locale, String> defaultMessage);

  /**
   * Refresh underling caching if necessary, usually all callers that use message source may perform
   * a refresh before starting to use.
   */
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
