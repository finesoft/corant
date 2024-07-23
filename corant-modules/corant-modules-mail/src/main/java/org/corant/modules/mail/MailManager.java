/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.mail;

import static org.corant.shared.util.Configurations.getConfig;
import static org.corant.shared.util.Configurations.getConfigValue;
import static org.corant.shared.util.Configurations.getOptionalConfigValue;
import static org.corant.shared.util.Configurations.searchConfigValues;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.EMPTY;
import static org.corant.shared.util.Strings.split;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.corant.modules.mail.MailSender.DefaultMailSender;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.ubiquity.Tuple.Pair;

/**
 * corant-modules-mail
 *
 * @author bingo 14:26:49
 */
public interface MailManager extends Sortable {

  MailManager DEFAULT_INSTANCE = new DefaultMailManager();

  Map<String, Properties> resolveConfig();

  MailReceiver resolveReceiver(Properties properties);

  MailSender resolveSender(Properties properties);

  /**
   * corant-modules-mail
   *
   * @author bingo 14:29:33
   */
  class DefaultMailManager implements MailManager {

    public static final String PROPERTIES_PREFIX = "corant.mail.";
    public static final String PROPERTIES_PATTERN = "corant.mail*.host";

    protected static final Logger logger = Logger.getLogger(MailManager.class.getName());

    @Override
    public Map<String, Properties> resolveConfig() {
      Map<String, Properties> propertiesMap = new HashMap<>();
      Set<String> prefixes = searchConfigValues(PROPERTIES_PATTERN).map(Pair::key)
          .map(k -> k.substring(0, k.length() - 5)).collect(Collectors.toSet());
      for (String prefix : prefixes) {
        String[] s = split(prefix.substring(11), ".", true, true);// remove 'corant.mail.'
        String key = s.length > 1 ? s[0] : EMPTY;
        final String rootPrefix;
        final int startIndex;
        if (key.isEmpty()) {
          startIndex = 12; // starts with 'corant.mail.'
          rootPrefix = PROPERTIES_PREFIX;
        } else {
          startIndex = 13 + key.length(); // start with 'corant.mail.???'
          rootPrefix = PROPERTIES_PREFIX + key + ".";
        }
        Properties pros = new Properties();

        // set username & password
        getOptionalConfigValue(rootPrefix + "username", String.class)
            .ifPresent(x -> pros.put("mail.username", x));
        getOptionalConfigValue(rootPrefix + "password", String.class)
            .ifPresent(x -> pros.put("mail.password", x));

        streamOf(getConfig().getKeys()).filter(k -> k.startsWith(prefix)).forEach(k -> {
          String v = getConfigValue(k, String.class);
          if (v != null) {
            pros.put("mail." + k.substring(startIndex), v);
          }
        });
        if (isNotEmpty(pros)) {
          propertiesMap.put(key, pros);
        }
      }
      return propertiesMap;
    }

    @Override
    public MailReceiver resolveReceiver(Properties properties) {
      throw new NotSupportedException();
    }

    @Override
    public MailSender resolveSender(Properties properties) {
      return new DefaultMailSender(properties);
    }

  }
}
