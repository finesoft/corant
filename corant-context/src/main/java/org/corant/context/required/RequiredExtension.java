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
import static org.corant.shared.util.Assertions.shouldBeFalse;
import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Sets.newConcurrentHashSet;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.inject.Singleton;
import org.corant.shared.normal.Priorities;

/**
 * corant-context
 *
 * @author bingo 下午5:08:08
 *
 */
public class RequiredExtension implements Extension {

  static final Logger logger = Logger.getLogger(RequiredExtension.class.getName());

  private static final Set<Class<?>> vetoes = newConcurrentHashSet();
  private static final Set<Bean<?>> startups = newConcurrentHashSet();

  private static volatile boolean afterBeanDiscovery = false;

  public static boolean addVeto(Class<?> beanType) {
    shouldBeFalse(afterBeanDiscovery,
        "Unable to add the veto bean [%s], the bean processing phase has passed!", beanType);
    return beanType != null && vetoes.add(getUserClass(beanType));
  }

  public static boolean cancelVeto(Class<?> beanType) {
    shouldBeFalse(afterBeanDiscovery,
        "Unable to cancel the veto bean [%s], the bean processing phase has passed!", beanType);
    return beanType != null && vetoes.remove(getUserClass(beanType));
  }

  public static boolean isVetoed(Class<?> beanType) {
    return beanType != null && vetoes.contains(getUserClass(beanType));
  }

  // @WithAnnotations({ RequiredClassNotPresent.class, RequiredClassPresent.class,
  // RequiredConfiguration.class})
  protected void onProcessAnnotatedType(
      @Observes @Priority(Priorities.FRAMEWORK_LOWER) ProcessAnnotatedType<?> event) {
    AnnotatedType<?> type = event.getAnnotatedType();
    if (isVetoed(type.getJavaClass()) || RequiredExt.INSTANCE.shouldVeto(type)) {
      vetoes.add(type.getJavaClass());
      event.veto();
      logger.info(() -> String.format("The bean type %s was ignored!",
          event.getAnnotatedType().getJavaClass().getName()));
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

  void afterBeanDiscovery(@Observes @Priority(Priorities.FRAMEWORK_HIGHER) AfterBeanDiscovery e) {
    afterBeanDiscovery = true;
  }

  void afterDeploymentValidation(
      @Observes @Priority(Priorities.FRAMEWORK_HIGHER) AfterDeploymentValidation e,
      BeanManager manager) {
    for (Bean<?> bean : startups) {
      if (!isVetoed(bean.getBeanClass())) {
        select(bean.getBeanClass()).stream().forEach(Object::toString); // FIXME toString()??
      }
    }
  }
}
