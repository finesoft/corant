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
package org.corant.suites.jpa.shared.inject;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.inject.Instance;
import javax.persistence.Cache;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.spi.PersistenceUnitInfo;
import org.corant.Corant;
import org.corant.suites.jpa.shared.AbstractJpaProvider;
import org.corant.suites.jpa.shared.JpaExtension;
import org.corant.suites.jpa.shared.inject.JpaProvider.JpaProviderLiteral;
import org.corant.suites.jpa.shared.metadata.PersistenceUnitInfoMetaData;

/**
 * corant-suites-jpa-shared
 *
 * @author bingo 上午9:56:24
 *
 */
public class ExtendedEntityManagerFactory implements EntityManagerFactory {

  static final Map<PersistenceUnitInfoMetaData, ExtendedEntityManagerFactory> emfs =
      new ConcurrentHashMap<>();
  private EntityManagerFactory delegate;
  private PersistenceUnitInfo persistenceUnitInfo;

  protected ExtendedEntityManagerFactory() {}

  protected ExtendedEntityManagerFactory(EntityManagerFactory delegate,
      PersistenceUnitInfo persistenceUnitInfo) {
    this.delegate = delegate;
    this.persistenceUnitInfo = persistenceUnitInfo;
  }

  static ExtendedEntityManagerFactory of(PersistenceUnitInfoMetaData pu) {
    return emfs.computeIfAbsent(pu, p -> {
      String providerName = p.getPersistenceProviderClassName();
      JpaProvider jp = JpaProviderLiteral.of(providerName);
      Instance<AbstractJpaProvider> provider =
          Corant.instance().select(AbstractJpaProvider.class, jp);
      shouldBeTrue(provider.isResolvable(), "Can not find jpa provider named %s.", jp.value());
      final ExtendedEntityManagerFactory emf =
          new ExtendedEntityManagerFactory(provider.get().buildEntityManagerFactory(p), p);
      return emf;
    });

  }

  static ExtendedEntityManagerFactory of(String unitName) {
    Instance<JpaExtension> ext = Corant.instance().select(JpaExtension.class);
    shouldBeTrue(ext.isResolvable(), "Can not find jpa extension.");
    PersistenceUnitInfoMetaData pu = ext.get().getPersistenceUnitInfoMetaData(unitName);
    return of(pu);
  }

  @Override
  public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
    delegate.addNamedEntityGraph(graphName, entityGraph);
  }

  @Override
  public void addNamedQuery(String name, Query query) {
    delegate.addNamedQuery(name, query);
  }

  @Override
  public void close() {
    delegate.close();
  }

  @Override
  public EntityManager createEntityManager() {
    return delegate.createEntityManager();
  }

  @SuppressWarnings("rawtypes")
  @Override
  public EntityManager createEntityManager(Map map) {
    return delegate.createEntityManager(map);
  }

  @Override
  public EntityManager createEntityManager(SynchronizationType synchronizationType) {
    return delegate.createEntityManager(synchronizationType);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public EntityManager createEntityManager(SynchronizationType synchronizationType, Map map) {
    return delegate.createEntityManager(synchronizationType, map);
  }

  @Override
  public Cache getCache() {
    return delegate.getCache();
  }

  @Override
  public CriteriaBuilder getCriteriaBuilder() {
    return delegate.getCriteriaBuilder();
  }

  public EntityManagerFactory getDelegate() {
    return delegate;
  }

  @Override
  public Metamodel getMetamodel() {
    return delegate.getMetamodel();
  }

  public PersistenceUnitInfo getPersistenceUnitInfo() {
    return persistenceUnitInfo;
  }

  @Override
  public PersistenceUnitUtil getPersistenceUnitUtil() {
    return delegate.getPersistenceUnitUtil();
  }

  @Override
  public Map<String, Object> getProperties() {
    return delegate.getProperties();
  }

  @Override
  public boolean isOpen() {
    return delegate.isOpen();
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    if (cls.isAssignableFrom(getClass())) {
      return cls.cast(this);
    }
    return delegate.unwrap(cls);
  }

}
