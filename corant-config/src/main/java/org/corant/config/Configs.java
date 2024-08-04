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

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Maps.immutableMap;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.EMPTY;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.parseDollarTemplate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.corant.config.declarative.ConfigInstances;
import org.corant.config.declarative.ConfigMetaClass;
import org.corant.config.declarative.ConfigMetaResolver;
import org.corant.config.declarative.DeclarativeConfig;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.TypeLiteral;
import org.corant.shared.util.Strings.WildcardMatcher;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperties;

/**
 * corant-config
 * <p>
 * A convenient class use to retrieve the configuration property from Microprofile-config or corant
 * declarative configuration.
 *
 * @author bingo 下午4:52:17
 */
public class Configs {

  /**
   * Returns the assembled configuration properties from Microprofile-config. According to the input
   * value, analyze whether the value contains the configuration property name variable, for
   * example:'$ {property.name}'. If the property name variable exists, get the relevant
   * configuration property value, and then divide the property value into the array according to
   * the Microprofile-config multi-value specification, use the array property values replace the
   * name variables of the input value separately and then return the assembled values. If there is
   * no attribute name variable, the passed value is not changed, and it is returned directly. This
   * is used to enhance some annotated configuration flexibility.
   * <p>
   * NOTE: This function only supports at most one variable
   * </p>
   *
   * @param value the configuration property key or the original value
   * @return the assembled values or the original given values if it can't expand
   *
   * @see Configs#assemblyStringConfigProperty(String)
   * @see ConfigProvider
   * @see Config
   */
  public static List<String> assemblyStringConfigProperties(String value) {
    String useKey = assemblyStringConfigProperty(value);
    if (useKey != null) {
      return Arrays.asList(CorantConfigResolver.splitValue(useKey));
    }
    return singletonList(value);
  }

  /**
   * Returns an assembled configuration properties from Microprofile-config. According to the input
   * values, and analyze whether the values contain the configuration property name variable, for
   * example:'${property.name}'. If the property name variable exists, replace the name variable
   * with the relevant configuration property value, and then return the assembled value, If there
   * are no property name variable, it doesn't change the passed value and directly return it. This
   * is use for enhance some annotated configuration flexibility.
   *
   * @param originals the original values
   * @return assemblyStringConfigProperty
   */
  public static String[] assemblyStringConfigProperties(String[] originals) {
    if (isEmpty(originals)) {
      return originals;
    } else {
      String[] resolves = new String[originals.length];
      Arrays.setAll(resolves, i -> Configs.assemblyStringConfigProperty(originals[i]));
      return resolves;
    }
  }

  /**
   * Returns the assembled configuration properties from Microprofile-config. According to the input
   * string, analyze whether the value contains the configuration property name variable, for
   * example: '${property.name}', if the property name variable exists, replace the name variable
   * with the relevant configuration property value, and the parsed-replaced string is finally
   * returned, if without the property name variable, the passed value is not changed and returned
   * directly. It is used to enhance some of the annotated configuration flexibility.
   *
   * @param value the configuration property key or the original value
   * @return the assembled value or the original given value if it can't expand
   * @see ConfigProvider
   * @see Config
   */
  public static String assemblyStringConfigProperty(String value) {
    if (isBlank(value)) {
      return value;
    }
    CorantConfigSources cs = ((CorantConfig) ConfigProvider.getConfig()).getCorantConfigSources();
    if (cs.isExpressionsEnabled()) {
      return cs.resolveValue(value);
    } else {
      return resolveVariable(value);
    }
  }

  /**
   * Get the optional configuration property from the {@link ConfigProvider} of Microprofile-config.
   *
   * @param <T> the property type
   * @param propertyName the property name
   * @param propertyType the property class
   */
  public static <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
    return ConfigProvider.getConfig().getOptionalValue(propertyName, propertyType);
  }

  /**
   * Get the configuration property from the {@link ConfigProvider} of Microprofile-config, if the
   * property doesn't exist return null.
   *
   * @param <T> the property type
   * @param propertyName the property name
   * @param propertyType the property class
   * @see ConfigProvider
   * @see Config
   */
  public static <T> T getValue(String propertyName, Class<T> propertyType) {
    return getValue(propertyName, propertyType, null);
  }

  /**
   * Get the configuration property from the {@link ConfigProvider} of Microprofile-config, if the
   * property doesn't exist return the given alternative {@code nvl} property value.
   *
   * @param <T> the property type
   * @param propertyName the property name
   * @param propertyType the property class
   * @param nvl the given alternative property value, if expected property doesn't exist
   * @see ConfigProvider
   * @see Config
   */
  public static <T> T getValue(String propertyName, Class<T> propertyType, T nvl) {
    Optional<T> op = ConfigProvider.getConfig().getOptionalValue(propertyName, propertyType);
    return op.orElse(nvl);
  }

  /**
   * Get the configuration property from the {@link ConfigProvider} of Microprofile-config, if the
   * property doesn't exist return null.
   *
   * @param <T> the property type
   * @param propertyName the property name
   * @param propertyTypeLiteral the property type literal
   * @see ConfigProvider
   * @see CorantConfig
   */
  public static <T> T getValue(String propertyName,
      jakarta.enterprise.util.TypeLiteral<T> propertyTypeLiteral) {
    return getValue(propertyName, propertyTypeLiteral, null);
  }

  /**
   * Get the configuration property from the {@link ConfigProvider} of Microprofile-config, if the
   * property doesn't exist return the given alternative {@code nvl} property value.
   *
   * @param <T> the property type
   * @param propertyName the property name
   * @param propertyTypeLiteral the property type literal
   * @param nvl the given alternative property value, if expected property doesn't exist
   * @see ConfigProvider
   * @see CorantConfig
   */
  public static <T> T getValue(String propertyName,
      jakarta.enterprise.util.TypeLiteral<T> propertyTypeLiteral, T nvl) {
    return ((CorantConfig) ConfigProvider.getConfig())
        .getOptionalValue(propertyName, propertyTypeLiteral).orElse(nvl);
  }

  /**
   * Get the configuration property from the {@link ConfigProvider} of Microprofile-config, if the
   * property doesn't exist return null.
   *
   * @param <T> the property type
   * @param propertyName the property name
   * @param propertyTypeLiteral the property type literal
   * @see ConfigProvider
   * @see CorantConfig
   */
  public static <T> T getValue(String propertyName, TypeLiteral<T> propertyTypeLiteral) {
    return getValue(propertyName, propertyTypeLiteral, null);
  }

  /**
   * Get the configuration property from the {@link ConfigProvider} of Microprofile-config, if the
   * property doesn't exist return the given alternative {@code nvl} property value.
   *
   * @param <T> the property type
   * @param propertyName the property name
   * @param propertyTypeLiteral the property type literal
   * @param nvl the given alternative property value, if expected property doesn't exist
   * @see ConfigProvider
   * @see CorantConfig
   */
  public static <T> T getValue(String propertyName, TypeLiteral<T> propertyTypeLiteral, T nvl) {
    return ((CorantConfig) ConfigProvider.getConfig())
        .getOptionalValue(propertyName, propertyTypeLiteral).orElse(nvl);
  }

  /**
   * Returns the microprofile configuration properties instance by given class and prefix.
   *
   * @param <T> the configuration properties instance type
   * @param cls the configuration properties instance class
   * @param prefix the prefix of the configuration properties.
   *
   * @see ConfigProperties
   * @see ConfigProvider
   * @see Config
   */
  public static <T> T resolveMicroprofile(Class<T> cls, String prefix) {
    Map<String, T> map = new HashMap<>(1);
    ConfigMetaClass configClass = ConfigMetaResolver.microprofile(cls, prefix);
    if (configClass != null) {
      Config config = ConfigProvider.getConfig();
      try {
        // FIXME EMPTY?
        map = ConfigInstances.resolveConfigInstances((CorantConfig) config, singleton(EMPTY),
            configClass);
      } catch (Exception e) {
        throw new CorantRuntimeException(e);
      }
    }
    return isEmpty(map) ? null : map.values().iterator().next();
  }

  /**
   * Returns the declarative configuration instances map by given configuration instance class. The
   * key of returns maps is the configuration instance qualifier, and the value of returns map is
   * the configuration instance. The returns map is not null and immutable.
   *
   * @param <T> the configuration instance type
   * @param cls the configuration instance class
   *
   * @see DeclarativeConfig
   */
  public static <T> Map<String, T> resolveMulti(Class<T> cls) {
    Map<String, T> configMaps = null;
    ConfigMetaClass configClass = ConfigMetaResolver.declarative(cls);
    if (configClass != null) {
      Config config = ConfigProvider.getConfig();
      try {
        configMaps = ConfigInstances.resolveConfigInstances((CorantConfig) config, configClass);
      } catch (Exception e) {
        throw new CorantRuntimeException(e);
      }
    }
    return immutableMap(configMaps);
  }

  /**
   * Returns a declarative configuration instance by given configuration instance class.
   * <p>
   * Note: If there are multiple instances then return one.
   *
   * @param <T> the configuration instance type
   * @param cls the configuration instance class
   *
   * @see DeclarativeConfig
   */
  public static <T> T resolveSingle(Class<T> cls) {
    Map<String, T> map = new HashMap<>(1);
    ConfigMetaClass configClass = ConfigMetaResolver.declarative(cls);
    if (configClass != null) {
      Config config = ConfigProvider.getConfig();
      try {
        // FIXME EMPTY?
        map = ConfigInstances.resolveConfigInstances((CorantConfig) config, singleton(EMPTY),
            configClass);
      } catch (Exception e) {
        throw new CorantRuntimeException(e);
      }
    }
    return isEmpty(map) ? null : map.values().iterator().next();
  }

  /**
   * Resolve and return the property name through the string macro templates and
   * Microprofile-config, if the given name does not contain the macro element of the string
   * template, it will return directly.
   * <p>
   *
   * <pre>
   * The string macro templates: ${...}
   * </pre>
   *
   * @param propertyName the given property name that may contain the macro element of the template.
   *
   */
  public static String resolveVariable(String propertyName) {
    return parseDollarTemplate(propertyName, k -> getValue(k, String.class));
  }

  /**
   * Returns all raw configuration property values that match any given property name or property
   * name wildcard.
   *
   * @param propertyNameOrWildcards property name or property name wildcard
   * @return the matched raw stream
   */
  public static Stream<String> searchValues(String... propertyNameOrWildcards) {
    if (propertyNameOrWildcards.length == 0) {
      return Stream.empty();
    }
    Predicate<String> predicate = s -> false;
    for (String pnow : propertyNameOrWildcards) {
      if (WildcardMatcher.hasWildcard(pnow)) {
        predicate = predicate.or(WildcardMatcher.of(false, pnow));
      } else if (isNotBlank(pnow)) {
        predicate = predicate.or(t -> areEqual(t, pnow));
      }
    }
    final Config config = ConfigProvider.getConfig();
    return streamOf(config.getPropertyNames()).filter(predicate)
        .map(pn -> config.getOptionalValue(pn, String.class)).filter(Optional::isPresent)
        .map(Optional::get);
  }

}
