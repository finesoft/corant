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
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Objects.isNotNull;
import static org.corant.shared.util.Objects.isNull;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import java.util.Set;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import org.corant.config.Configs;
import org.corant.context.required.RequiredConfiguration.ValuePredicate;

/**
 * corant-kernel
 *
 * @author bingo 下午5:08:08
 *
 */
public class RequiredExtension implements Extension {

  public <T> void checkRequired(
      @WithAnnotations({RequiredClassNotPresent.class, RequiredClassPresent.class,
          RequiredConfiguration.class}) @Observes ProcessAnnotatedType<T> event) {
    AnnotatedType<?> type = event.getAnnotatedType();
    if (checkVeto(type.getAnnotations(RequiredClassPresent.class),
        type.getAnnotations(RequiredClassNotPresent.class),
        type.getAnnotations(RequiredConfiguration.class))) {
      event.veto();
    }
  }

  boolean checkVeto(Set<RequiredClassPresent> requiredClassNames,
      Set<RequiredClassNotPresent> requiredNotClassNames,
      Set<RequiredConfiguration> requireConfigs) {
    boolean veto = false;
    if (isNotEmpty(requiredClassNames)) {
      veto = requiredClassNames.stream().flatMap(r -> streamOf(r.value()))
          .anyMatch(r -> isNull(tryAsClass(r)));
    }

    if (isNotEmpty(requiredNotClassNames) && !veto) {
      veto = requiredNotClassNames.stream().flatMap(r -> streamOf(r.value()))
          .anyMatch(r -> isNotNull(tryAsClass(r)));
    }

    if (isNotEmpty(requireConfigs) && !veto) {
      veto = requireConfigs.stream().allMatch(c -> {
        String key = c.key();
        if (isBlank(key)) {
          return true;
        }
        if (c.predicate() == ValuePredicate.NO_BLANK) {
          return isNotBlank(Configs.getValue(key, String.class));
        } else if (c.predicate() == ValuePredicate.NO_NULL) {
          return isNotNull(Configs.getValue(key, String.class));
        } else {
          return areEqual(c.value(), Configs.getValue(key, String.class));
        }
      });
    }
    return veto;
  }
}
