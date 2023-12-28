/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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

import static org.corant.context.Beans.select;
import static org.corant.shared.util.Objects.asString;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import org.corant.modules.ddd.shared.unitwork.AbstractJTAJPAUnitOfWork;
import org.corant.modules.ddd.shared.unitwork.UnitOfWorks;
import org.corant.modules.jpa.shared.PersistenceService.PersistenceContextLiteral;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.normal.Names.PersistenceNames;

/**
 * corant-modules-ddd-shared
 * <p>
 * FIXME supports multiple persistence context for one entity class
 *
 * @author bingo 下午8:56:24
 */
@ApplicationScoped
public class EntityManagers {

  protected final Map<Class<?>, PersistenceContext> clsUns = new ConcurrentHashMap<>();

  protected final Logger logger = Logger.getLogger(this.getClass().getName());

  @Inject
  protected UnitOfWorks unitOfWorks;

  public EntityManager getEntityManager(Class<?> cls) {
    return getEntityManager(getPersistenceContext(cls));
  }

  public EntityManager getEntityManager(PersistenceContext pc) {
    Optional<AbstractJTAJPAUnitOfWork> uowo = unitOfWorks.currentDefaultUnitOfWork();
    if (uowo.isPresent()) {
      return uowo.get().getEntityManager(pc);
    }
    throw new NotSupportedException();
  }

  public PersistenceContext getPersistenceContext(Class<?> cls) {
    return clsUns.get(cls);
  }

  @PostConstruct
  protected synchronized void onPostConstruct() {
    select(EntityManagerFactory.class, Any.Literal.INSTANCE).forEach(emf -> {
      String puNme = asString(emf.getProperties().get(PersistenceNames.PU_NME_KEY), null);
      Set<EntityType<?>> entities = emf.getMetamodel().getEntities();
      entities.stream().map(ManagedType::getJavaType)
          .forEach(cls -> clsUns.put(cls, PersistenceContextLiteral.of(puNme)));
    });
    logger.fine(() -> "Initialized EntityMetamodels.");
  }

  @PreDestroy
  protected synchronized void onPreDestroy() {
    clsUns.clear();
  }
}
