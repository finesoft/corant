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
import static java.util.Collections.singletonList;
import static org.corant.shared.util.Conversions.toList;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.escapedCommaSplit;
import static org.corant.shared.util.Strings.splitAs;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Names.ConfigNames;
import org.corant.shared.resource.Resource;
import org.corant.shared.util.Iterables;
import org.corant.shared.util.Objects;
import org.corant.shared.util.Resources;
import org.corant.shared.util.Services;
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
   * variable, the passed value is not changed, otherwise the parsed-replaced string is finally
   * returned, which is used to enhance some of the annotated configuration flexibility.
   *
   * @param value the configuration key or the original value
   * @return the assembled value or the original given value if it can't be assembled
   */
  String getAssembledValue(String value);

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
  default List<String> getAssembledValues(String value) {
    String actualValue = getAssembledValue(value);
    if (actualValue != null) {
      return Arrays.asList(escapedCommaSplit(actualValue));
    }
    return singletonList(value);
  }

  /**
   * Returns all configuration keys
   */
  default Iterable<String> getKeys() {
    return emptySet();
  }

  /**
   * Get the configuration value by key, if the configuration doesn't exist return null.
   *
   * @param key the configuration key
   * @return the relevant configuration value
   */
  String getValue(String key);

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
      String[] arrays = escapedCommaSplit(string);
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
   * A configuration source interface which used to load custom configuration key & values. The
   * system uses java.util.ServiceLoader to load the implementations.
   * <p>
   * Services path: META-INF/services/org.corant.shared.ubiquity.Configuration$ConfigurationSource
   *
   * @author bingo 11:19:55
   */
  interface ConfigurationSource
      extends Comparable<ConfigurationSource>, Iterable<String>, Sortable {

    String configOrdinalKey = "config_ordinal";

    @Override
    default int compareTo(ConfigurationSource o) {
      return Sortable.reverseCompare(this, o);
    }

    default boolean containsKey(String key) {
      if (key != null) {
        for (String k : getKeys()) {
          if (key.equals(k)) {
            return true;
          }
        }
      }
      return false;
    }

    Iterable<String> getKeys();

    String getValue(String key);

    @Override
    default Iterator<String> iterator() {
      return getKeys().iterator();
    }

  }

  /**
   * corant-shared
   * <p>
   * Default configuration implementation using system properties and system environment variables
   * or configured property file paths as the configuration source.
   *
   * @author bingo 上午11:05:12
   */
  class DefaultConfiguration implements Configuration {

    protected List<ConfigurationSource> sources = new ArrayList<>();

    public DefaultConfiguration() {
      sources.add(new SystemPropertyConfigurationSource());
      sources.add(new SystemEnvironmentConfigurationSource());
      Services.selectRequired(ConfigurationSource.class).forEach(sources::add);
      Stream<Resource> resources = splitAs(
          defaultObject(Systems.getProperty(ConfigNames.CFG_LOCATION_KEY),
              () -> Systems.getEnvironmentVariable(ConfigNames.CFG_LOCATION_KEY)),
          ",", String.class, HashSet::new).stream().flatMap(Resources::tryFrom);
      resources.filter(Objects::isNotNull).map(PropertyResourceBundleConfigurationSource::new)
          .forEach(sources::add);
      Collections.sort(sources);
    }

    @Override
    public boolean containsKey(String key) {
      if (key != null) {
        for (ConfigurationSource source : sources) {
          if (source.containsKey(key)) {
            return true;
          }
        }
      }
      return false;
    }

    @Override
    public String getAssembledValue(String value) {
      return StringTemplate.DEFAULT.parse(value, k -> getValue(k, String.class));
    }

    @Override
    public Iterable<String> getKeys() {
      return () -> sources.stream().map(it -> StreamSupport.stream(it.spliterator(), false))
          .reduce(Stream::concat).orElseGet(Stream::empty).iterator();
    }

    @Override
    public String getValue(String key) {
      return StringTemplate.DEFAULT.parse(getRawValue(key), this::getRawValue);
    }

    @Override
    public <T> T getValue(String key, Class<T> valueType) {
      return toObject(getValue(key), valueType);
    }

    @Override
    public <T> T getValue(String key, TypeLiteral<T> valueTypeLiteral) {
      return toObject(getValue(key), valueTypeLiteral);
    }

    protected String getRawValue(String key) {
      for (ConfigurationSource s : sources) {
        String value = s.getValue(key);
        if (value != null) {
          return value;
        }
      }
      return null;
    }

    /**
     * corant-shared
     *
     * @author bingo 11:22:01
     */
    protected static class PropertyResourceBundleConfigurationSource
        implements ConfigurationSource {

      protected final PropertyResourceBundle bundle;

      public PropertyResourceBundleConfigurationSource(Resource resource) {
        try (InputStream is = resource.openInputStream();
            InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
          bundle = new PropertyResourceBundle(isr);
        } catch (IOException e) {
          throw new CorantRuntimeException(e,
              "Load configuration property resource bundle %s error!", resource.getLocation());
        }
      }

      @Override
      public boolean containsKey(String key) {
        if (key != null) {
          return bundle.containsKey(key);
        }
        return false;
      }

      @Override
      public Iterable<String> getKeys() {
        return bundle.keySet();
      }

      @Override
      public int getPriority() {
        if (bundle.containsKey(configOrdinalKey)) {
          String configOrdinal = getValue(configOrdinalKey);
          if (configOrdinal != null) {
            try {
              return Integer.parseInt(configOrdinal);
            } catch (NumberFormatException ignored) {
            }
          }
        }
        return 100;
      }

      @Override
      public String getValue(String key) {
        if (bundle.containsKey(key)) {
          return bundle.getString(key);
        }
        return null;
      }
    }

    /**
     * corant-shared
     *
     * @author bingo 11:22:19
     */
    protected static class SystemEnvironmentConfigurationSource implements ConfigurationSource {

      @Override
      public boolean containsKey(String key) {
        return getValue(key) != null;
      }

      @Override
      public Iterable<String> getKeys() {
        return System.getenv().keySet();
      }

      @Override
      public int getPriority() {
        String configOrdinal = getValue(configOrdinalKey);
        if (configOrdinal != null) {
          try {
            return Integer.parseInt(configOrdinal);
          } catch (NumberFormatException ignored) {
          }
        }
        return 300;
      }

      @Override
      public String getValue(String key) {
        return Systems.getEnvironmentVariable(key);
      }
    }

    /**
     * corant-shared
     *
     * @author bingo 11:22:01
     */
    protected static class SystemPropertyConfigurationSource implements ConfigurationSource {

      @Override
      public boolean containsKey(String key) {
        if (key != null) {
          return System.getProperties().containsKey(key);
        }
        return false;
      }

      @Override
      public Iterable<String> getKeys() {
        return Iterables.transform(System.getProperties().keySet(),
            t -> t == null ? null : t.toString());
      }

      @Override
      public int getPriority() {
        String configOrdinal = getValue(configOrdinalKey);
        if (configOrdinal != null) {
          try {
            return Integer.parseInt(configOrdinal);
          } catch (NumberFormatException ignored) {
          }
        }
        return 400;
      }

      @Override
      public String getValue(String key) {
        return Systems.getProperty(key);
      }
    }
  }
}
