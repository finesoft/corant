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
package org.corant.modules.jndi;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Strings.FALSE;
import static org.corant.shared.util.Strings.TRUE;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.CreationException;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * corant-modules-jndi
 *
 * @author bingo 下午1:40:11
 *
 */
public class JNDIBean<T> implements Bean<T>, PassivationCapable {

  private final Set<Annotation> qualifiers;

  private final String name;

  private final Class<? extends T> type;

  private final Class<? extends Annotation> scope;

  public JNDIBean(final String name, final Class<? extends T> type,
      final Class<? extends Annotation> scope, Annotation... qualifiers) {
    this.name = shouldNotNull(name);
    this.type = shouldNotNull(type);
    this.scope = scope == null ? Dependent.class : scope;
    this.qualifiers = new HashSet<>();
    this.qualifiers.add(Any.Literal.INSTANCE);
    this.qualifiers.add(Default.Literal.INSTANCE);
    Collections.addAll(this.qualifiers, qualifiers);
  }

  @Override
  public final T create(final CreationalContext<T> cc) {
    Context initialContext = null;
    CreationException e = null;
    try {
      initialContext = this.getNewInitialContext();
      return this.type.cast(initialContext.lookup(this.name));
    } catch (final NamingException namingException) {
      e = new CreationException(namingException.getMessage(), namingException);
      throw e;
    } finally {
      if (initialContext != null) {
        try {
          initialContext.close();
        } catch (final NamingException namingException) {
          if (e != null) {
            e.addSuppressed(namingException);
          } else {
            e = new CreationException(namingException.getMessage(), namingException);
          }
          throw e;
        }
      }
    }
  }

  @Override
  public void destroy(final T instance, final CreationalContext<T> cc) {
    if (cc != null) {
      cc.release();
    }
  }

  @Override
  public Class<?> getBeanClass() {
    return this.getClass();
  }

  @Override
  public String getId() {
    return String.join(";", getBeanClass().getName() + "%" + getName(), getScope().getName(),
        isAlternative() ? TRUE : FALSE,
        String.join(",",
            getQualifiers().stream().map(q -> q.annotationType().getSimpleName())
                .toArray(String[]::new)),
        String.join(",",
            getStereotypes().stream().map(Class::getSimpleName).toArray(String[]::new)),
        String.join(",", getTypes().stream().map(Type::getTypeName).toArray(String[]::new)));
  }

  @Override
  public Set<InjectionPoint> getInjectionPoints() {
    return Collections.emptySet();
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public Set<Annotation> getQualifiers() {
    return this.qualifiers;
  }

  @Override
  public final Class<? extends Annotation> getScope() {
    return this.scope;
  }

  @Override
  public Set<Class<? extends Annotation>> getStereotypes() {
    return Collections.emptySet();
  }

  @Override
  public final Set<Type> getTypes() {
    return Collections.singleton(this.type);
  }

  @Override
  public boolean isAlternative() {
    return false;
  }

  @Override
  public final boolean isNullable() {
    return false;
  }

  protected Context getNewInitialContext() throws NamingException {
    return new InitialContext();
  }
}
