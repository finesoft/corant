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

import static org.corant.shared.util.Sets.newConcurrentHashSet;
import java.util.Set;
import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;

/**
 * corant-context
 *
 * @author bingo 下午5:08:08
 *
 */
public class RequiredExtension implements Extension {

  private static final Set<Class<?>> vetoes = newConcurrentHashSet();

  public static boolean isVetoed(Class<?> beanType) {
    return beanType != null && vetoes.contains(beanType);
  }

  public <T> void checkRequired(
      @WithAnnotations({RequiredClassNotPresent.class, RequiredClassPresent.class,
          RequiredConfiguration.class}) @Observes @Priority(99999) ProcessAnnotatedType<T> event) {
    AnnotatedType<?> type = event.getAnnotatedType();
    if (Required.shouldVeto(type)) {
      event.veto();
    }
  }
}
