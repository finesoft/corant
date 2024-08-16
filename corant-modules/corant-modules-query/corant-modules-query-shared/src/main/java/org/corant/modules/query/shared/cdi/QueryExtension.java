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
package org.corant.modules.query.shared.cdi;

import static java.lang.String.format;
import static org.corant.context.Beans.resolve;
import static org.corant.shared.util.Configurations.getConfigValue;
import static org.corant.shared.util.Empties.isEmpty;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeShutdown;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.WithAnnotations;
import org.corant.context.qualifier.AutoCreated;
import org.corant.modules.query.shared.QueryMappingService;
import org.corant.modules.query.shared.declarative.DeclarativeQueryService;
import org.corant.modules.query.shared.declarative.DeclarativeQueryServiceDelegateBean;
import org.corant.shared.normal.Priorities;
import org.corant.shared.util.Services;

/**
 * corant-modules-query-shared
 * <p>
 * Unfinish yet
 *
 * @author bingo 下午2:39:57
 */
public class QueryExtension implements Extension {

  public static boolean verifyDeployment =
      getConfigValue("corant.query.verify-deployment", Boolean.TYPE, false);

  protected final Logger logger = Logger.getLogger(this.getClass().getName());

  Set<Class<?>> declarativeQueryServiceClasses = new LinkedHashSet<>();

  protected void onBeforeShutdown(
      @Observes @Priority(Priorities.FRAMEWORK_LOWER) BeforeShutdown bs) {
    declarativeQueryServiceClasses.clear();
  }

  void findDeclarativeQueryServices(
      @Observes @WithAnnotations({DeclarativeQueryService.class}) ProcessAnnotatedType<?> pat) {
    if (Services.shouldVeto(pat.getAnnotatedType().getJavaClass())) {
      return;
    }
    Class<?> klass = pat.getAnnotatedType().getJavaClass();
    if (!klass.isInterface()) {
      logger.warning(() -> format(
          "Found %s with annotation @DeclarativeQueryService, but it not an interface.", klass));
      return;
    }
    if (isEmpty(klass.getDeclaredMethods())) {
      logger.warning(() -> format(
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
    if (verifyDeployment) {
      resolve(QueryMappingService.class).getQueries();
      for (Class<?> cls : declarativeQueryServiceClasses) {
        String inst = resolve(cls, AutoCreated.INST).toString();
        logger.info(() -> format("Resolve declarative query service %s, instance: %s", cls, inst));
      }
    }
  }
}
