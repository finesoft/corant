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
import static org.corant.shared.util.Objects.defaultObject;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.transaction.TransactionScoped;
import org.corant.context.ComponentManager.AbstractComponentManager;
import org.corant.modules.ddd.Entity.EntityManagerProvider;
import org.corant.modules.ddd.shared.unitwork.AbstractJTAJPAUnitOfWork;
import org.corant.modules.ddd.shared.unitwork.UnitOfWorks;
import org.corant.modules.jpa.shared.PersistenceService.PersistenceContextLiteral;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.normal.Names.PersistenceNames;
import org.corant.shared.util.Configurations;

/**
 * corant-modules-ddd-shared
 * <p>
 * Default entity manager provider that use to retrieve entity manager from current unit of work,
 * also supports bind/un-bind a transaction scoped persistence context for special entity classes.
 *
 * @author bingo 下午8:56:24
 */
@ApplicationScoped
public class DefaultEntityManagerProvider implements EntityManagerProvider {

  protected final Map<Class<?>, PersistenceContext> defaultPersistenceContexts =
      new ConcurrentHashMap<>();

  protected final Logger logger = Logger.getLogger(this.getClass().getName());

  protected final boolean allowSpecializePersistenceContextBinding = Configurations.getConfigValue(
      "corant.ddd.entity-manager-provider.allow-specialize-persistence-context-binding",
      Boolean.class, false);

  @Inject
  protected UnitOfWorks unitOfWorks;

  @Inject
  protected TransactionalPersistenceContexts specializePersistenceContexts;

  public void bindPersistenceContext(PersistenceContext ctx, Class<?>... entityClasses) {
    if (!allowSpecializePersistenceContextBinding) {
      throw new NotSupportedException("Can't support specialize persistence context binding");
    }
    if (ctx != null) {
      for (Class<?> cls : entityClasses) {
        if (cls != null) {
          specializePersistenceContexts.put(cls, ctx);
        }
      }
    }
  }

  public void bindPersistenceContext(String unitName, Class<?>... classes) {
    bindPersistenceContext(PersistenceContextLiteral.of(unitName), classes);
  }

  @Override
  public EntityManager getEntityManager(Class<?> cls) {
    return getEntityManager(getPersistenceContext(cls));
  }

  @Override
  public EntityManager getEntityManager(PersistenceContext pc) {
    AbstractJTAJPAUnitOfWork unitOfWork = unitOfWorks.currentDefaultUnitOfWork();
    if (unitOfWork != null) {
      return unitOfWork.getEntityManager(pc);
    }
    throw new NotSupportedException();
  }

  @Override
  public PersistenceContext getPersistenceContext(Class<?> entityClass) {
    if (allowSpecializePersistenceContextBinding) {
      return defaultObject(specializePersistenceContexts.get(entityClass),
          () -> defaultPersistenceContexts.get(entityClass));
    } else {
      return defaultPersistenceContexts.get(entityClass);
    }
  }

  public boolean isAllowSpecializePersistenceContextBinding() {
    return allowSpecializePersistenceContextBinding;
  }

  public void unbindPersistenceContext(Class<?>... entityClasses) {
    if (!allowSpecializePersistenceContextBinding) {
      throw new NotSupportedException("Can't support specialize persistence context binding");
    }
    for (Class<?> cls : entityClasses) {
      if (cls != null) {
        specializePersistenceContexts.remove(cls);
      }
    }
  }

  @PostConstruct
  protected synchronized void onPostConstruct() {
    select(EntityManagerFactory.class, Any.Literal.INSTANCE).forEach(emf -> {
      String puNme = asString(emf.getProperties().get(PersistenceNames.PU_NME_KEY), null);
      Set<EntityType<?>> entities = emf.getMetamodel().getEntities();
      entities.stream().map(ManagedType::getJavaType)
          .forEach(cls -> defaultPersistenceContexts.put(cls, PersistenceContextLiteral.of(puNme)));
    });
    logger.fine(() -> "Initialized EntityMetamodels.");
  }

  @PreDestroy
  protected synchronized void onPreDestroy() {
    defaultPersistenceContexts.clear();
  }

  @TransactionScoped
  public static class TransactionalPersistenceContexts
      extends AbstractComponentManager<Class<?>, PersistenceContext> {

    private static final long serialVersionUID = -6960941021710322254L;

    @Override
    protected void preDestroy() {
      components.clear();
    }
  }
}
