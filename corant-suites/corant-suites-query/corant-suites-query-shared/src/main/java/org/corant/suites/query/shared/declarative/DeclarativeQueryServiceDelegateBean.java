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
package org.corant.suites.query.shared.declarative;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.ClassUtils.getUserClass;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.BeanManager;
import org.corant.suites.cdi.AbstractBean;
import org.corant.suites.cdi.AutoCreated;

/**
 * corant-suites-query-shared
 *
 * Unfinish yet
 *
 * @author bingo 下午2:03:58
 *
 */
public class DeclarativeQueryServiceDelegateBean extends AbstractBean<Object> {

  final Class<?> proxyType;

  /**
   * @param beanManager
   */
  public DeclarativeQueryServiceDelegateBean(BeanManager beanManager, Class<?> proxyType) {
    super(beanManager);
    this.proxyType = shouldNotNull(getUserClass(proxyType));
    qualifiers.add(AutoCreated.INST);
    qualifiers.add(Default.Literal.INSTANCE);
    qualifiers.add(Any.Literal.INSTANCE);
    stereotypes.add(DeclarativeQueryService.class);
    types.add(proxyType);
  }

  @Override
  public Object create(CreationalContext<Object> creationalContext) {
    return null;// TODO
  }

  @Override
  public void destroy(Object instance, CreationalContext<Object> creationalContext) {

  }

}
