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

import org.eclipse.microprofile.config.ConfigValue;

/**
 * corant-config
 *
 * @author bingo 下午5:40:21
 *
 */
public class CorantConfigValue implements ConfigValue {

  private final String name;
  private final String rawValue;
  private final String value;
  private final String sourceName;
  private final int sourceOrdinal;

  /**
   * @param name the name of the property.
   * @param rawValue the value of the property lookup without any transformation
   * @param value the value of the property lookup with transformations
   * @param sourceName the ConfigSource name
   * @param sourceOrdinal the ConfigSource ordinal
   */
  public CorantConfigValue(String name, String rawValue, String value, String sourceName,
      int sourceOrdinal) {
    this.name = name;
    this.rawValue = rawValue;
    this.value = value;
    this.sourceName = sourceName;
    this.sourceOrdinal = sourceOrdinal;
  }

  /**
   * The name of the property.
   *
   * @return the name of the property.
   */
  @Override
  public String getName() {
    // TODO MP 2.0
    return name;
  }

  /**
   * The value of the property lookup without any transformation (expanded , etc).
   *
   * @return the raw value of the property lookup or {@code null} if the property could not be
   *         found.
   */
  @Override
  public String getRawValue() {
    // TODO MP 2.0
    return rawValue;
  }

  /**
   * The {@link org.eclipse.microprofile.config.spi.ConfigSource} name that loaded the property
   * lookup.
   *
   * @return the ConfigSource name that loaded the property lookup or {@code null} if the property
   *         could not be found
   */
  @Override
  public String getSourceName() {
    // TODO MP 2.0
    return sourceName;
  }

  /**
   * The {@link org.eclipse.microprofile.config.spi.ConfigSource} ordinal that loaded the property
   * lookup.
   *
   * @return the ConfigSource ordinal that loaded the property lookup or {@code 0} if the property
   *         could not be found
   */
  @Override
  public int getSourceOrdinal() {
    // TODO MP 2.0
    return sourceOrdinal;
  }

  /**
   * The value of the property lookup with transformations (expanded, etc).
   *
   * @return the value of the property lookup or {@code null} if the property could not be found
   */
  @Override
  public String getValue() {
    // TODO MP 2.0
    return value;
  }
}
