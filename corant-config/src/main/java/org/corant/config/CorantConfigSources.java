/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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

import static org.corant.config.CorantConfig.CORANT_CONFIG_SOURCE_BASE_NAME_PREFIX;
import static org.corant.config.CorantConfig.MP_CONFIG_SOURCE_BASE_NAME_PREFIX;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Conversions.toBoolean;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.defaultString;
import static org.corant.shared.util.Strings.strip;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.corant.config.expression.ConfigELProcessor;
import org.corant.config.source.MicroprofileConfigSources;
import org.corant.config.spi.ConfigAdjuster;
import org.corant.shared.normal.Names;
import org.corant.shared.normal.Names.ConfigNames;
import org.corant.shared.ubiquity.Mutable.MutableBoolean;
import org.corant.shared.ubiquity.Mutable.MutableObject;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.FileUtils;
import org.corant.shared.util.Objects;
import org.corant.shared.util.Strings;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * corant-config
 * <p>
 * This class is used to organize and aggregate all configuration resources according to the
 * microprofile specification and provide a unified interface to caller.
 *
 * @author bingo 下午6:04:42
 *
 */
public class CorantConfigSources {

  public static final char PROFILE_SPECIFIC_PREFIX = '%';

  public static final Comparator<ConfigSource> CONFIG_SOURCE_COMPARATOR = (o1, o2) -> {
    int res = Long.signum((long) o2.getOrdinal() - (long) o1.getOrdinal());
    return res != 0 ? res : o2.getName().compareTo(o1.getName());
  };

  protected final List<CorantConfigSource> sources;
  protected final String[] profiles;
  protected final String[] profilePrefixs;
  protected final ConfigELProcessor elProcessor;
  protected final boolean expressionsEnabled;

  /**
   * Build an instance
   *
   * @param sources the processed configuration resources.
   * @param expressionsEnabled whether to enable the el expression.
   * @param profiles the parsed profiles.
   */
  protected CorantConfigSources(List<CorantConfigSource> sources, boolean expressionsEnabled,
      String[] profiles) {
    this.sources = sources;
    this.profiles = defaultObject(profiles, Strings.EMPTY_ARRAY);
    this.expressionsEnabled = expressionsEnabled;
    if (isNotEmpty(profiles)) {
      String[] cps = new String[profiles.length];
      Arrays.setAll(cps, i -> PROFILE_SPECIFIC_PREFIX + profiles[i] + Names.NAME_SPACE_SEPARATOR);
      profilePrefixs = cps;
    } else {
      profilePrefixs = Strings.EMPTY_ARRAY;
    }
    elProcessor = new ConfigELProcessor(this::retrieveValue);
  }

  /**
   * Build an instance
   *
   * @param originalSources the original sources
   * @param classLoader the used class loader
   */
  public static CorantConfigSources of(List<ConfigSource> originalSources,
      ClassLoader classLoader) {
    shouldNotNull(originalSources, "The config sources can not null!");
    MutableObject<String[]> profiles = new MutableObject<>(Strings.EMPTY_ARRAY);
    MutableBoolean enableExpressions = MutableBoolean.of(true);
    List<Pair<String, ConfigSource>> profileSources = new ArrayList<>(originalSources.size());
    // collect the profile and source
    originalSources.stream().sorted(CONFIG_SOURCE_COMPARATOR.reversed()).forEachOrdered(cs -> {
      String sourceProfile = resolveSourceProfile(cs.getName());
      if (sourceProfile == null) {
        String propertyProfile =
            defaultString(cs.getValue(ConfigNames.CFG_PROFILE_KEY), cs.getValue(Config.PROFILE));
        if (propertyProfile != null) {
          profiles.set(CorantConfigResolver.splitValue(propertyProfile));
        }
      }
      profileSources.add(Pair.of(sourceProfile, cs));
      String expressionEnabled = cs.getValue(Config.PROPERTY_EXPRESSIONS_ENABLED);
      if (expressionEnabled != null) {
        enableExpressions.set(toBoolean(expressionEnabled));
      }
    });
    final ConfigAdjuster configAdjuster = ConfigAdjuster.resolve(classLoader);
    List<CorantConfigSource> sources = new ArrayList<>(originalSources.size());
    profileSources.forEach(ps -> {
      if (ps.getLeft() == null || Arrays.binarySearch(profiles.get(), ps.getLeft()) != -1) {
        ConfigSource adjustedSource = configAdjuster.apply(ps.getRight());
        if (adjustedSource != null) {
          sources.add(new CorantConfigSource(adjustedSource, ps.getLeft()));
        }
      }
    });
    // collect the profiled microprofile-config-* sources if necessary, maybe we can use GLOB
    // pattern to collect them in Config Builder
    if (isNotEmpty(profiles.get())) {
      for (String profile : profiles.get()) {
        for (ConfigSource mps : MicroprofileConfigSources.get(classLoader, profile)) {
          if (mps != null) {
            ConfigSource adjustedSource = configAdjuster.apply(mps);
            if (adjustedSource != null) {
              sources.add(new CorantConfigSource(adjustedSource, profile));
            }
          }
        }
      }
    }
    // sorting the collected sources
    sources.sort(CONFIG_SOURCE_COMPARATOR);
    return new CorantConfigSources(sources, enableExpressions.get(), profiles.get());
  }

  static String resolveSourceProfile(String sourceName) {
    String name;
    if (isNotEmpty(name = FileUtils.getFileBaseName(sourceName))) {
      int start;
      if ((start = name.indexOf(CORANT_CONFIG_SOURCE_BASE_NAME_PREFIX)) == 0) {
        name = name.substring(start + CORANT_CONFIG_SOURCE_BASE_NAME_PREFIX.length());
      } else if ((start = name.indexOf(MP_CONFIG_SOURCE_BASE_NAME_PREFIX)) == 0) {
        name = name.substring(start + MP_CONFIG_SOURCE_BASE_NAME_PREFIX.length());
      } else {
        name = null;
      }
    }
    return isEmpty(name) ? null : strip(name);
  }

  public ConfigValue getConfigValue(String propertyName, String defaultValue) {
    Pair<ConfigSource, String> val = getSourceAndValue(propertyName);
    if (!val.isEmpty()) {
      return new CorantConfigValue(propertyName, val.getValue(),
          defaultString(resolveValue(val.getValue()), defaultValue), val.getKey().getName(),
          val.getKey().getOrdinal());
    }
    return new CorantConfigValue(propertyName, null, defaultValue, null, 0);
  }

  public String[] getProfiles() {
    return Arrays.copyOf(profiles, profiles.length);
  }

  public Iterable<String> getPropertyNames() {
    return sources.stream().flatMap(cs -> cs.getProperties().keySet().stream())
        .map(this::normalizeName).filter(Objects::isNotNull).collect(Collectors.toSet());
  }

  public List<CorantConfigSource> getSources() {
    return sources;
  }

  /**
   * Returns the profiled and expanded value
   *
   * @param propertyName the config property name
   * @return getValue
   */
  public String getValue(String propertyName) {
    return resolveValue(retrieveValue(propertyName));
  }

  /**
   *
   * @return the expressionsEnabled
   */
  public boolean isExpressionsEnabled() {
    return expressionsEnabled;
  }

  /**
   * Returns the processed value of the EL expression
   */
  protected String evaluateValue(String value) {
    return elProcessor.evalValue(value);
  }

  /**
   * Find and return the property value in the processed configuration resources according to the
   * given property name.
   * <p>
   * Note: The search order complies with the microprofile sorting rule. First search the highest
   * priority config source system.profile etc., and then search the profiled sources according to
   * the profile names order if not found, and then search the profiled source items with the
   * profile prefix key if not found, finally search source items diectly.
   *
   * @param propertyName the property name to find
   * @return config source and value
   */
  protected Pair<ConfigSource, String> getSourceAndValue(String propertyName) {
    int i = profilePrefixs.length;
    while (--i >= 0) {
      String value;
      String key = profilePrefixs[i] + propertyName;
      for (CorantConfigSource cs : sources) {
        if (areEqual(cs.getSourceProfile(), profiles[i])) {
          value = cs.getValue(propertyName);
        } else {
          value = defaultString(cs.getValue(key), cs.getValue(propertyName));
        }
        if (value != null) {
          return Pair.of(cs, value);
        }
      }
    }
    for (CorantConfigSource cs : sources) {
      String value = cs.getValue(propertyName);
      if (value != null) {
        return Pair.of(cs, value);
      }
    }
    return Pair.empty();
  }

  protected String normalizeName(final String name) {
    int i = profilePrefixs.length;
    while (--i >= 0) {
      if (profilePrefixs[i] != null && name.startsWith(profilePrefixs[i])) {
        return name.substring(profilePrefixs[i].length());
      }
    }
    return name;
  }

  /**
   * Returns the expanded value
   */
  protected String resolveValue(String value) {
    if (expressionsEnabled) {
      return CorantConfigResolver.resolveValue(value,
          (e, k) -> e ? evaluateValue(k) : retrieveValue(k));
    }
    return value;
  }

  /**
   * Return the profiled value if necessary
   */
  protected String retrieveValue(String propertyName) {
    return getSourceAndValue(propertyName).getValue();
  }
}
