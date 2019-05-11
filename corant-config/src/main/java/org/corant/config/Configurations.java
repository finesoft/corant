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
package org.corant.config;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.MapUtils.mapOf;
import static org.corant.shared.util.StringUtils.defaultString;
import static org.corant.shared.util.StringUtils.group;
import static org.corant.shared.util.StringUtils.split;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.corant.shared.normal.Names;
import org.corant.shared.normal.Names.ConfigNames;
import org.eclipse.microprofile.config.Config;

/**
 * corant-config
 *
 * @author bingo 下午3:23:28
 *
 */
public class Configurations {

  public static final String SEPARATOR = String.valueOf(Names.NAME_SPACE_SEPARATOR);

  public static void adjust(Object... props) {
    Map<String, String> map = mapOf(props);
    map.forEach((k, v) -> System.setProperty(ConfigNames.CFG_ADJUST_PREFIX + defaultString(k), v));
  }

  public static Map<String, List<String>> getGroupConfigNames(Config config,
      Predicate<String> filter, int keyIndex) {
    return getGroupConfigNames(config.getPropertyNames(), filter, keyIndex);
  }

  public static Map<String, List<String>> getGroupConfigNames(Config config, String prefix,
      int keyIndex) {
    return getGroupConfigNames(config.getPropertyNames(), prefix, keyIndex);
  }

  public static Map<String, List<String>> getGroupConfigNames(Iterable<String> configs,
      Predicate<String> filter, int keyIndex) {
    shouldBeTrue(keyIndex >= 0);
    return group(configs, s -> filter.test(s), s -> {
      String[] arr = split(s, SEPARATOR, true, true);
      if (arr.length > keyIndex) {
        return new String[] {arr[keyIndex], s};
      }
      return new String[0];
    });
  }

  public static Map<String, List<String>> getGroupConfigNames(Iterable<String> configs,
      String prefix, int keyIndex) {
    return getGroupConfigNames(configs, s -> defaultString(s).startsWith(prefix), keyIndex);
  }

}
