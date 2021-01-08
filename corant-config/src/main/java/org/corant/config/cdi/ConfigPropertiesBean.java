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
package org.corant.config.cdi;

import static org.corant.shared.util.Strings.defaultString;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import org.corant.config.declarative.ConfigInstances;
import org.corant.shared.util.Strings;
import org.eclipse.microprofile.config.inject.ConfigProperties;

public class ConfigPropertiesBean<T> implements Bean<T> {
  static final Set<Annotation> qualifiers = new HashSet<>();
  final BeanManager beanManager;
  final Class<T> clazz;
  final String prefix;

  public ConfigPropertiesBean(BeanManager bm, final AnnotatedType<T> type) {
    this.beanManager = bm;
    this.clazz = type.getJavaClass();
    this.prefix = defaultString(extractPrefix(type), Strings.EMPTY);
    ConfigPropertiesBean.qualifiers.add(Default.Literal.INSTANCE);
    if (type.isAnnotationPresent(ConfigProperties.class)) {
      ConfigPropertiesBean.qualifiers.add(ConfigProperties.Literal.of(prefix));
    }
  }

  @Override
  public T create(CreationalContext<T> context) {
    InjectionPoint ip =
        (InjectionPoint) beanManager.getInjectableReference(new CurrentInjectionPoint(), context);
    String thePrefix = extractPrefix(ip.getAnnotated());
    if (thePrefix == null) {
      thePrefix = ip.getQualifiers().stream().filter(ConfigProperties.class::isInstance)
          .map(ConfigProperties.class::cast).map(ConfigProperties::prefix)
          .filter(prefix -> !prefix.equals(ConfigProperties.UNCONFIGURED_PREFIX)).findFirst()
          .orElse(prefix);
    }
    return ConfigInstances.resolveMicroprofile(clazz, thePrefix);
  }

  @Override
  public void destroy(T instance, CreationalContext<T> context) {
    // Noop!
  }

  @Override
  public Class<T> getBeanClass() {
    return clazz;
  }

  @Override
  public Set<InjectionPoint> getInjectionPoints() {
    return Collections.emptySet();
  }

  @Override
  public String getName() {
    return "ConfigPropertiesBean_" + clazz.getName();
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
    return Collections.singleton(clazz);
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
    return "ConfigPropertiesBean [clazz=" + clazz + "]";
  }

  String extractPrefix(Annotated annotated) {
    Class<?> cls = (Class<?>) annotated.getBaseType();
    ConfigProperties cp = cls.getAnnotation(ConfigProperties.class);
    if (cp != null) {
      return cp.prefix().equals(ConfigProperties.UNCONFIGURED_PREFIX) ? null : cp.prefix();
    }
    return null;
  }

}
