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

  public static final String REP = "${";
  public static final String EXP = "#{";
  public static final String END = "}";

  @Override
  public Map<String, String> apply(final Map<String, String> properties,
      final Collection<ConfigSource> originalSources) {
    final Map<String, String> adjusted = new HashMap<>(properties);
    final Set<String> stack = new LinkedHashSet<>();
    final ConfigVariableProcessor processor = new ConfigVariableProcessor(originalSources);
    properties.forEach((k, v) -> {
      String value = v;
      if (areEqual(value, processor.getValue(k))) {
        if (hasExpression(value)) {
          value = resolveVariables(true, k, value, processor, stack);
          stack.clear();
        }
        if (hasVariable(value)) {
          value = resolveVariables(false, k, value, processor, stack);
          stack.clear();
        }
      }
      adjusted.put(k, value);
    });
    return adjusted;
  }

  boolean hasExpression(String v) {
    return v != null && v.indexOf(EXP) != -1 && v.indexOf(END) != -1;
  }

  boolean hasVariable(String v) {
    return v != null && v.indexOf(REP) != -1 && v.indexOf(END) != -1;
  }

  String resolveVariables(boolean expression, String key, String value,
      final ConfigVariableProcessor processor, final Set<String> stack) {
    int startVar = 0;
    String begin = expression ? EXP : REP;
    String resolvedValue = value;
    while ((startVar = resolvedValue.indexOf(begin, startVar)) >= 0) {
      int endVar = resolvedValue.indexOf(END, startVar);
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
      String varVal = expression ? processor.evalValue(varName) : processor.getValue(varName);
      if (varVal != null) {
        resolvedValue = resolveVariables(expression, key,
            resolvedValue.replace(begin + varName + END, varVal), processor, stack);
      }
      startVar++;
    }
    return resolvedValue;
  }

}
