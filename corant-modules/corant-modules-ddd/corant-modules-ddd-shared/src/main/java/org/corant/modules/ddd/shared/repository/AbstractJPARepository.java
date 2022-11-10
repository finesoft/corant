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
package org.corant.modules.ddd.shared.repository;

import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Objects.defaultObject;
import javax.annotation.PostConstruct;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.corant.modules.ddd.shared.unitwork.AbstractJPAUnitOfWorksManager;
import org.corant.modules.jpa.shared.PersistenceService.PersistenceContextLiteral;

/**
 * corant-modules-ddd-shared
 *
 * @author bingo 下午9:54:26
 *
 */
public abstract class AbstractJPARepository implements JPARepository {

  protected AbstractJPAUnitOfWorksManager unitOfWorkManager;

  protected volatile PersistenceContext persistenceContext;

  @Override
  public EntityManager getEntityManager() {
    // FIXME TODO CHECK THE REPO NAMES AND EMF NAMES
    return unitOfWorkManager.getCurrentUnitOfWork().getEntityManager(persistenceContext);
  }

  @PostConstruct
  protected void onPostConstruct() {
    if (persistenceContext == null) {
      synchronized (this) {
        if (persistenceContext == null) {
          PersistenceContext usePersistenceContext;
          Class<?> thisClass = getUserClass(this.getClass());
          if ((usePersistenceContext = thisClass.getAnnotation(PersistenceContext.class)) == null) {
            Named name = defaultObject(thisClass.getAnnotation(Named.class), NamedLiteral.INSTANCE);
            usePersistenceContext = PersistenceContextLiteral.of(name.value());
          }
          persistenceContext = usePersistenceContext;
        }
      }
    }
  }
}
