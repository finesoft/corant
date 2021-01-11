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
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.WithAnnotations;
import org.corant.config.CorantConfigProviderResolver;
import org.corant.config.declarative.DeclarativeConfigKey;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

/**
 * corant-config
 *
 * @author bingo 下午4:34:28
 *
 */
public class ConfigExtension20 implements Extension {

  private static final Logger logger = Logger.getLogger(ConfigExtension20.class.getName());

  private final Set<InjectionPoint> configPropertyInjectionPoints = new HashSet<>();
  private final Set<InjectionPoint> configPropertiesInjectionPoints = new HashSet<>();
  private final Set<AnnotatedType<?>> configPropertiesTypes = new HashSet<>();
  private final Set<InjectionPoint> declarativeConfigInjectionPoints = new HashSet<>();

  public void onAfterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager bm) {
    configPropertyInjectionPoints.stream()
        .map(ip -> ip.getType() instanceof Class<?> ? wrap((Class<?>) ip.getType()) : ip.getType())
        .collect(Collectors.toSet())
        .forEach(type -> abd.addBean(new ConfigPropertyBean<>(bm, setOf(type))));
    declarativeConfigInjectionPoints.stream().map(ip -> (Class<?>) ip.getType())
        .collect(Collectors.toSet())
        .forEach(type -> abd.addBean(new DeclarativeConfigBean<>(bm, type)));
    configPropertiesTypes.stream().map(t -> new ConfigPropertiesBean<>(bm, t))
        .forEach(abd::addBean);
  }

  void onBeforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
    AnnotatedType<ConfigProducer> configBean = bm.createAnnotatedType(ConfigProducer.class);
    bbd.addAnnotatedType(configBean, ConfigProducer.class.getName());
  }

  synchronized void onBeforeShutdown(@Observes @Priority(0) BeforeShutdown bs) {
    ConfigProviderResolver cfr = ConfigProviderResolver.instance();
    if (cfr instanceof CorantConfigProviderResolver) {
      ((CorantConfigProviderResolver) cfr).clear(); // FIXME Is it necessary?
    }
    configPropertyInjectionPoints.clear();
    configPropertiesInjectionPoints.clear();
    declarativeConfigInjectionPoints.clear();
    configPropertiesTypes.clear();
  }

  void onProcessAnnotatedType(
      @Observes @WithAnnotations({ConfigProperties.class}) ProcessAnnotatedType<?> pat) {
    if (pat.getAnnotatedType().isAnnotationPresent(ConfigProperties.class)) {
      pat.veto();
      configPropertiesTypes.add(pat.getAnnotatedType());
    }
  }

  void onProcessInjectionPoint(@Observes ProcessInjectionPoint<?, ?> pip) {
    if (pip.getInjectionPoint().getAnnotated().isAnnotationPresent(ConfigProperty.class)) {
      logger.finer(
          () -> String.format("Find config property inject point %s.", pip.getInjectionPoint()));
      configPropertyInjectionPoints.add(pip.getInjectionPoint());
    }
    if (pip.getInjectionPoint().getAnnotated().isAnnotationPresent(ConfigProperties.class)) {
      logger.finer(
          () -> String.format("Find config properties inject point %s.", pip.getInjectionPoint()));
      configPropertiesInjectionPoints.add(pip.getInjectionPoint());
    }
    if (pip.getInjectionPoint().getAnnotated().isAnnotationPresent(DeclarativeConfigKey.class)) {
      logger.finer(() -> String.format("Find declarative config key inject point %s.",
          pip.getInjectionPoint()));
      declarativeConfigInjectionPoints.add(pip.getInjectionPoint());
    }
  }

  void validate(@Observes AfterDeploymentValidation adv, BeanManager bm) {
    // TODO FIXME validate config
  }
}
