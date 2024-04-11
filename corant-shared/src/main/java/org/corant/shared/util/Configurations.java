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
package org.corant.shared.util;

import static org.corant.shared.util.Classes.defaultClassLoader;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.isNotBlank;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.corant.shared.ubiquity.Configuration;
import org.corant.shared.ubiquity.Configuration.DefaultConfiguration;
import org.corant.shared.ubiquity.TypeLiteral;
import org.corant.shared.util.Strings.WildcardMatcher;

/**
 * corant-shared
 *
 * @author bingo 上午11:12:33
 */
public class Configurations {

  public static final Configuration INSTANCE = Services
      .findRequired(Configuration.class, defaultClassLoader()).orElseGet(DefaultConfiguration::new);

  /**
   * Returns the assembled configuration values. According to the input value, analyze whether the
   * value contains the configuration key variable, for example:'$ {key}'. If the key variable
   * exists, get the relevant configuration value, and then use '{@code ,}' to divide the value into
   * the array, use the array property values replace the key variables of the input value
   * separately and then return the assembled values. If there is no key variable, the passed value
   * is not changed, and it is returned directly. This is used to enhance some annotated
   * configuration flexibility.
   *
   * @param values the original values
   * @return assemblyStringConfigProperty
   */
  public static String[] getAllAssembledConfigValues(String[] values) {
    if (isEmpty(values)) {
      return values;
    } else {
      String[] resolves = new String[values.length];
      Arrays.setAll(resolves, i -> getAssembledConfigValue(values[i]));
      return resolves;
    }
  }

  /**
   * Returns the assembled configuration value. According to the input string, analyze whether the
   * value contains the configuration key variable, for example: '${key}', if the key variable
   * exists, replace the key variable with the relevant configuration value, if without the key
   * variable, the passed value is not changed, otherwise the parsed-replaced string is finally
   * returned, which is used to enhance some of the annotated configuration flexibility.
   *
   * @param value the configuration key or the original value
   * @return the assembled value or the original given value if it can't be assembled
   */
  public static String getAssembledConfigValue(String value) {
    return getConfig().getAssembledValue(value);
  }

  /**
   * Returns the assembled configuration value list. According to the input value, analyze whether
   * the value contains the configuration key variable, for example:'$ {key}'. If the key variable
   * exists, get the relevant configuration value, and then use '{@code ,}' to divide the value into
   * a list and return it. If there is no attribute name variable, the passed value is not changed,
   * and it is returned directly. This is used to enhance some annotated configuration flexibility.
   *
   * @param value the configuration key or the original value
   * @return assembled values or the original given values if it can't be assembled
   */
  public static List<String> getAssembledConfigValues(String value) {
    return getConfig().getAssembledValues(value);
  }

  /**
   * Returns the configuration. The default implementation takes all configuration values from
   * system properties and system environment variables. It is generally used in conjunction with
   * other more comprehensive configuration components, such as the eclipse microprofile-config.
   */
  public static Configuration getConfig() {
    return INSTANCE;
  }

  /**
   * Returns the configuration value by the given key, if the configuration value doesn't exist
   * return null.
   *
   * @param key the configuration key
   * @return the relevant configuration value
   */
  public static String getConfigValue(String key) {
    return getConfig().getValue(key);
  }

  /**
   * Returns the configuration value by the given key, if the configuration doesn't exist return
   * null.
   *
   * @param <T> the configuration value type
   * @param key the configuration key
   * @param valueType the configuration value class
   * @return the relevant configuration value, type conversion is performed when necessary
   */
  public static <T> T getConfigValue(String key, Class<T> valueType) {
    return getConfig().getValue(key, valueType);
  }

  /**
   * Returns the configuration value by the given key, if the key doesn't exist or the value is null
   * return the given default value.
   *
   * @param <T> the configuration value type
   * @param key the configuration key
   * @param valueType the configuration value class
   * @param nvl default value.
   * @return the relevant configuration value, type conversion is performed when necessary
   */
  public static <T> T getConfigValue(String key, Class<T> valueType, T nvl) {
    return getConfig().getValue(key, valueType, nvl);
  }

  /**
   * Returns the configuration value by the given key, if the configuration doesn't exist return
   * null.
   *
   * @param <T> the configuration value type
   * @param key the configuration key
   * @param valueTypeLiteral the configuration value type literal
   * @return the relevant configuration value, type conversion is performed when necessary
   */
  public static <T> T getConfigValue(String key, TypeLiteral<T> valueTypeLiteral) {
    return getConfig().getValue(key, valueTypeLiteral);
  }

  /**
   * Returns the configuration value by the given key, if the key doesn't exist or the value is null
   * return the given default value.
   *
   * @param <T> the configuration value type
   * @param key the configuration key
   * @param valueTypeLiteral the configuration value type literal
   * @param nvl default value.
   * @return the relevant configuration value, type conversion is performed when necessary
   */
  public static <T> T getConfigValue(String key, TypeLiteral<T> valueTypeLiteral, T nvl) {
    return getConfig().getValue(key, valueTypeLiteral, nvl);
  }

  /**
   * Return the resolved configuration values with the specified type for the specified key from the
   * underlying configuration sources.
   *
   * @param <T> the configuration value type
   * @param key the configuration key
   * @param valueType the value type
   */
  public static <T> List<T> getConfigValues(String key, Class<T> valueType) {
    return getConfig().getValues(key, valueType);
  }

  /**
   * Returns all raw configuration values that match any given key or key wildcard.
   *
   * @param keyOrWildcards key or key wildcard
   * @return the matched raw stream
   */
  public static Stream<String> searchConfigValues(String... keyOrWildcards) {
    if (keyOrWildcards.length == 0) {
      return Stream.empty();
    }
    Predicate<String> predicate = s -> false;
    for (String k : keyOrWildcards) {
      if (WildcardMatcher.hasWildcard(k)) {
        predicate = predicate.or(WildcardMatcher.of(false, k));
      } else if (isNotBlank(k)) {
        predicate = predicate.or(t -> areEqual(t, k));
      }
    }
    return streamOf(getConfig().getKeys()).filter(predicate)
        .map(pn -> getConfigValue(pn, String.class)).filter(Objects::isNoneNull);

  }
}
