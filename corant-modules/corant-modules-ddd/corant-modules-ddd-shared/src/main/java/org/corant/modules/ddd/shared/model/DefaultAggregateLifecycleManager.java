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
package org.corant.modules.ddd.shared.model;

import java.util.logging.Logger;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.corant.config.Configs;
import org.corant.modules.ddd.Aggregate;
import org.corant.modules.ddd.Aggregate.Lifecycle;
import org.corant.modules.ddd.AggregateLifecycleManageEvent;
import org.corant.modules.ddd.AggregateLifecycleManager;
import org.corant.modules.ddd.annotation.InfrastructureServices;
import org.corant.modules.ddd.shared.repository.EntityManagers;
import org.corant.shared.normal.Priorities;

/**
 * corant-modules-ddd-shared
 *
 * <p>
 * The default aggregate life cycle manager, use EDA mechanism.
 *
 * @author bingo 上午10:44:19
 *
 */
@ApplicationScoped
@InfrastructureServices
public class DefaultAggregateLifecycleManager implements AggregateLifecycleManager {

  protected static final boolean forceMerge = Configs.<Boolean>getValue(
      "corant.ddd.aggregate.lifecycle.force-merge", Boolean.class, Boolean.FALSE);

  protected final Logger logger = Logger.getLogger(this.getClass().getName());

  @Inject
  protected EntityManagers entityManagers;

  @Override
  public void handle(@Observes(
      during = TransactionPhase.IN_PROGRESS) @Priority(Priorities.FRAMEWORK_HIGHER) AggregateLifecycleManageEvent e) {
    if (e.getSource() != null) {
      Aggregate entity = e.getSource();
      handle(entity, e.getAction(), e.isEffectImmediately(), e.getLockModeType());
      logger.fine(() -> String.format("Handle %s %s %s.", entity.getClass().getName(),
          e.getAction().name(), entity.getId()));
    }
  }

  protected void handle(Aggregate entity, LifecycleAction action, boolean effectImmediately,
      LockModeType lockModeType) {
    EntityManager em = entityManagers.getEntityManager(entity.getClass());
    if (action == LifecycleAction.PERSIST) {
      if (entity.getLifecycle() == Lifecycle.INITIAL) {
        em.persist(entity);
        if (effectImmediately) {
          em.flush();
        }
      } else {
        if (!em.contains(entity) || forceMerge) {
          em.merge(entity); // performance turning
        }
        if (effectImmediately) {
          em.flush();
        }
      }
    } else if (action == LifecycleAction.REMOVE) {
      em.remove(entity);
      if (effectImmediately) {
        em.flush();
      }
    } else if (action == LifecycleAction.RECOVER) {
      em.refresh(entity);
    } else if (!entity.isPhantom()) {
      em.lock(entity, lockModeType);
    }
  }

}
