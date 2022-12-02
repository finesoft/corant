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

import static org.corant.shared.util.Strings.isNotBlank;
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
 *
 */
public interface MessageResolver extends AutoCloseable {

  String UNKNOW_INF_CODE = "INF.message.unknow";
  String UNKNOW_ERR_CODE = "ERR.message.unknow";
  String UNKNOW_DES_CODE = "DES.message.unknow";

  static String getNoFoundMessage(Locale locale, Object codes) {
    return String.format("Can't find any message for %s.", codes);
  }

  /**
   * Try to resolve the message with given parameters.
   *
   * @param locale the locale in which to do the lookup
   * @param code code the message code to lookup
   * @param params an array of context arguments that will be used to generate a message.
   * @return the resolved message if the lookup was successful otherwise retrun null
   */
  default String getMessage(Locale locale, Object code, Object... params) {
    if (code == null) {
      return null;
    }
    return getMessage(locale, code, params, l -> null);
  }

  /**
   * Try to resolve the message with given parameters. Return default message if no message was
   * found.
   *
   * @param locale the locale in which to do the lookup
   * @param code code the message code to lookup
   * @param params an array of context arguments that will be used to generate a message.
   * @param dfltMsg a default message callback is used to return default message if the lookup fails
   * @return the resolved message if the lookup was successful, otherwise return the default
   *         message.
   */
  String getMessage(Locale locale, Object code, Object[] params, Function<Locale, String> dfltMsg);

  /**
   * Used to refresh the cache, if the implementation uses a caching mechanism.
   */
  default void refresh() {}

  /**
   * corant-modules-bundle
   *
   * <p>
   * Message category, mainly used to distinguish message codes, etc.
   *
   * @author bingo 下午2:54:09
   *
   */
  enum MessageCategory {
    INF, ERR, DES;

    public String genMessageCode(Object... codes) {
      StringBuilder sb = new StringBuilder(name());
      for (Object code : codes) {
        String cs;
        if (code != null && isNotBlank(cs = code.toString())) {
          sb.append(Names.NAME_SPACE_SEPARATORS).append(cs);
        }
      }
      return sb.toString();
    }
  }

}
