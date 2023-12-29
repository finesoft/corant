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
import org.corant.shared.normal.Names;

/**
 * corant-modules-bundle
 *
 * <p>
 * Interface used to resolving messages, support for the parameterization and internationalization
 * of such messages.
 *
 * @author bingo 下午9:28:49
 */
public interface MessageResolver extends AutoCloseable {

  String UNKNOWN_INF_KEY = "INF.message.unknown";
  String UNKNOWN_ERR_KEY = "ERR.message.unknown";
  String UNKNOWN_DES_KEY = "DES.message.unknown";

  static String getNoFoundMessage(Locale locale, Object key) {
    return String.format("Can't find any message for %s.", key);
  }

  /**
   * Try to resolve the message with given parameters.
   *
   * @param locale the locale in which to do the lookup
   * @param key key the message key to lookup
   * @param params an array of context arguments that will be used to generate a message.
   * @return the resolved message if the lookup was successful otherwise return null
   */
  default String getMessage(Locale locale, Object key, Object... params) {
    if (key == null) {
      return null;
    }
    return getMessage(locale, key, params, l -> null);
  }

  /**
   * Try to resolve the message with given parameters. Return default message if no message was
   * found.
   *
   * @param locale the locale in which to do the lookup
   * @param key key the message key to lookup
   * @param params an array of context arguments that will be used to generate a message.
   * @param failLookupHandler a default message callback is used to return default message if the
   *        lookup fails
   * @return the resolved message if the lookup was successful, otherwise return the default
   *         message.
   */
  String getMessage(Locale locale, Object key, Object[] params,
      Function<Locale, String> failLookupHandler);

  /**
   * corant-modules-bundle
   *
   * <p>
   * Message category, mainly used to distinguish message keys, etc.
   *
   * @author bingo 下午2:54:09
   *
   */
  enum MessageCategory {
    INF, ERR, DES;

    public String genMessageKey(Object key) {
      if (key == null) {
        return null;
      } else {
        return name() + Names.NAME_SPACE_SEPARATORS + key;
      }
    }
  }

}
