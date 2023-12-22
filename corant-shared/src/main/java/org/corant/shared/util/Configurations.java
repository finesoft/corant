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

import org.corant.shared.ubiquity.Configuration;
import org.corant.shared.ubiquity.TypeLiteral;

/**
 * corant-shared
 *
 * @author bingo 上午11:12:33
 */
public class Configurations {

  public static String getAssembledConfigValue(String value) {
    return getConfig().getAssembledValue(value);
  }

  public static Configuration getConfig() {
    return Configuration.INSTANCE;
  }

  public static <T> T getConfigValue(String name, Class<T> valueType) {
    return getConfig().getValue(name, valueType);
  }

  public static <T> T getConfigValue(String name, Class<T> valueType, T nvl) {
    return getConfig().getValue(name, valueType, nvl);
  }

  public static <T> T getConfigValue(String name, TypeLiteral<T> valueTypeLiteral) {
    return getConfig().getValue(name, valueTypeLiteral);
  }

  public static <T> T getConfigValue(String name, TypeLiteral<T> valueTypeLiteral, T nvl) {
    return getConfig().getValue(name, valueTypeLiteral, nvl);
  }
}
