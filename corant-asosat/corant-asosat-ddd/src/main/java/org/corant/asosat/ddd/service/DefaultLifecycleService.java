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
package org.corant.asosat.ddd.service;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import org.corant.suites.ddd.annotation.qualifier.JPA;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.suites.ddd.event.LifecycleEvent;
import org.corant.suites.ddd.model.Aggregate.LifcyclePhase;
import org.corant.suites.ddd.model.Entity;
import org.corant.suites.ddd.repository.JpaRepository;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午2:17:04
 *
 */
@JPA
@ApplicationScoped
@InfrastructureServices
public class DefaultLifecycleService implements LifecycleService {

  @Inject
  @JPA
  protected JpaRepository repo;

  public void merge(Object obj, boolean immediately) {
    repo.merge(obj);
    if (immediately) {
      repo.getEntityManager().flush();
    }
  }

  @Override
  public void on(@Observes(during = TransactionPhase.IN_PROGRESS) LifecycleEvent event) {
    if (event.getSource() != null) {
      Entity entity = event.getSource();
      if (event.getPhase() == LifcyclePhase.ENABLE) {
        if (entity.getId() == null) {
          persist(entity, event.isEffectImmediately());
        } else {
          merge(entity, event.isEffectImmediately());
        }
      } else if (event.getPhase() == LifcyclePhase.DESTROY && entity.getId() != null) {
        remove(entity, event.isEffectImmediately());
      }
    }
  }

  public void persist(Object obj, boolean immediately) {
    repo.persist(obj);
    if (immediately) {
      repo.getEntityManager().flush();
    }
  }

  public void remove(Object obj, boolean immediately) {
    repo.remove(obj);
    if (immediately) {
      repo.getEntityManager().flush();
    }
  }

}
