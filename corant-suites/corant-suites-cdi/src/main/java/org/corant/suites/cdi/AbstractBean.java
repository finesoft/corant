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
package org.corant.suites.cdi;

import static org.corant.shared.util.Classes.getUserClass;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;

/**
 * corant-suites-cdi
 *
 * @author bingo 下午3:45:22
 *
 */
public abstract class AbstractBean<T> implements Bean<T>, PassivationCapable {

  protected final Set<Annotation> qualifiers = new HashSet<>();
  protected final Set<Type> types = new HashSet<>();
  protected final Set<Class<? extends Annotation>> stereotypes = new HashSet<>();
  protected final Set<InjectionPoint> injectionPoints = new HashSet<>();
  protected final BeanManager beanManager;
  protected Class<? extends Annotation> scope = ApplicationScoped.class;

  /**
   * @param beanManager
   */
  public AbstractBean(BeanManager beanManager) {
    super();
    this.beanManager = beanManager;
  }

  @Override
  public T create(CreationalContext<T> creationalContext) {
    return null;
  }

  @Override
  public void destroy(T instance, CreationalContext<T> creationalContext) {

  }

  @Override
  public Class<?> getBeanClass() {
    return getUserClass(getClass());
  }

  public BeanManager getBeanManager() {
    return beanManager;
  }

  @Override
  public String getId() {
    return getBeanClass().getCanonicalName();
  }

  @Override
  public Set<InjectionPoint> getInjectionPoints() {
    return Collections.unmodifiableSet(injectionPoints);
  }

  @Override
  public String getName() {
    return getId();
  }

  @Override
  public Set<Annotation> getQualifiers() {
    return Collections.unmodifiableSet(qualifiers);
  }

  @Override
  public Class<? extends Annotation> getScope() {
    return scope;
  }

  @Override
  public Set<Class<? extends Annotation>> getStereotypes() {
    return Collections.unmodifiableSet(stereotypes);
  }

  @Override
  public Set<Type> getTypes() {
    return Collections.unmodifiableSet(types);
  }

  @Override
  public boolean isAlternative() {
    return false;
  }

  @Override
  public boolean isNullable() {
    return false;
  }

}
