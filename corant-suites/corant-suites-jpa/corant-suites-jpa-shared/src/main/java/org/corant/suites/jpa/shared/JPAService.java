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
package org.corant.suites.jpa.shared;

import static org.corant.kernel.util.Instances.resolvableApply;
import static org.corant.kernel.util.Instances.select;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.MapUtils.mapOf;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
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
import javax.transaction.TransactionScoped;
import org.corant.kernel.normal.Names.PersistenceNames;
import org.corant.kernel.service.ComponentManager.AbstractComponentManager;
import org.corant.kernel.service.PersistenceService;
import org.corant.kernel.service.TransactionService;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.jpa.shared.metadata.PersistenceUnitInfoMetaData;

/**
 * corant-suites-jpa-shared
 *
 * @author bingo 下午2:10:42
 *
 */
@ApplicationScoped
public class JPAService implements PersistenceService {

  static final Logger logger = Logger.getLogger(JPAService.class.getName());
  static final Map<PersistenceUnit, EntityManagerFactory> emfs = new ConcurrentHashMap<>();

  @Inject
  @Any
  RsEntityManagerManager rsEmManager;

  @Inject
  @Any
  TsEntityManagerManager tsEmManager;

  @Override
  public EntityManager getEntityManager(PersistenceContext pc) {
    if (pc.type() == PersistenceContextType.TRANSACTION) {
      shouldBeTrue(TransactionService.isCurrentTransactionActive());
      return tsEmManager
          .computeIfAbsent(pc,
              p -> getEntityManagerFactory(PersistenceUnitLiteral.of(p)).createEntityManager(
                  p.synchronization(),
                  PersistenceContextLiteral.extractProperties(pc.properties())));
    } else {
      return rsEmManager
          .computeIfAbsent(pc,
              p -> getEntityManagerFactory(PersistenceUnitLiteral.of(p)).createEntityManager(
                  p.synchronization(),
                  PersistenceContextLiteral.extractProperties(pc.properties())));
    }
  }

  @Override
  public EntityManagerFactory getEntityManagerFactory(PersistenceUnit pu) {
    EntityManagerFactory emf = emfs.computeIfAbsent(pu, p -> {
      PersistenceUnitInfoMetaData puim =
          resolvableApply(JPAExtension.class, b -> b.getPersistenceUnitInfoMetaData(pu));
      Named jp = NamedLiteral.of(puim.getPersistenceProviderClassName());
      Instance<JPAProvider> provider = select(JPAProvider.class, jp);
      shouldBeTrue(provider.isResolvable(), "Can not find jpa provider named %s.", jp.value());
      final EntityManagerFactory newEmf = provider.get().buildEntityManagerFactory(puim,
          mapOf(PersistenceNames.PU_NME_KEY, pu.unitName()));
      return newEmf;
    });
    return emf;
  }

  @Produces
  EntityManager produceEntityManager(InjectionPoint ip) {
    final Annotated annotated = ip.getAnnotated();
    final PersistenceContext pc = annotated.getAnnotation(PersistenceContext.class);
    return getEntityManager(pc);
  }

  /**
   * corant-suites-jpa-shared
   *
   * @author bingo 下午4:09:51
   *
   */
  public static abstract class EntityManagerManager
      extends AbstractComponentManager<PersistenceContext, EntityManager> {

    private static final long serialVersionUID = 2369488429315443982L;

    @Override
    protected void preDestroy() {
      Exception ex = null;
      for (final EntityManager c : components.values()) {
        try {
          logger.fine(() -> String.format("Close entityManager %s", c));
          c.close();
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
   * corant-suites-jpa-shared
   *
   * @author bingo 下午4:09:58
   *
   */
  @RequestScoped
  public static class RsEntityManagerManager extends EntityManagerManager {

    private static final long serialVersionUID = -3784150786218329029L;

  }

  /**
   * corant-suites-jpa-shared
   *
   * @author bingo 下午4:10:01
   *
   */
  @TransactionScoped
  public static class TsEntityManagerManager extends EntityManagerManager {

    private static final long serialVersionUID = -3016209011507920624L;

  }
}
