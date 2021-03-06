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

import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
   * @param value the configuration property key or the original value
   * @return the assembled values or the original given values if it can't expand
   */
  public static List<String> assemblyStringConfigProperties(String value) {
    String useKey = assemblyStringConfigProperty(value);
    if (isNotBlank(useKey)) {
      List<String> list = new ArrayList<>();
      for (String proVal : CorantConfigResolver.splitValue(useKey)) {
        list.add(proVal);
      }
      return list;
    }
    return listOf(value);
  }

  /**
   * According to the input value, and analyze whether the value contains the configuration property
   * name variable, for example:'${property.name}'. If the property name variable exists, replace
   * the name variable with the relevant configuration property value, and then return the assembled
   * value, If there is no property name variable, it doesn't change the passed value and directly
   * return it. This is use for enhance some annotated configuration flexibility.
   *
   * @param value the configuration property key or the original value
   * @return the assembled value or the original given value if it can't expand
   */
  public static String assemblyStringConfigProperty(String value) {
    CorantConfigSources cs = ((CorantConfig) ConfigProvider.getConfig()).getCorantConfigSources();
    if (cs.isExpressionsEnabled()) {
      return cs.resolveValue(value);
    } else {
      return resolveVariable(value);
    }
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
    return getValue(propertyName, propertyType, null);
  }

  /**
   * Get the config property, if the property doesn't exist return nvl.
   *
   * @param <T>
   * @param propertyName
   * @param propertyType
   * @param nvl
   * @return getValue
   */
  public static <T> T getValue(String propertyName, Class<T> propertyType, T nvl) {
    Optional<T> op = ConfigProvider.getConfig().getOptionalValue(propertyName, propertyType);
    return op.orElse(nvl);
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
