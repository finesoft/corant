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

import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Strings.EMPTY;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.util.AnnotationLiteral;
import org.corant.shared.util.Objects;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class ConfigPropertyBean<T> implements Bean<T>, PassivationCapable {
  static final Set<Annotation> qualifiers = Collections.singleton(new ConfigPropertyLiteral());
  final BeanManager beanManager;
  final Set<Type> types;

  public ConfigPropertyBean(BeanManager bm, Set<Type> types) {
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
    return ConfigPropertyBean.class;
  }

  @Override
  public String getId() {
    return "ConfigPropertyBean_" + String.join("_", Objects.asStrings(types));
  }

  @Override
  public Set<InjectionPoint> getInjectionPoints() {
    return Collections.emptySet();
  }

  @Override
  public String getName() {
    return "ConfigPropertyBean_" + String.join("_", Objects.asStrings(types));
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
    return "ConfigPropertyBean [types=" + types + "]";
  }

  private static class ConfigPropertyLiteral extends AnnotationLiteral<ConfigProperty>
      implements ConfigProperty {
    private static final long serialVersionUID = -4241417907420530257L;

    @Override
    public String defaultValue() {
      return EMPTY;
    }

    @Override
    public String name() {
      return EMPTY;
    }
  }
}
