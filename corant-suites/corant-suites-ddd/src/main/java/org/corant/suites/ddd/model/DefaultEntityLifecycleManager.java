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
import static org.corant.shared.util.ObjectUtils.asString;
import static org.corant.shared.util.ObjectUtils.forceCast;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import org.corant.kernel.normal.Names.PersistenceNames;
import org.corant.kernel.service.PersistenceService;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.suites.ddd.event.LifecycleManageEvent;
import org.corant.suites.ddd.unitwork.JTAJPAUnitOfWorksManager;

/**
 * corant-asosat-ddd
 *
 * @author bingo 上午10:44:19
 *
 */
@ApplicationScoped
@InfrastructureServices
public class DefaultEntityLifecycleManager implements EntityLifecycleManager {

  static final Map<Class<?>, PersistenceContext> clsUns = new ConcurrentHashMap<>();

  final Logger logger = Logger.getLogger(this.getClass().getName());

  @Inject
  @Any
  Instance<EntityManagerFactory> emfs;

  @Inject
  JTAJPAUnitOfWorksManager unitOfWorksManager;

  @Inject
  PersistenceService jpaService;

  @Override
  public PersistenceContext getPersistenceContext(Class<?> cls) {
    return clsUns.get(cls);
  }

  @Override
  public void on(@Observes(
      during = TransactionPhase.IN_PROGRESS) @Priority(APPLICATION + 1000) LifecycleManageEvent e) {
    if (e.getSource() != null) {
      Entity entity = forceCast(e.getSource());
      boolean effectImmediately = e.isEffectImmediately();
      handle(entity, e.getAction(), effectImmediately, getPersistenceContext(entity.getClass()));
      logger.fine(
          () -> String.format("Handle %s %s", entity.getClass().getName(), e.getAction().name()));
    }
  }

  protected void handle(Entity entity, LifecycleAction action, boolean effectImmediately,
      PersistenceContext pc) {
    EntityManager em = unitOfWorksManager.getCurrentUnitOfWork().getEntityManager(pc);
    if (action == LifecycleAction.PERSIST) {
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
    } else if (action == LifecycleAction.DESTROY) {
      em.remove(entity);
      if (effectImmediately) {
        em.flush();
      }
    } else {
      em.refresh(entity);
    }
  }

  @PostConstruct
  void onPostConstruct() {
    emfs.forEach(emf -> {
      String puNme = asString(emf.getProperties().get(PersistenceNames.PU_NME_KEY), null);
      Set<EntityType<?>> entities = emf.getMetamodel().getEntities();
      entities.stream().map(ManagedType::getJavaType)
          .forEach(cls -> clsUns.put(cls, jpaService.getPersistenceContext(puNme)));
    });
    logger.info(() -> "Initialized JPAPersistenceService.");
  }
}
