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

import static org.corant.context.Beans.select;
import static org.corant.shared.util.Sets.newConcurrentHashSet;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessBean;
import jakarta.inject.Singleton;
import jakarta.annotation.Priority;
import org.corant.shared.normal.Priorities;
import org.corant.shared.util.Services;

/**
 * corant-context
 *
 * @author bingo 上午11:59:35
 *
 */
public class StartupExtension implements Extension {

  private static final Set<Bean<?>> startups = newConcurrentHashSet();

  protected void afterDeploymentValidation(
      @Observes @Priority(Priorities.FRAMEWORK_HIGHER) AfterDeploymentValidation e,
      BeanManager manager) {
    for (Bean<?> bean : startups) {
      if (!Services.shouldVeto(bean.getBeanClass())) {
        select(bean.getBeanClass()).stream().forEach(Object::toString); // FIXME toString()??
      }
    }
  }

  protected void onProcessBean(
      @Observes @Priority(Priorities.FRAMEWORK_LOWER) ProcessBean<?> event) {
    if ((event.getBean().getScope().equals(ApplicationScoped.class)
        || event.getBean().getScope().equals(Singleton.class))
        && event.getAnnotated().isAnnotationPresent(Startup.class)) {
      startups.add(event.getBean());
    }
  }
}
