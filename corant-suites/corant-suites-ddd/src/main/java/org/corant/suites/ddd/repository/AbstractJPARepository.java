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
package org.corant.suites.ddd.repository;

import static org.corant.shared.util.AnnotationUtils.findAnnotation;
import static org.corant.shared.util.ClassUtils.getUserClass;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import javax.annotation.PostConstruct;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.corant.kernel.api.PersistenceService.PersistenceContextLiteral;
import org.corant.suites.ddd.unitwork.JTAJPAUnitOfWorksManager;

/**
 * corant-suites-ddd
 *
 * @author bingo 下午9:54:26
 *
 */
public abstract class AbstractJPARepository implements JPARepository {

  @Inject
  protected JTAJPAUnitOfWorksManager unitOfWorkManager;

  protected volatile PersistenceContext persistenceContext;

  /**
   * One transaction one entity manager
   */
  @Override
  public EntityManager getEntityManager() {
    // FIXME TODO CHECK THE REPO NAMES AND EMF NAMES
    return unitOfWorkManager.getCurrentUnitOfWork().getEntityManager(persistenceContext);
  }

  @PostConstruct
  void onPostConstruct() {
    if (persistenceContext == null) {
      synchronized (this) {
        if (persistenceContext == null) {
          Named name = defaultObject(findAnnotation(getUserClass(this.getClass()), Named.class),
              NamedLiteral.INSTANCE);
          persistenceContext = PersistenceContextLiteral.of(name.value());
        }
      }
    }
  }
}
