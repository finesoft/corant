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
package org.corant.shared.ubiquity;

import static java.util.Collections.emptySet;
import static org.corant.shared.util.Conversions.toList;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.parseDollarTemplate;
import static org.corant.shared.util.Strings.split;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.corant.shared.util.Iterables;
import org.corant.shared.util.Sets;
import org.corant.shared.util.Systems;

/**
 * corant-shared
 *
 * @author bingo 上午10:58:29
 */
public interface Configuration extends Sortable {

  /**
   * Returns whether the configuration contains the given key.
   *
   * @param key the configuration key to search
   */
  default boolean containsKey(String key) {
    return false;
  }

  /**
   * Returns the assembled configuration value. According to the input string, analyze whether the
   * value contains the configuration key variable, for example: '${key}', if the key variable
   * exists, replace the key variable with the relevant configuration value, if without the key
   * variable, the passed value is not changed, and the parsed-replaced string is finally returned,
   * which is used to enhance some of the annotated configuration flexibility.
   *
   * @param value the configuration key or the original value
   * @return the assembled value or the original given value if it can't be assembled
   */
  String getAssembledValue(String value);

  /**
   * Returns all configuration keys
   */
  default Iterable<String> getKeys() {
    return emptySet();
  }

  /**
   * Get the configuration value by key, if the configuration doesn't exist return null.
   *
   * @param <T> the configuration value type
   * @param key the configuration key
   * @param valueType the configuration value class
   * @return the relevant configuration value, type conversion is performed when necessary
   */
  <T> T getValue(String key, Class<T> valueType);

  /**
   * Get the configuration value by key, if the key doesn't exist or the value is null return the
   * given default value.
   *
   * @param <T> the configuration value type
   * @param key the configuration key
   * @param valueType the configuration value class
   * @param nvl default value.
   * @return the relevant configuration value, type conversion is performed when necessary
   */
  default <T> T getValue(String key, Class<T> valueType, T nvl) {
    return defaultObject(getValue(key, valueType), nvl);
  }

  /**
   * Get the configuration value by key, if the configuration doesn't exist return null.
   *
   * @param <T> the configuration value type
   * @param key the configuration key
   * @param valueTypeLiteral the configuration value type literal
   * @return the relevant configuration value, type conversion is performed when necessary
   */
  <T> T getValue(String key, TypeLiteral<T> valueTypeLiteral);

  /**
   * Get the configuration value by key, if the key doesn't exist or the value is null return the
   * given default value.
   *
   * @param <T> the configuration value type
   * @param name the configuration key
   * @param valueTypeLiteral the configuration value type literal
   * @param nvl default value.
   * @return the relevant configuration value, type conversion is performed when necessary
   */
  default <T> T getValue(String name, TypeLiteral<T> valueTypeLiteral, T nvl) {
    return defaultObject(getValue(name, valueTypeLiteral), nvl);
  }

  /**
   * Return the resolved configuration values with the specified type for the specified key from the
   * underlying configuration sources.
   *
   * @param <T> the configuration value type
   * @param key the configuration key
   * @param valueType the value type
   */
  default <T> List<T> getValues(String key, Class<T> valueType) {
    Object object = getValue(key, Object.class);
    if (object instanceof String string) {
      String[] arrays = split(string, ",");
      return toList(arrays, valueType);
    } else if (object instanceof Collection<?> collection) {
      return toList(collection, valueType);
    } else if (object != null) {
      return listOf(toObject(object, valueType));
    }
    return Collections.emptyList();
  }

  /**
   * corant-shared
   * <p>
   * The default Configuration implementation which use system properties and system environment
   * variables as configuration source.
   *
   * @author bingo 上午11:05:12
   */
  class SystemConfiguration implements Configuration {

    @Override
    public boolean containsKey(String key) {
      return System.getProperties().containsKey(key) || System.getenv().containsKey(key);
    }

    @Override
    public String getAssembledValue(String value) {
      return parseDollarTemplate(value, k -> getValue(k, String.class));
    }

    @Override
    public Iterable<String> getKeys() {
      return Iterables.concat(
          Sets.transform(System.getProperties().keySet(), t -> t == null ? null : t.toString()),
          System.getenv().keySet());
    }

    @Override
    public <T> T getValue(String key, Class<T> valueType) {
      return toObject(getValue(key), valueType);
    }

    @Override
    public <T> T getValue(String key, TypeLiteral<T> valueTypeLiteral) {
      return toObject(getValue(key), valueTypeLiteral);
    }

    protected String getValue(String key) {
      return defaultObject(Systems.getProperty(key), () -> Systems.getEnvironmentVariable(key));
    }
  }
}
