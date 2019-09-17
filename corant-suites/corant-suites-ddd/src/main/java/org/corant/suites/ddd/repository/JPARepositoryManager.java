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

import static org.corant.shared.util.StringUtils.EMPTY;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.persistence.PersistenceContext;
import org.corant.kernel.api.PersistenceService.PersistenceContextLiteral;
import org.corant.suites.ddd.annotation.qualifier.PU;
import org.corant.suites.ddd.unitwork.JTAJPAUnitOfWorksManager;

/**
 * corant-suites-ddd
 *
 * @author bingo 下午7:09:18
 *
 */
@ApplicationScoped
public class JPARepositoryManager {

  static final Map<PersistenceContext, JPARepository> respositories = new ConcurrentHashMap<>();

  @Inject
  JTAJPAUnitOfWorksManager unitOfWorkManager;

  @Produces
  @PU
  JPARepository produce(InjectionPoint ip) {
    final Annotated annotated = ip.getAnnotated();
    final PU pu = annotated.getAnnotation(PU.class);
    PersistenceContext pc = PersistenceContextLiteral.of(pu == null ? EMPTY : pu.value());
    return respositories.computeIfAbsent(pc, (p) -> {
      return new DefaultJPARepository(pc, unitOfWorkManager);
    });
  }

  /**
   * corant-suites-ddd
   *
   * @author bingo 下午5:06:40
   *
   */
  public static final class DefaultJPARepository extends AbstractJPARepository {

    protected DefaultJPARepository(PersistenceContext pc, JTAJPAUnitOfWorksManager uofm) {
      super();
      logger = Logger.getLogger(this.getClass().getName());
      persistenceContext = pc;
      unitOfWorkManager = uofm;
    }
  }
}
