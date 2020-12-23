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

import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Primitives.wrap;
import static org.corant.shared.util.Sets.setOf;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.util.AnnotationLiteral;
import org.corant.config.CorantConfigProviderResolver;
import org.corant.shared.util.Objects;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

/**
 * corant-config
 *
 * @author bingo 下午4:34:28
 *
 */
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
    types.forEach(type -> abd.addBean(new ConfigInjectionBean<>(bm, setOf(type))));
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
    injectionPoints.clear();
  }

  void onProcessInjectionPoint(@Observes ProcessInjectionPoint<?, ?> pip) {
    if (pip.getInjectionPoint().getAnnotated().isAnnotationPresent(ConfigProperty.class)) {
      logger.fine(
          () -> String.format("Find config property inject point %s", pip.getInjectionPoint()));
      injectionPoints.add(pip.getInjectionPoint());
    }
  }

  void validate(@Observes AfterDeploymentValidation adv, BeanManager bm) {
    // TODO FIXME validate config
  }

  public static class ConfigInjectionBean<T> implements Bean<T>, PassivationCapable {
    static final Set<Annotation> qualifiers = Collections.singleton(new ConfigPropertyLiteral());
    final BeanManager beanManager;
    final Set<Type> types;

    public ConfigInjectionBean(BeanManager bm, Set<Type> types) {
      this.beanManager = bm;
      this.types = Collections.unmodifiableSet(types);
    }

    @Override
    public T create(CreationalContext<T> context) {
      InjectionPoint ip =
          (InjectionPoint) beanManager.getInjectableReference(new CurrentInjectionPoint(), context);
      return forceCast(ConfigProducer.getConfigProperty(ip));
    }

    @Override
    public void destroy(T instance, CreationalContext<T> context) {
      // Noop!
    }

    @Override
    public Class<?> getBeanClass() {
      return ConfigInjectionBean.class;
    }

    @Override
    public String getId() {
      return "ConfigInjectionBean_" + String.join("_", Objects.asStrings(types));
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
      return Collections.emptySet();
    }

    @Override
    public String getName() {
      return "ConfigInjectionBean_" + String.join("_", Objects.asStrings(types));
    }

    @Override
    public Set<Annotation> getQualifiers() {
      return qualifiers;
    }

    @Override
    public Class<? extends Annotation> getScope() {
      return Dependent.class;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
      return Collections.emptySet();
    }

    @Override
    public Set<Type> getTypes() {
      return types;
    }

    @Override
    public boolean isAlternative() {
      return false;
    }

    @Override
    public boolean isNullable() {
      return false;
    }

    @Override
    public String toString() {
      return "ConfigInjectionBean [types=" + types + "]";
    }

    private static class ConfigPropertyLiteral extends AnnotationLiteral<ConfigProperty>
        implements ConfigProperty {
      private static final long serialVersionUID = -4241417907420530257L;

      @Override
      public String defaultValue() {
        return "";
      }

      @Override
      public String name() {
        return "";
      }
    }

    private static class CurrentInjectionPoint implements InjectionPoint {

      static final Set<Annotation> qualifiers = Collections.singleton(Default.Literal.INSTANCE);

      @Override
      public Annotated getAnnotated() {
        return null;
      }

      @Override
      public Bean<?> getBean() {
        return null;
      }

      @Override
      public Member getMember() {
        return null;
      }

      @Override
      public Set<Annotation> getQualifiers() {
        return qualifiers;
      }

      @Override
      public Type getType() {
        return InjectionPoint.class;
      }

      @Override
      public boolean isDelegate() {
        return false;
      }

      @Override
      public boolean isTransient() {
        return false;
      }

    }
  }
}
