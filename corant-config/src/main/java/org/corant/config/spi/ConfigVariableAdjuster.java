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

import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Strings.isNotBlank;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.corant.shared.exception.CorantRuntimeException;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * corant-config
 *
 * @author bingo 下午12:15:56
 *
 */
public class ConfigVariableAdjuster implements ConfigAdjuster {

  @Override
  public Map<String, String> apply(final Map<String, String> properties,
      final Collection<ConfigSource> originalSources) {
    Map<String, String> adjustered = new HashMap<>(properties);
    final Set<String> stack = new LinkedHashSet<>();
    properties.forEach((k, v) -> {
      if (hasVariable(v) && areEqual(v, resolveValue(k, originalSources))) {
        String av = resolveVariables(k, v, originalSources, stack);
        stack.clear();
        adjustered.put(k, av);
      }
    });
    return adjustered;
  }

  boolean hasVariable(String v) {
    return v != null && v.indexOf("${") != -1 && v.indexOf('}') != -1;
  }

  String resolveValue(final String propertyName, final Collection<ConfigSource> originalSources) {
    for (ConfigSource cs : originalSources) {
      String value = cs.getValue(propertyName);
      if (isNotBlank(value)) {
        return value;
      }
    }
    return null;
  }

  String resolveVariables(String key, String value, final Collection<ConfigSource> originalSources,
      final Set<String> stack) {
    int startVar = 0;
    String resolvedValue = value;
    while ((startVar = resolvedValue.indexOf("${", startVar)) >= 0) {
      int endVar = resolvedValue.indexOf('}', startVar);
      if (endVar <= 0) {
        break;
      }
      String varName = resolvedValue.substring(startVar + 2, endVar);
      if (varName.isEmpty()) {
        break;
      } else if (varName.equals(key)) {
        throw new CorantRuntimeException(
            "A recursive error occurred in the configuration entry [%s].",
            String.join(" -> ", stack));
      } else {
        stack.add(varName);
      }
      String varVal = resolveValue(varName, originalSources);
      if (varVal != null) {
        resolvedValue = resolveVariables(key, resolvedValue.replace("${" + varName + "}", varVal),
            originalSources, stack);
      }
      startVar++;
    }
    return resolvedValue;
  }
}
