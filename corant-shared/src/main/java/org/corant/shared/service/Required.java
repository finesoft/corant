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
package org.corant.shared.service;

import static org.corant.shared.util.Classes.tryAsClass;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Objects.compare;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.isNotNull;
import static org.corant.shared.util.Objects.isNull;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.matchAnyRegex;
import static org.corant.shared.util.Strings.split;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.corant.shared.conversion.converter.factory.StringObjectConverterFactory;
import org.corant.shared.service.RequiredConfiguration.ValuePredicate;
import org.corant.shared.util.Strings.WildcardMatcher;
import org.corant.shared.util.Systems;

/**
 * corant-shared
 *
 * @author bingo 下午9:24:34
 *
 */
public class Required {

  public static final Required INSTANCE = new Required();

  protected Required() {}

  public boolean shouldVeto(Class<?> type) {
    return shouldVeto(type.getClassLoader(), type.getAnnotationsByType(RequiredClassPresent.class),
        type.getAnnotationsByType(RequiredClassNotPresent.class),
        type.getAnnotationsByType(RequiredConfiguration.class));
  }

  public boolean shouldVeto(ClassLoader classLoader, RequiredClassPresent[] requiredClassNames,
      RequiredClassNotPresent[] requiredNotClassNames, RequiredConfiguration[] requireConfigs) {
    boolean veto = false;
    if (isNotEmpty(requiredClassNames)) {
      veto = Arrays.stream(requiredClassNames).flatMap(r -> streamOf(r.value()))
          .anyMatch(r -> isNull(tryAsClass(r, classLoader)));
    }

    if (!veto && isNotEmpty(requiredNotClassNames)) {
      veto = Arrays.stream(requiredNotClassNames).flatMap(r -> streamOf(r.value()))
          .anyMatch(r -> isNotNull(tryAsClass(r, classLoader)));
    }

    if (!veto && isNotEmpty(requireConfigs)) {
      veto = Arrays.stream(requireConfigs).allMatch(c -> {
        String key = c.key();
        String value =
            RequiredConfiguration.DEFAULT_NULL_VALUE.equals(c.value()) ? null : c.value();
        if (isBlank(key)) {
          return false;
        }
        Set<String> keys = new LinkedHashSet<>();
        if (key.indexOf('*') != -1 || key.indexOf('?') != -1) {
          WildcardMatcher matcher = WildcardMatcher.of(false, key);
          for (String keyName : Systems.getPropertyNames()) {
            if (matcher.test(keyName)) {
              keys.add(keyName);
            }
          }
        } else {
          keys.add(key);
        }
        return shouldVeto(keys, c.predicate(), c.type(), value);
      });
    }
    return veto;
  }

  protected Object getConfigValue(String key, Class<?> valueType, Object dfltNullValue) {
    return defaultObject(getConvertValue(
        defaultObject(Systems.getProperty(key), () -> Systems.getEnvironmentVariable(key)),
        valueType), dfltNullValue);
  }

  protected Object getConvertValue(String value, Class<?> valueType) {
    if (Collection.class.isAssignableFrom(valueType)) {
      return listOf(split(value, ",", true, true));
    }
    return StringObjectConverterFactory.convert(value, valueType);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  protected boolean shouldVeto(Set<String> keys, ValuePredicate requiredValuePredicate,
      Class<?> requiredValueType, String requiredValue) {
    if (keys.size() == 0) {
      switch (requiredValuePredicate) {
        case BLANK:
        case NULL:
        case EMPTY:
          return false;
        default:
          return true;
      }
    } else {
      final Object defaultNullValue =
          requiredValueType.equals(Boolean.class) || requiredValueType.equals(Boolean.TYPE)
              ? Boolean.FALSE
              : null;
      for (String k : keys) {
        Object configValue = getConfigValue(k, requiredValueType, defaultNullValue);
        Object value = getConvertValue(requiredValue, requiredValueType);
        boolean match;
        switch (requiredValuePredicate) {
          case BLANK:
            match = isNotBlank((String) configValue);
            break;
          case NULL:
            match = isNotNull(configValue);
            break;
          case EMPTY:
            match = isNotEmpty(configValue);
            break;
          case NO_EMPTY:
            match = isEmpty(configValue);
            break;
          case NO_BLANK:
            match = isBlank((String) configValue);
            break;
          case NO_NULL:
            match = isNull(configValue);
            break;
          case GTE:
            match = compare((Comparable) configValue, (Comparable) value) < 0;
            break;
          case GT:
            match = compare((Comparable) configValue, (Comparable) value) <= 0;
            break;
          case LT:
            match = compare((Comparable) configValue, (Comparable) value) > 0;
            break;
          case LTE:
            match = compare((Comparable) configValue, (Comparable) value) >= 0;
            break;
          case NO_EQ:
            match = areEqual(value, configValue);
            break;
          case INC:
            if (value instanceof Collection && configValue instanceof Collection) {
              match = !((Collection) configValue).containsAll((Collection) value);
            } else if (value instanceof String && configValue instanceof String) {
              match = !((String) configValue).contains((String) value);
            } else {
              match = true;
            }
            break;
          case EXC:
            if (value instanceof Collection && configValue instanceof Collection) {
              match = ((Collection) configValue).containsAll((Collection) value);
            } else if (value instanceof String && configValue instanceof String) {
              match = ((String) configValue).contains((String) value);
            } else {
              match = true;
            }
            break;
          case REGEX:
            match = configValue == null || value == null ? true
                : !matchAnyRegex(value.toString(), Pattern.CASE_INSENSITIVE,
                    configValue.toString());
            break;
          default:
            match = !areEqual(value, configValue);
            break;
        }
        if (match) {
          return true;
        }
      }
    }
    return false;
  }
}
