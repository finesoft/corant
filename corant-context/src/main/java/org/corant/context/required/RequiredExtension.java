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

import static java.lang.String.format;
import static org.corant.context.Beans.select;
import static org.corant.shared.util.Assertions.shouldBeFalse;
import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.trim;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.logging.Logger;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessBeanAttributes;
import org.corant.shared.normal.Priorities;
import org.corant.shared.service.RequiredClassNotPresent;
import org.corant.shared.service.RequiredClassPresent;
import org.corant.shared.service.RequiredConfiguration;
import org.corant.shared.util.Classes;
import org.corant.shared.util.Services;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * corant-context
 *
 * @author bingo 下午5:08:08
 */
public class RequiredExtension implements Extension {

  public static final String VETO_BEANS_CFG_NAME = "corant.context.veto.beans";

  protected static final Logger logger = Logger.getLogger(RequiredExtension.class.getName());

  protected static volatile boolean afterBeanDiscovery = false;

  public static boolean addVeto(Class<?> beanType) {
    shouldBeFalse(afterBeanDiscovery,
        "Unable to add the veto bean [%s], the bean processing phase has passed!", beanType);
    return beanType != null && Services.getRequired().addVeto(getUserClass(beanType));
  }

  public static boolean removeVeto(Class<?> beanType) {
    shouldBeFalse(afterBeanDiscovery,
        "Unable to cancel the veto bean [%s], the bean processing phase has passed!", beanType);
    return beanType != null && Services.getRequired().removeVeto(getUserClass(beanType));
  }

  protected void beforeBeanDiscovery(@Observes final BeforeBeanDiscovery bbd,
      final BeanManager beanManager) {
    ConfigProvider.getConfig().getOptionalValues(VETO_BEANS_CFG_NAME, String.class)
        .ifPresent(beans -> {
          for (String bean : beans) {
            if (isNotBlank(bean)) {
              Class<?> beanClass = Classes.tryAsClass(trim(bean));
              if (beanClass != null) {
                addVeto(beanClass);
              }
            }
          }
        });
  }

  void afterBeanDiscovery(@Observes @Priority(Priorities.FRAMEWORK_HIGHER) AfterBeanDiscovery e) {
    afterBeanDiscovery = true;
  }

  void afterDeploymentValidation(
      @Observes @Priority(Priorities.FRAMEWORK_HIGHER) AfterDeploymentValidation e,
      BeanManager bm) {
    Instance<RequiredValidator> validators = select(RequiredValidator.class);
    if (!validators.isUnsatisfied()) {
      validators.forEach(v -> v.validate(e, bm));
    }
  }

  void onProcessBeanAttributes(
      @Observes @Priority(Priorities.FRAMEWORK_LOWER) ProcessBeanAttributes<?> event) {
    // Veto bean which was discovered if necessary
    Annotated annotated = event.getAnnotated();
    Type baseType = annotated.getBaseType();
    boolean veto = (baseType instanceof Class<?> baseClass)
        && Services.getRequired().shouldVeto(getUserClass(baseClass));
    if (!veto && (annotated.isAnnotationPresent(RequiredClassNotPresent.class)
        || annotated.isAnnotationPresent(RequiredClassPresent.class)
        || annotated.isAnnotationPresent(RequiredConfiguration.class))) {
      Set<RequiredClassNotPresent> requiredClassNotPresents =
          annotated.getAnnotations(RequiredClassNotPresent.class);
      Set<RequiredClassPresent> requiredClassPresents =
          annotated.getAnnotations(RequiredClassPresent.class);
      Set<RequiredConfiguration> requiredConfigurations =
          annotated.getAnnotations(RequiredConfiguration.class);
      if (Services.getRequired().shouldVeto(this.getClass().getClassLoader(),
          requiredClassPresents.toArray(RequiredClassPresent[]::new),
          requiredClassNotPresents.toArray(RequiredClassNotPresent[]::new),
          requiredConfigurations.toArray(RequiredConfiguration[]::new))) {
        veto = true;
      }
    }
    if (veto) {
      event.veto();
      logger.info(() -> format("Veto a bean [%s] which don't meet the requirements",
          baseType.getTypeName()));
    }
  }

}
