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
package org.corant.suites.jpa.shared.cdi;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.StringUtils.defaultBlank;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.logging.Logger;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import org.corant.suites.cdi.AbstractBean;
import org.corant.suites.jpa.shared.PersistenceService;

/**
 * corant-suites-jpa-shared
 *
 * @author bingo 上午10:34:41
 *
 */
public class EntityManagerFactoryBean extends AbstractBean<EntityManagerFactory> {

  final Logger logger = Logger.getLogger(getClass().getName());

  final PersistenceUnit pu;

  /**
   * @param beanManager
   * @param pu
   */
  public EntityManagerFactoryBean(BeanManager beanManager, PersistenceUnit pu,
      Annotation[] qualifiers) {
    super(beanManager);
    this.pu = shouldNotNull(pu);
    Collections.addAll(this.qualifiers, qualifiers);
    types.add(EntityManagerFactory.class);
  }

  @Override
  public EntityManagerFactory create(CreationalContext<EntityManagerFactory> creationalContext) {
    return CDI.current().select(PersistenceService.class).get().getEntityManagerFactory(pu);
  }

  @Override
  public void destroy(EntityManagerFactory instance,
      CreationalContext<EntityManagerFactory> creationalContext) {
    if (instance != null && instance.isOpen()) {
      instance.close();
      logger.fine(
          () -> String.format("Destroyed entity manager factory that persistence unit named %s.",
              defaultBlank(pu.unitName(), "unnamed")));
    }
  }

  @Override
  public String getId() {
    return EntityManagerFactoryBean.class.getName() + "." + pu.unitName();
  }

  @Override
  public String getName() {
    return "EntityManagerFactoryBean." + pu.unitName();
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
