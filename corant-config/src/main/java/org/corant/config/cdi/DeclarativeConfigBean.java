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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import org.corant.config.declarative.DeclarativeConfigKey;
import org.corant.config.declarative.DeclarativeConfigKey.DeclarativeConfigKeyLiteral;

public class DeclarativeConfigBean<T> implements Bean<T> {
  static final Set<Annotation> qualifiers =
      Collections.singleton(DeclarativeConfigKeyLiteral.UNCONFIGURED);
  final BeanManager beanManager;
  final Class<T> clazz;
  final Set<Type> types;

  public DeclarativeConfigBean(BeanManager bm, final Class<T> type) {
    this.beanManager = bm;
    this.clazz = type;
    this.types = Collections.singleton(clazz);
  }

  @Override
  public T create(CreationalContext<T> context) {
    InjectionPoint ip =
        (InjectionPoint) beanManager.getInjectableReference(new CurrentInjectionPoint(), context);
    return ConfigProducer.declarative(ip);
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
    return "DeclarativeConfigBean_" + clazz.getName();
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
    return "DeclarativeConfigBean [clazz=" + clazz + "]";
  }

  String extractKey(Class<?> annotated) {
    if (annotated != null) {
      DeclarativeConfigKey cp = annotated.getAnnotation(DeclarativeConfigKey.class);
      if (cp != null) {
        return DeclarativeConfigKey.UNCONFIGURED_KEY.equals(cp.value()) ? null : cp.value();
      }
    }
    return null;
  }

}
