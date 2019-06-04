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
package org.corant.suites.ddd.unitwork;

import static org.corant.kernel.util.Qualifiers.resolveNamed;
import static org.corant.shared.util.ObjectUtils.asString;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import org.corant.kernel.normal.Names.PersistenceNames;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午3:49:06
 *
 */
@ApplicationScoped
@InfrastructureServices
public class DefaultJPAPersistenceService implements JPAPersistenceService {

  protected static final Map<Class<?>, Annotation> clsUns = new ConcurrentHashMap<>();

  protected final Logger logger = Logger.getLogger(this.getClass().getName());

  @Inject
  @Any
  Instance<EntityManagerFactory> emfs;

  @Override
  public EntityManagerFactory getEntityManagerFactory(Annotation qualifier) {
    if (emfs.select(qualifier).isResolvable()) {
      return emfs.select(qualifier).get();
    }
    throw new CorantRuntimeException("Can not find appropriate entity manager factory %s",
        qualifier);
  }

  @Override
  public Annotation getPersistenceUnitQualifier(Class<?> entityClass) {
    return entityClass == null ? null : clsUns.get(entityClass);
  }

  @PostConstruct
  void onPostConstruct() {
    emfs.forEach(emf -> {
      String puNme = asString(emf.getProperties().get(PersistenceNames.PU_NME_KEY), null);
      Annotation ann = resolveNamed(puNme);
      Set<EntityType<?>> entities = emf.getMetamodel().getEntities();
      entities.stream().map(ManagedType::getJavaType).forEach(cls -> clsUns.put(cls, ann));
    });
    logger.info(() -> "Initialized JPAPersistenceService.");
  }

}
