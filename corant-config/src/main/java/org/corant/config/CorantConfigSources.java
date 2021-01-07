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

import static org.corant.config.CorantConfig.COARNT_CONFIG_SOURCE_BASE_NAME_PREFIX;
import static org.corant.config.CorantConfig.MP_CONFIG_SOURCE_BASE_NAME_PREFIX;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Conversions.toBoolean;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Strings.defaultString;
import static org.corant.shared.util.Strings.trim;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.corant.config.expression.ConfigELProcessor;
import org.corant.shared.normal.Names;
import org.corant.shared.normal.Names.ConfigNames;
import org.corant.shared.ubiquity.Mutable.MutableBoolean;
import org.corant.shared.ubiquity.Mutable.MutableString;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.FileUtils;
import org.corant.shared.util.Objects;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * corant-config
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
  protected final String profile;
  protected final String profilePrefix;
  protected final ConfigELProcessor elProcessor;
  protected final boolean expressionsEnabled;

  /**
   * @param sources
   * @param expressionsEnabled
   * @param profile
   */
  protected CorantConfigSources(List<CorantConfigSource> sources, boolean expressionsEnabled,
      String profile) {
    super();
    this.sources = sources;
    this.profile = profile;
    this.expressionsEnabled = expressionsEnabled;
    if (profile != null) {
      profilePrefix = PROFILE_SPECIFIC_PREFIX + profile + Names.NAME_SPACE_SEPARATOR;
    } else {
      profilePrefix = null;
    }
    elProcessor = new ConfigELProcessor(this::retrieveValue);
  }

  /**
   *
   * @param orginals
   * @param classLoader
   * @return of
   */
  public static CorantConfigSources of(List<ConfigSource> orginals, ClassLoader classLoader) {
    shouldNotNull(orginals, "The config sources can not null!");
    MutableString profile = MutableString.of(null);
    MutableBoolean enableExpressions = MutableBoolean.of(true);
    List<Pair<String, ConfigSource>> profileSources = new ArrayList<>(orginals.size());
    orginals.stream().sorted(CONFIG_SOURCE_COMPARATOR.reversed()).forEachOrdered(cs -> {
      String sourceProfile = resolveSourceProfile(cs.getName());
      if (sourceProfile == null) {
        String propertyProfile =
            defaultString(cs.getValue(ConfigNames.CFG_PROFILE_KEY), cs.getValue(Config.PROFILE));
        if (propertyProfile != null) {
          profile.set(propertyProfile);
        }
      }
      profileSources.add(Pair.of(sourceProfile, cs));
      String expressionEnabled = cs.getValue(Config.PROPERTY_EXPRESSIONS_ENABLED);
      if (expressionEnabled != null) {
        enableExpressions.set(toBoolean(expressionEnabled));
      }
    });

    List<CorantConfigSource> sources = new ArrayList<>(orginals.size());
    profileSources.forEach(ps -> {
      if (ps.getLeft() == null || areEqual(ps.getLeft(), profile.get())) {
        sources.add(new CorantConfigSource(ps.getRight(), ps.getLeft()));
      }
    });
    sources.sort(CONFIG_SOURCE_COMPARATOR);
    return new CorantConfigSources(sources, enableExpressions.get(), profile.get());
  }

  static String resolveSourceProfile(String sourceName) {
    String name = null;
    if (isNotEmpty(name = FileUtils.getFileBaseName(sourceName))) {
      int start = -1;
      if ((start = name.indexOf(COARNT_CONFIG_SOURCE_BASE_NAME_PREFIX)) > -1) {
        name = name.substring(start + COARNT_CONFIG_SOURCE_BASE_NAME_PREFIX.length());
      } else if ((start = name.indexOf(MP_CONFIG_SOURCE_BASE_NAME_PREFIX)) > -1) {
        name = name.substring(start + MP_CONFIG_SOURCE_BASE_NAME_PREFIX.length());
      } else {
        name = null;
      }
    }
    return isEmpty(name) ? null : trim(name);
  }

  public ConfigValue getConfigValue(String propertyName) {
    Pair<ConfigSource, String> val = getSourceAndValue(propertyName);
    if (val != null) {
      return new CorantConfigValue(propertyName, val.getValue(), resolveValue(val.getValue()),
          val.getKey().getName(), val.getKey().getOrdinal());
    }
    return new CorantConfigValue(propertyName, null, null, null, 0);
  }

  public String getProfile() {
    return profile;
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
   * @param propertyName
   * @return getValue
   */
  public String getValue(String propertyName) {
    return resolveValue(retrieveValue(propertyName));
  }

  /**
   * Returns the processed value of the EL expression
   *
   * @param value
   * @return evaluateValue
   */
  protected String evaluateValue(String value) {
    return elProcessor.evalValue(value);
  }

  /**
   * Return the profiled value with source if necessary
   *
   * @param propertyName
   * @return config source and value
   */
  protected Pair<ConfigSource, String> getSourceAndValue(String propertyName) {
    if (profilePrefix != null) {
      String value = null;
      String key = profilePrefix + propertyName;
      for (CorantConfigSource cs : sources) {
        if (areEqual(cs.getSourceProfile(), profile)) {
          value = cs.getValue(propertyName);
        } else {
          value = defaultString(cs.getValue(key), cs.getValue(propertyName));
        }
        if (value != null) {
          return Pair.of(cs, value);
        }
      }
    } else {
      for (CorantConfigSource cs : sources) {
        String value = cs.getValue(propertyName);
        if (value != null) {
          return Pair.of(cs, value);
        }
      }
    }
    return Pair.empty();
  }

  protected String normalizeName(final String name) {
    if (profilePrefix != null && name.startsWith(profilePrefix)) {
      return name.substring(profilePrefix.length());
    }
    // else if (name.startsWith("%")) {
    // return null;
    // }
    return name;
  }

  /**
   * Returns the expanded value
   *
   * @param value
   * @return resolveValue
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
   *
   * @param propertyName
   * @return retrieveValue
   */
  protected String retrieveValue(String propertyName) {
    return getSourceAndValue(propertyName).getValue();
  }
}
