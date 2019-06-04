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
package org.corant.suites.ddd.model;

import static javax.interceptor.Interceptor.Priority.APPLICATION;
import static org.corant.shared.util.ObjectUtils.forceCast;
import java.lang.annotation.Annotation;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.suites.ddd.event.LifecycleManageEvent;
import org.corant.suites.ddd.unitwork.JPAPersistenceService;
import org.corant.suites.ddd.unitwork.JTAJPAUnitOfWorksManager;

/**
 * corant-asosat-ddd
 *
 * @author bingo 上午10:44:19
 *
 */
@ApplicationScoped
@InfrastructureServices
public class DefaultAggregateLifecycleManager implements AggregateLifecycleManager {

  final Logger logger = Logger.getLogger(this.getClass().getName());

  @Inject
  JTAJPAUnitOfWorksManager unitOfWorksManager;

  @Inject
  JPAPersistenceService persistenceService;

  @Override
  @Transactional
  public void on(@Observes(
      during = TransactionPhase.IN_PROGRESS) @Priority(APPLICATION + 1000) LifecycleManageEvent e) {
    if (e.getSource() != null) {
      Entity entity = forceCast(e.getSource());
      Annotation named = persistenceService.getPersistenceUnitQualifier(entity.getClass());
      boolean effectImmediately = e.isEffectImmediately();
      handle(entity, e.isDestroy(), effectImmediately, named);
      logger.fine(() -> String.format("Handle %s %s", entity.getClass().getName(),
          e.isDestroy() ? "destroy" : "persist"));
    }
  }

  protected void handle(Entity entity, boolean destroy, boolean effectImmediately,
      Annotation qualifier) {
    EntityManager em = unitOfWorksManager.getCurrentUnitOfWork().getEntityManager(qualifier);
    if (destroy) {
      em.remove(entity);
      if (effectImmediately) {
        em.flush();
      }
    } else {
      if (entity.getId() == null) {
        em.persist(entity);
        if (effectImmediately) {
          em.flush();
        }
      } else {
        em.merge(entity);
        if (effectImmediately) {
          em.flush();
        }
      }
    }
  }
}
