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
package org.corant.config.spi;

import static org.corant.shared.util.ObjectUtils.isEquals;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * corant-config
 *
 * @author bingo 下午12:15:56
 *
 */
public class ConfigVariableAdjuster implements ConfigAdjuster {

  @Override
  public Map<String, String> apply(Map<String, String> properties,
      Map<String, String> allProperties) {
    Map<String, String> adjustered = new HashMap<>(properties);
    properties.forEach((k, v) -> {
      if (hasVariable(v) && isEquals(v, allProperties.get(k))) {
        String av = resolveVariables(v, allProperties::get);
        adjustered.put(k, av);
      }
    });
    return adjustered;
  }

  boolean hasVariable(String v) {
    return v != null && v.indexOf("${") != -1 && v.indexOf("}") != -1;
  }

  String resolveVariables(String value, Function<String, String> vals) {
    int startVar = 0;
    while ((startVar = value.indexOf("${", startVar)) >= 0) {
      int endVar = value.indexOf("}", startVar);
      if (endVar <= 0) {
        break;
      }
      String varName = value.substring(startVar + 2, endVar);
      if (varName.isEmpty()) {
        break;
      }
      String variableValue = vals.apply(varName);
      if (variableValue != null) {
        value = resolveVariables(value.replace("${" + varName + "}", variableValue), vals);
      }
      startVar++;
    }
    return value;
  }
}
