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
package org.corant.config.cdi;

import static org.corant.shared.util.Primitives.wrap;
import static org.corant.shared.util.Sets.setOf;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.BeforeShutdown;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.ProcessInjectionPoint;
import org.corant.config.CorantConfigProviderResolver;
import org.corant.shared.normal.Priorities;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

/**
 * corant-config
 *
 * @deprecated Since microprifle-config 2.0
 *
 * @author bingo 下午4:34:28
 */
@Deprecated
public class ConfigExtension implements Extension {

  private static final Logger logger = Logger.getLogger(ConfigExtension.class.getName());

  private final Set<InjectionPoint> injectionPoints = new HashSet<>();

  public void onAfterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager bm) {
    Set<Type> types = injectionPoints.stream().map(ip -> {
      if (ip.getType() instanceof Class<?>) {
        return wrap((Class<?>) ip.getType());
      } else {
        return ip.getType();
      }
    }).collect(Collectors.toSet());
    types.forEach(type -> abd.addBean(new ConfigPropertyBean<>(bm, setOf(type))));
  }

  void onBeforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
    AnnotatedType<ConfigProducer> configBean = bm.createAnnotatedType(ConfigProducer.class);
    bbd.addAnnotatedType(configBean, ConfigProducer.class.getName());
  }

  synchronized void onBeforeShutdown(
      @Observes @Priority(Priorities.FRAMEWORK_LOWER) BeforeShutdown bs) {
    ConfigProviderResolver cfr = ConfigProviderResolver.instance();
    if (cfr instanceof CorantConfigProviderResolver) {
      ((CorantConfigProviderResolver) cfr).clear(); // FIXME Is it necessary?
    }
    injectionPoints.clear();
  }

  void onProcessInjectionPoint(@Observes ProcessInjectionPoint<?, ?> pip) {
    if (pip.getInjectionPoint().getAnnotated().isAnnotationPresent(ConfigProperty.class)) {
      logger.fine(
          () -> String.format("Found config property inject point %s.", pip.getInjectionPoint()));
      injectionPoints.add(pip.getInjectionPoint());
    }
  }

  void validate(@Observes AfterDeploymentValidation adv, BeanManager bm) {
    // TODO FIXME validate config
  }

}
