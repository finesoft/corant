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

import static org.corant.shared.util.Assertions.shouldBeFalse;
import static org.corant.shared.util.Classes.getUserClass;
import java.util.logging.Logger;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.WithAnnotations;
import org.corant.shared.normal.Priorities;
import org.corant.shared.service.RequiredClassNotPresent;
import org.corant.shared.service.RequiredClassPresent;
import org.corant.shared.service.RequiredConfiguration;
import org.corant.shared.util.Services;

/**
 * corant-context
 *
 * @author bingo 下午5:08:08
 *
 */
public class RequiredExtension implements Extension {

  static final Logger logger = Logger.getLogger(RequiredExtension.class.getName());

  private static volatile boolean afterBeanDiscovery = false;

  public static boolean addVeto(Class<?> beanType) {
    shouldBeFalse(afterBeanDiscovery,
        "Unable to add the veto bean [%s], the bean processing phase has passed!", beanType);
    return beanType != null && Services.getRequired().addVeto(getUserClass(beanType));
  }

  public static boolean cancelVeto(Class<?> beanType) {
    shouldBeFalse(afterBeanDiscovery,
        "Unable to cancel the veto bean [%s], the bean processing phase has passed!", beanType);
    return beanType != null && Services.getRequired().removeVeto(getUserClass(beanType));
  }

  public static boolean isVetoed(Class<?> beanType) {
    return beanType != null && Services.getRequired().shouldVeto(getUserClass(beanType));
  }

  protected void onProcessAnnotatedType(
      @Observes @Priority(Priorities.FRAMEWORK_LOWER) @WithAnnotations({
          RequiredClassNotPresent.class, RequiredClassPresent.class,
          RequiredConfiguration.class}) ProcessAnnotatedType<?> event) {
    AnnotatedType<?> type = event.getAnnotatedType();
    if (isVetoed(type.getJavaClass())) {
      event.veto();
      logger.info(() -> String.format("The bean type %s was ignored!",
          event.getAnnotatedType().getJavaClass().getName()));
    }
  }

  void afterBeanDiscovery(@Observes @Priority(Priorities.FRAMEWORK_HIGHER) AfterBeanDiscovery e) {
    afterBeanDiscovery = true;
  }

}
