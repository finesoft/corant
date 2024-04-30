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
import jakarta.enterprise.inject.literal.NamedLiteral;
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
import org.corant.shared.normal.Names.PersistenceNames;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-jpa-shared
 *
 * <p>
 * TODO FIXME Need to readjust how em and emf lookup, resolve all emfs in boost stage for warm-up
 * the application.
 *
 * @author bingo 下午2:10:42
 */
@ApplicationScoped
public class JPAService implements PersistenceService {

  protected final Logger logger = Logger.getLogger(JPAService.class.getName()); // static
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
  public EntityManager createEntityManager(PersistenceContext pc) {
    final EntityManager em =
        getEntityManagerFactory(PersistenceUnitLiteral.of(pc)).createEntityManager(
            pc.synchronization(), PersistenceContextLiteral.extractProperties(pc.properties()));
    if (!emConfigurator.isUnsatisfied()) {
      emConfigurator.stream().sorted(Sortable::compare).forEachOrdered(c -> c.accept(pc, em));
    }
    return em;
  }

  @Override
  public EntityManager getEntityManager(PersistenceContext pc) {
    if (pc.type() == PersistenceContextType.TRANSACTION) {
      return createTxScopedManagedEntityManager(pc);
    } else if (Contexts.isContextActive(RequestScoped.class)) {
      return createRsScopedManagedEntityManager(pc);
    } else {
      // create stand-alone entity manager, maintained by caller
      return createEntityManager(pc);
    }
  }

  @Override
  public EntityManagerFactory getEntityManagerFactory(PersistenceUnit pu) {
    return emfs.computeIfAbsent(pu, p -> {
      PersistenceUnitInfoMetaData unit = shouldNotNull(extension.getPersistenceUnitInfoMetaData(pu),
          "Can't find any metadata for persistence unit %s", pu);
      Named jp = NamedLiteral.of(unit.getPersistenceProviderClassName());
      Instance<JPAProvider> provider = providers.select(jp);
      shouldBeTrue(provider.isResolvable(), "Can not find jpa provider named %s.", jp.value());
      final EntityManagerFactory emf = provider.get().buildEntityManagerFactory(unit,
          mapOf(PersistenceNames.PU_NME_KEY, pu.unitName()));
      if (!emfConfigurator.isUnsatisfied()) {
        emfConfigurator.stream().sorted(Sortable::compare).forEachOrdered(c -> c.accept(pu, emf));
      }
      return emf;
    });
  }

  protected ExtendedEntityManager createRsScopedManagedEntityManager(PersistenceContext pc) {
    Set<PersistenceContext> exists = rsEmManager.getCurrentPersistenceContexts();
    if (isNotEmpty(exists)) {
      // JavaPersistence 2.0 #7.6.3.1 Inheritance of Extended Persistence Context
      exists.stream().filter(p -> areEqual(p.unitName(), pc.unitName())).findFirst()
          .ifPresent(p -> shouldBeTrue(areEqual(p.synchronization(), pc.synchronization()),
              "Get entity manager error, the synchronization of persistence context must be "
                  + "equal with the already exist one that has same unit name."));
    }
    final ExtendedEntityManager em = new ExtendedEntityManager(
        () -> rsEmManager.computeIfAbsent(pc, this::createEntityManager), false);
    if (pc.synchronization() == SynchronizationType.SYNCHRONIZED
        && TransactionService.isCurrentTransactionActive() && !em.isJoinedToTransaction()) {
      shouldBeNull(tsEmManager.get(pc), ""); // TODO FIXME
      em.joinTransaction();
    }
    logger.fine(() -> format("Get request scoped entity manager for persistence unit [%s].",
        pc.unitName()));
    return em;
  }

  protected ExtendedEntityManager createTxScopedManagedEntityManager(PersistenceContext pc) {
    ExtendedEntityManager em = new ExtendedEntityManager(
        () -> tsEmManager.computeIfAbsent(pc, this::createEntityManager), true);
    logger.fine(() -> format("Get transactional scoped entity manager for persistence unit [%s].",
        pc.unitName()));
    return em;
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

  /**
   * corant-modules-jpa-shared
   *
   * <p>
   * TODO use entity manager factory as components key
   *
   * @author bingo 下午4:09:51
   */
  public abstract static class EntityManagerManager
      extends AbstractComponentManager<PersistenceContext, EntityManager> {

    private static final long serialVersionUID = 2369488429315443982L;

    public Set<PersistenceContext> getCurrentPersistenceContexts() {
      return components.keySet();
    }

    @Override
    protected void preDestroy() {
      Exception ex = null;
      for (final EntityManager c : components.values()) {
        try {
          logger.fine(() -> format("Close entity manager [%s].", c));
          if (c.isOpen()) {
            c.close();
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
   */
  @RequestScoped
  public static class RsEntityManagerManager extends EntityManagerManager {

    private static final long serialVersionUID = -3784150786218329029L;
  }

  /**
   * corant-modules-jpa-shared
   *
   * @author bingo 下午4:10:01
   */
  @TransactionScoped
  public static class TsEntityManagerManager extends EntityManagerManager {

    private static final long serialVersionUID = -3016209011507920624L;
  }
}
