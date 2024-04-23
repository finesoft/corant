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
package org.corant.modules.jpa.shared;

import static java.lang.String.format;
import static org.corant.shared.util.Assertions.shouldBeNull;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Objects.areEqual;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;
import jakarta.persistence.PersistenceUnit;
import jakarta.persistence.SynchronizationType;
import jakarta.transaction.TransactionScoped;
import org.corant.context.ComponentManager.AbstractComponentManager;
import org.corant.context.Contexts;
import org.corant.modules.jpa.shared.metadata.PersistenceUnitInfoMetaData;
import org.corant.modules.jta.shared.TransactionService;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.normal.Names.PersistenceNames;
import org.corant.shared.ubiquity.Mutable.MutableObject;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-jpa-shared
 * <p>
 * TODO FIXME Need to readjust how em and emf lookup, resolve all emfs in boost stage for warm-up
 * the application.
 *
 * @author bingo 下午2:10:42
 */
@ApplicationScoped
public class JPAService implements PersistenceService {

  protected final Logger logger = Logger.getLogger(JPAService.class.getName());// static
  protected final Map<PersistenceUnit, EntityManagerFactory> emfs = new ConcurrentHashMap<>(); // static

  @Inject
  @Any
  protected JPAExtension extension;

  @Inject
  @Any
  protected Instance<JPAProvider> providers;

  @Inject
  @Any
  protected RsEntityManagerManager rsEmManager;

  @Inject
  @Any
  protected TsEntityManagerManager tsEmManager;

  @Inject
  @Any
  protected Instance<EntityManagerConfigurator> emConfigurator;

  @Inject
  @Any
  protected Instance<EntityManagerFactoryConfigurator> emfConfigurator;

  @Override
  public EntityManager createStandAloneEntityManager(PersistenceContext pc) {
    final MutableObject<EntityManager> entityManager = new MutableObject<>(
        getEntityManagerFactory(PersistenceUnitLiteral.of(pc)).createEntityManager(
            pc.synchronization(), PersistenceContextLiteral.extractProperties(pc.properties())));
    if (!emConfigurator.isUnsatisfied()) {
      emConfigurator.stream().sorted(Sortable::compare).forEachOrdered(entityManager::apply);
    }
    return entityManager.get();
  }

  @Override
  public EntityManager getEntityManager(PersistenceContext pc) {
    if (pc.type() == PersistenceContextType.TRANSACTION) {
      return createJTAManagedEntityManager(pc);
    } else if (Contexts.isContextActive(RequestScoped.class)) {
      return createNoJTAManagedEntityManager(pc);
    } else {
      throw new NotSupportedException("Only support request and transaction scope entity manager.");
    }
  }

  @Override
  public EntityManagerFactory getEntityManagerFactory(PersistenceUnit pu) {
    return emfs.computeIfAbsent(pu, p -> {
      PersistenceUnitInfoMetaData puim = shouldNotNull(extension.getPersistenceUnitInfoMetaData(pu),
          "Can't find any metadata for persistence unit %s", pu);
      Named jp = NamedLiteral.of(puim.getPersistenceProviderClassName());
      Instance<JPAProvider> provider = providers.select(jp);
      shouldBeTrue(provider.isResolvable(), "Can not find jpa provider named %s.", jp.value());
      final EntityManagerFactory emf = provider.get().buildEntityManagerFactory(puim,
          mapOf(PersistenceNames.PU_NME_KEY, pu.unitName()));
      if (!emfConfigurator.isUnsatisfied()) {
        emfConfigurator.stream().sorted(Sortable::compare).forEachOrdered(c -> c.accept(emf));
      }
      return emf;
    });
  }

  protected ExtendedEntityManager createJTAManagedEntityManager(PersistenceContext pc) {
    shouldBeTrue(TransactionService.isCurrentTransactionActive(),
        "Unable to obtain the transaction scope entity manager, the transaction isn't active!");// FIXME
    final ExtendedEntityManager em =
        tsEmManager.computeIfAbsent(pc, p -> newEntityManager(p, true));
    logger
        .fine(() -> format("Get transactional scope entity manager [%s] for persistence unit [%s].",
            em, pc.unitName()));
    return em;
  }

  protected ExtendedEntityManager createNoJTAManagedEntityManager(PersistenceContext pc) {
    Set<PersistenceContext> exists = rsEmManager.getCurrentPersistenceContexts();
    if (isNotEmpty(exists)) {
      // JavaPersistence 2.0 #7.6.3.1 Inheritance of Extended Persistence Context
      exists.stream().filter(p -> areEqual(p.unitName(), pc.unitName())).findFirst()
          .ifPresent(p -> shouldBeTrue(areEqual(p.synchronization(), pc.synchronization()),
              "Get entity manager error, the synchronization of persistence context must be equal with the already exist one that has same unit name."));
    }
    final ExtendedEntityManager em =
        rsEmManager.computeIfAbsent(pc, p -> newEntityManager(p, false));
    if (pc.synchronization() == SynchronizationType.SYNCHRONIZED
        && TransactionService.isCurrentTransactionActive() && !em.isJoinedToTransaction()) {
      shouldBeNull(tsEmManager.get(pc), "");// TODO FIXME
      em.joinTransaction();
    }
    logger.fine(() -> format("Get request scope entity manager [%s] for persistence unit [%s].", em,
        pc.unitName()));
    return em;
  }

  protected ExtendedEntityManager newEntityManager(PersistenceContext p, boolean transaction) {
    final MutableObject<EntityManager> delegate = new MutableObject<>(
        getEntityManagerFactory(PersistenceUnitLiteral.of(p)).createEntityManager(
            p.synchronization(), PersistenceContextLiteral.extractProperties(p.properties())));
    if (!emConfigurator.isUnsatisfied()) {
      emConfigurator.stream().sorted(Sortable::compare).forEachOrdered(delegate::apply);
    }
    return new ExtendedEntityManager(delegate.get(), transaction);
  }

  @PreDestroy
  protected synchronized void onPreDestroy() {
    emfs.forEach((k, v) -> {
      if (v.isOpen()) {
        v.close();
        logger.info(() -> format("Close entity manager factory [%s].", k));
      }
    });
  }

  @Produces
  protected EntityManager produceEntityManager(InjectionPoint ip) {
    final Annotated annotated = ip.getAnnotated();
    final PersistenceContext pc = annotated.getAnnotation(PersistenceContext.class);
    return getEntityManager(pc);
  }

  /**
   * corant-modules-jpa-shared
   * <p>
   * TODO use entity manager factory as components key
   *
   * @author bingo 下午4:09:51
   *
   */
  public abstract static class EntityManagerManager
      extends AbstractComponentManager<PersistenceContext, ExtendedEntityManager> {

    private static final long serialVersionUID = 2369488429315443982L;

    public Set<PersistenceContext> getCurrentPersistenceContexts() {
      return components.keySet();
    }

    @Override
    protected void preDestroy() {
      Exception ex = null;
      for (final ExtendedEntityManager c : components.values()) {
        try {
          logger.fine(() -> format("Close entity manager [%s].", c));
          if (c.isOpen()) {
            c.destroy();
          }
        } catch (final Exception e) {
          ex = e;
        }
      }
      if (ex != null) {
        throw new CorantRuntimeException(ex);
      }
    }
  }

  /**
   * corant-modules-jpa-shared
   *
   * @author bingo 下午4:09:58
   *
   */
  @RequestScoped
  public static class RsEntityManagerManager extends EntityManagerManager {

    private static final long serialVersionUID = -3784150786218329029L;

  }

  /**
   * corant-modules-jpa-shared
   *
   * @author bingo 下午4:10:01
   *
   */
  @TransactionScoped
  public static class TsEntityManagerManager extends EntityManagerManager {

    private static final long serialVersionUID = -3016209011507920624L;

  }
}
