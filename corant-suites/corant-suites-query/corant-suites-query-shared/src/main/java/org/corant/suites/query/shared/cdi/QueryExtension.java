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
package org.corant.suites.query.shared.cdi;

import static org.corant.shared.util.Empties.isEmpty;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import org.corant.context.required.Required;
import org.corant.suites.query.shared.declarative.DeclarativeQueryService;
import org.corant.suites.query.shared.declarative.DeclarativeQueryServiceDelegateBean;

/**
 * corant-suites-query-shared
 *
 * Unfinish yet
 *
 * @author bingo 下午2:39:57
 *
 */
public class QueryExtension implements Extension {

  protected final Logger logger = Logger.getLogger(this.getClass().getName());

  Set<Class<?>> declarativeQueryServiceClasses = new LinkedHashSet<>();

  protected void onBeforeShutdown(@Observes @Priority(0) BeforeShutdown bs) {
    declarativeQueryServiceClasses.clear();
  }

  void findDeclarativeQueryServices(
      @Observes @WithAnnotations(DeclarativeQueryService.class) ProcessAnnotatedType<?> pat) {
    if (Required.shouldVeto(pat.getAnnotatedType())) {
      return;
    }
    Class<?> klass = pat.getAnnotatedType().getJavaClass();
    if (!klass.isInterface()) {
      logger.warning(() -> String.format(
          "Found %s with annotation @DeclarativeQueryService, but it not an interface.", klass));
      return;
    }
    if (isEmpty(klass.getDeclaredMethods())) {
      logger.warning(() -> String.format(
          "Found %s with annotation @DeclarativeQueryService, but it didn't declare any methods.",
          klass));
      return;
    }
    declarativeQueryServiceClasses.add(klass);
  }

  void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery event, BeanManager beanManager) {
    if (event != null) {
      for (Class<?> klass : declarativeQueryServiceClasses) {
        event.addBean(new DeclarativeQueryServiceDelegateBean(beanManager, klass));
      }
    }
  }

  void onAfterDeploymentValidation(@Observes final AfterDeploymentValidation event) {
    // TODO check whether the declarative query service fit the query define
  }
}
