/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.suites.jpa.shared.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 * corant-suites-jpa-shared
 *
 * @author bingo 下午7:29:22
 *
 */
public class PersistenceContextInjectionPoint implements InjectionPoint {

  private final InjectionPoint orginal;
  private final Set<Annotation> qualifiers = new HashSet<>();

  /**
   * @param orginal
   */
  public PersistenceContextInjectionPoint(InjectionPoint orginal, Annotation... qualifiers) {
    super();
    this.orginal = orginal;
    for (Annotation qualifier : qualifiers) {
      this.qualifiers.add(qualifier);
    }
  }

  @Override
  public Annotated getAnnotated() {
    return orginal.getAnnotated();
  }

  @Override
  public Bean<?> getBean() {
    return orginal.getBean();
  }

  @Override
  public Member getMember() {
    return orginal.getMember();
  }

  @Override
  public Set<Annotation> getQualifiers() {
    return qualifiers;
  }

  @Override
  public Type getType() {
    return orginal.getType();
  }

  @Override
  public boolean isDelegate() {
    return orginal.isDelegate();
  }

  @Override
  public boolean isTransient() {
    return orginal.isTransient();
  }

}
