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

import static org.corant.context.Beans.resolveApply;
import static org.corant.context.Beans.select;
import static org.corant.shared.util.Assertions.shouldBeNull;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Objects.areEqual;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceUnit;
import javax.persistence.SynchronizationType;
import javax.transaction.TransactionScoped;
import org.corant.context.ComponentManager.AbstractComponentManager;
import org.corant.context.Contexts;
import org.corant.modules.jpa.shared.metadata.PersistenceUnitInfoMetaData;
import org.corant.modules.jta.shared.TransactionService;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.normal.Names.PersistenceNames;

/**
 * corant-modules-jpa-shared
 *
 * @author bingo 下午2:10:42
 *
 */
@ApplicationScoped
public class JPAService implements PersistenceService {

  protected final Logger logger = Logger.getLogger(JPAService.class.getName());// static
  protected final Map<PersistenceUnit, EntityManagerFactory> emfs = new ConcurrentHashMap<>(); // static

  @Inject
  @Any
  protected RsEntityManagerManager rsEmManager;

  @Inject
  @Any
  protected TsEntityManagerManager tsEmManager;

  @Inject
  @Any
  protected Instance<EntityManagerConfigurator> emConfigurator;

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
      PersistenceUnitInfoMetaData puim =
          resolveApply(JPAExtension.class, b -> b.getPersistenceUnitInfoMetaData(pu));
      Named jp = NamedLiteral.of(puim.getPersistenceProviderClassName());
      Instance<JPAProvider> provider = select(JPAProvider.class, jp);
      shouldBeTrue(provider.isResolvable(), "Can not find jpa provider named %s.", jp.value());
      return provider.get().buildEntityManagerFactory(puim,
          mapOf(PersistenceNames.PU_NME_KEY, pu.unitName()));
    });
  }

  protected ExtendedEntityManager createJTAManagedEntityManager(PersistenceContext pc) {
    shouldBeTrue(TransactionService.isCurrentTransactionActive(),
        "Unable to obtain the transaction scope entity manager, the transaction isn't active!");// FIXME
    final ExtendedEntityManager em =
        tsEmManager.computeIfAbsent(pc, p -> newEntityManager(p, true));
    logger.fine(() -> String.format(
        "Get transactional scope entity manager [%s] for persistence unit [%s].", em,
        pc.unitName()));
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
    logger.fine(() -> String.format(
        "Get request scope entity manager [%s] for persistence unit [%s].", em, pc.unitName()));
    return em;
  }

  protected ExtendedEntityManager newEntityManager(PersistenceContext p, boolean transaction) {
    final EntityManager delegate =
        getEntityManagerFactory(PersistenceUnitLiteral.of(p)).createEntityManager(
            p.synchronization(), PersistenceContextLiteral.extractProperties(p.properties()));
    if (!emConfigurator.isUnsatisfied()) {
      emConfigurator.stream().forEach(c -> c.accept(delegate));
    }
    return new ExtendedEntityManager(delegate, transaction);
  }

  @PreDestroy
  protected synchronized void onPreDestroy() {
    emfs.forEach((k, v) -> {
      if (v.isOpen()) {
        v.close();
        logger.info(() -> String.format("Close entity manager factory [%s].", k));
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
   *
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
          logger.fine(() -> String.format("Close entity manager [%s].", c));
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
