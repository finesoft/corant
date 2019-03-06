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
package org.corant.asosat.ddd.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.ManagedType;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.suites.ddd.unitwork.AbstractJpaPersistenceService;
import org.corant.suites.jpa.shared.JpaExtension;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午3:49:06
 *
 */
@ApplicationScoped
@InfrastructureServices
public class JpaLifecycleService extends AbstractJpaPersistenceService {

  protected static final Map<Class<?>, Named> clsUns = new ConcurrentHashMap<>();

  @Inject
  @Any
  Instance<EntityManagerFactory> emfs;

  @Inject
  JpaExtension ext;

  @Override
  public Named resolvePuQualifier(Class<?> entityClass) {
    return clsUns.get(entityClass);
  }

  @PostConstruct
  void onPostConstruct() {
    // FIXME ugly implemention
    emfs.forEach(Object::toString);
    ext.getEntityManagerFactories().forEach((n, emf) -> {
      emf.getMetamodel().getEntities().stream().map(ManagedType::getJavaType).forEach(cls -> {
        clsUns.put(cls, n);
      });
    });
    logger.info(() -> "initialized JpaLifecycleService.");
  }

}
