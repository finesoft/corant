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
package org.corant.modules.ddd.shared.unitwork;

import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.corant.modules.ddd.UnitOfWorksManager;
import org.corant.modules.ddd.annotation.InfrastructureServices;
import org.corant.modules.jpa.shared.PersistenceService;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-ddd-shared
 *
 * <p>
 * Abstract unit of works manager implementation, using JPA as a persistent implementation. This
 * implementation can provide the {@link UnitOfWorksListener}/pre-post
 * {@link UnitOfWorksHandler}/JPA {@link PersistenceService} involved in the use of the unit of
 * work.
 * </p>
 *
 *
 * @author bingo 上午11:39:15
 *
 */
@ApplicationScoped
@InfrastructureServices
public abstract class AbstractJPAUnitOfWorksManager implements UnitOfWorksManager {

  protected final transient Logger logger = Logger.getLogger(this.getClass().toString());

  @Inject
  protected Instance<UnitOfWorksListener> listeners;

  @Inject
  protected Instance<UnitOfWorksHandler> handlers;

  @Inject
  protected PersistenceService persistenceService;

  @Override
  public abstract AbstractJPAUnitOfWork getCurrentUnitOfWork();

  @Override
  public EntityManager getEntityManager(PersistenceContext pc) {
    return persistenceService.getEntityManager(pc);
  }

  @Override
  public Stream<UnitOfWorksHandler> getHandlers() {
    if (!handlers.isUnsatisfied()) {
      return handlers.stream().sorted(Sortable::compare);
    }
    return Stream.empty();
  }

  @Override
  public Stream<UnitOfWorksListener> getListeners() {
    if (!listeners.isUnsatisfied()) {
      return listeners.stream();
    }
    return Stream.empty();
  }

}
