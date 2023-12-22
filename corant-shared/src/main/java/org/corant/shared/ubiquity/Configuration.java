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
import static org.corant.shared.util.Classes.defaultClassLoader;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.parseDollarTemplate;
import java.util.Set;
import org.corant.shared.util.Services;
import org.corant.shared.util.Systems;

/**
 * corant-shared
 *
 * @author bingo 上午10:58:29
 */
public interface Configuration extends Sortable {

  Configuration INSTANCE = Services.findRequired(Configuration.class, defaultClassLoader())
      .orElseGet(SystemConfiguration::new);

  /**
   * Returns whether the configuration contains the given key.
   *
   * @param key the configuration key to search
   */
  default boolean containsKey(String key) {
    return false;
  }

  /**
   * Returns the assembled configuration property value. According to the input string, analyze
   * whether the value contains the configuration property name variable, for example:
   * '${property.name}', if the property name variable exists, replace the name variable with the
   * relevant configuration property value, if without the property name variable, the passed value
   * is not changed, and the parsed-replaced string is finally returned, which is used to enhance
   * some of the annotated configuration flexibility.
   *
   * @param value the configuration property key or the original value
   * @return the assembled value or the original given value if it can't be assembled
   */
  String getAssembledValue(String value);

  /**
   * Returns all configuration keys
   */
  default Set<String> getKeys() {
    return emptySet();
  }

  /**
   * Get the configuration property value, if the property doesn't exist return null.
   *
   * @param <T> the property value type
   * @param name the property name
   * @param valueType the property value class
   * @return the relevant configuration property value, type conversion is performed when necessary
   */
  <T> T getValue(String name, Class<T> valueType);

  /**
   * Get the configuration property value, if the property doesn't exist return the given default
   * value.
   *
   * @param <T> the property value type
   * @param name the property name
   * @param valueType the property value class
   * @param nvl default value.
   * @return the relevant configuration property value, type conversion is performed when necessary
   */
  default <T> T getValue(String name, Class<T> valueType, T nvl) {
    return defaultObject(getValue(name, valueType), nvl);
  }

  /**
   * Get the configuration property value, if the property doesn't exist return null.
   *
   * @param <T> the property value type
   * @param name the property name
   * @param valueTypeLiteral the property value type literal
   * @return the relevant configuration property value, type conversion is performed when necessary
   */
  <T> T getValue(String name, TypeLiteral<T> valueTypeLiteral);

  /**
   * Get the configuration property value, if the property doesn't exist return the given default
   * value.
   *
   * @param <T> the property value type
   * @param name the property name
   * @param valueTypeLiteral the property value type literal
   * @param nvl default value.
   * @return the relevant configuration property value, type conversion is performed when necessary
   */
  default <T> T getValue(String name, TypeLiteral<T> valueTypeLiteral, T nvl) {
    return defaultObject(getValue(name, valueTypeLiteral), nvl);
  }

  /**
   * corant-shared
   *
   * @author bingo 上午11:05:12
   */
  class SystemConfiguration implements Configuration {

    @Override
    public String getAssembledValue(String value) {
      return parseDollarTemplate(value, k -> getValue(k, String.class));
    }

    @Override
    public <T> T getValue(String name, Class<T> valueType) {
      return toObject(getValue(name), valueType);
    }

    @Override
    public <T> T getValue(String name, TypeLiteral<T> valueTypeLiteral) {
      return toObject(getValue(name), valueTypeLiteral);
    }

    protected String getValue(String name) {
      return defaultObject(Systems.getProperty(name), () -> Systems.getEnvironmentVariable(name));
    }
  }
}
