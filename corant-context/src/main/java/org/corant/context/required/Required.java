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
package org.corant.context.required;

import static org.corant.shared.util.Classes.tryAsClass;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Objects.compare;
import static org.corant.shared.util.Objects.isNotNull;
import static org.corant.shared.util.Objects.isNull;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.enterprise.inject.spi.AnnotatedType;
import org.corant.context.required.RequiredConfiguration.ValuePredicate;
import org.corant.shared.util.Strings.WildcardMatcher;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * corant-context
 *
 * @author bingo 下午9:24:34
 *
 */
public class Required {

  public static boolean shouldVeto(AnnotatedType<?> type) {
    return shouldVeto(type.getAnnotations(RequiredClassPresent.class),
        type.getAnnotations(RequiredClassNotPresent.class),
        type.getAnnotations(RequiredConfiguration.class));
  }

  public static boolean shouldVeto(Set<RequiredClassPresent> requiredClassNames,
      Set<RequiredClassNotPresent> requiredNotClassNames,
      Set<RequiredConfiguration> requireConfigs) {
    boolean veto = false;
    if (isNotEmpty(requiredClassNames)) {
      veto = requiredClassNames.stream().flatMap(r -> streamOf(r.value()))
          .anyMatch(r -> isNull(tryAsClass(r)));
    }

    if (!veto && isNotEmpty(requiredNotClassNames)) {
      veto = requiredNotClassNames.stream().flatMap(r -> streamOf(r.value()))
          .anyMatch(r -> isNotNull(tryAsClass(r)));
    }

    if (!veto && isNotEmpty(requireConfigs)) {
      veto = requireConfigs.stream().allMatch(c -> {
        String key = c.key();
        if (isBlank(key)) {
          return false;
        }
        Set<String> keys = new LinkedHashSet<>();
        if (key.indexOf('*') != -1 || key.indexOf('?') != -1) {
          WildcardMatcher matcher = WildcardMatcher.of(false, key);
          for (String keyName : ConfigProvider.getConfig().getPropertyNames()) {
            if (matcher.test(keyName)) {
              keys.add(keyName);
            }
          }
        } else {
          keys.add(key);
        }
        return shouldVeto(keys, c.predicate(), c.type(), c.value());
      });
    }
    return veto;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  static boolean shouldVeto(Set<String> keys, ValuePredicate requiredValuePredicate,
      Class<?> requiredValueType, Object requiredValue) {
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
      for (String k : keys) {
        Object configValue = ConfigProvider.getConfig().getValue(k, requiredValueType);
        Object value = toObject(requiredValue, requiredValueType);
        boolean match = false;
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
