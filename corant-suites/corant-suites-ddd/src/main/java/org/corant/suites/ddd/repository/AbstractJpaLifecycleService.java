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

import java.lang.annotation.Annotation;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.suites.ddd.event.LifecycleEvent;
import org.corant.suites.ddd.model.Aggregate.LifecyclePhase;
import org.corant.suites.ddd.model.Entity;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午2:17:04
 *
 */
@ApplicationScoped
@InfrastructureServices
public abstract class AbstractJpaLifecycleService implements LifecycleService {

  protected final transient Logger logger = Logger.getLogger(this.getClass().toString());

  @Inject
  @Any
  Instance<JpaRepository> repos;

  @Override
  @Transactional
  public void on(@Observes(during = TransactionPhase.IN_PROGRESS) LifecycleEvent e) {
    if (e.getSource() != null) {
      Entity entity = e.getSource();
      Named named = resolvePersistenceUnitName(entity.getClass());
      LifecyclePhase phase = e.getPhase();
      boolean effectImmediately = e.isEffectImmediately();
      handle(entity, phase, effectImmediately, named);
      logger.fine(() -> String.format("Listen %s %s", entity.getClass().getName(), phase.name()));
    }
  }

  protected void handle(Entity entity, LifecyclePhase lifcyclePhase, boolean effectImmediately,
      Annotation repoQf) {
    JpaRepository repo = repos.select(repoQf).get();
    if (lifcyclePhase == LifecyclePhase.ENABLE) {
      if (entity.getId() == null) {
        repo.persist(entity);
        if (effectImmediately) {
          repo.getEntityManager().flush();
        }
      } else {
        repo.merge(entity);
        if (effectImmediately) {
          repo.getEntityManager().flush();
        }
      }
    } else if (lifcyclePhase == LifecyclePhase.DESTROY && entity.getId() != null) {
      repo.remove(entity);
      if (effectImmediately) {
        repo.getEntityManager().flush();
      }
    }
  }

}
