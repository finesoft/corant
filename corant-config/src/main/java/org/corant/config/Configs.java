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

import static org.corant.shared.util.Sets.linkedHashSetOf;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * corant-config
 *
 * @author bingo 下午4:52:17
 *
 */
public class Configs {

  /**
   * According to the input value, analyze whether the value contains the configuration property
   * name variable, for example:'$ {property.name}'. If the property name variable exists, get the
   * relevant configuration property value, and then divide the property value into the array
   * according to the Microprofile config multi-value specification, use the array property values
   * replace the name variables of the input value separately and then return the assembled values.
   * If there is no attribute name variable, the passed value is not changed and it is returned
   * directly. This is used to enhance some annotated configuration flexibility.
   *
   * NOTE: This function only supports at most one variable
   *
   * @param key
   * @return assemblyStringConfigProperties
   */
  public static Set<String> assemblyStringConfigProperties(String key) {
    if (isNotBlank(key)) {
      int s;
      int e;
      if ((s = key.indexOf("${")) != -1 && (e = key.indexOf('}')) != -1 && e - s > 2) {
        String proKey = key.substring(s + 2, e);
        if (proKey.length() > 0) {
          Set<String> set = new LinkedHashSet<>();
          String proVals = getConfig().getOptionalValue(proKey, String.class).orElse(null);
          for (String proVal : ConfigUtils.splitValue(proVals)) {
            set.add(new StringBuilder(key.substring(0, s)).append(proVal)
                .append(key.substring(e + 1)).toString());
          }
          return set;
        }
      }
    }
    return linkedHashSetOf(key);
  }

  /**
   * According to the input value, and analyze whether the value contains the configuration
   * propertyname variable, for example:'${property.name}'. If the property name variable exists,
   * replace the name variable with the relevant configuration property value, and then return the
   * assembledvalue, If there is no property name variable, it doesn't change the passed value and
   * directlyreturn it. This is use for enhance some annotated configuration flexibility.
   *
   * @param value
   * @return assemblyStringConfigProperty
   */
  public static String assemblyStringConfigProperty(String value) {
    if (isNotBlank(value)) {
      return resolveVariable(value);
    }
    return value;
  }

  /**
   * Get the config property, if the property doesn't exist return null.
   *
   * @param <T>
   * @param propertyName
   * @param propertyType
   * @return getValue
   */
  public static <T> T getValue(String propertyName, Class<T> propertyType) {
    Optional<T> op = ConfigProvider.getConfig().getOptionalValue(propertyName, propertyType);
    return op.orElseGet(null);
  }

  public static String resolveVariable(String propertyName) {
    int startVar = 0;
    String resolvedValue = propertyName;
    while ((startVar = resolvedValue.indexOf("${", startVar)) >= 0) {
      int endVar = resolvedValue.indexOf('}', startVar);
      if (endVar <= 0) {
        break;
      }
      String varName = resolvedValue.substring(startVar + 2, endVar);
      if (varName.isEmpty()) {
        break;
      }
      Optional<String> varVal = getConfig().getOptionalValue(varName, String.class);
      if (varVal.isPresent()) {
        resolvedValue = resolveVariable(resolvedValue.replace("${" + varName + "}", varVal.get()));
      }
      startVar++;
    }
    return resolvedValue;
  }
}
