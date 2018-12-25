package org.corant.suites.jpa.shared;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.jboss.weld.injection.spi.JpaInjectionServices;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

public abstract class AbstractJpaInjectionServices implements JpaInjectionServices {

  protected static Map<String, ResourceReferenceFactory<EntityManagerFactory>> emfs =
      new ConcurrentHashMap<>();
  protected static Map<String, ResourceReferenceFactory<EntityManager>> ems =
      new ConcurrentHashMap<>();

  @Override
  public void cleanup() {
    for (ResourceReferenceFactory<EntityManagerFactory> rrf : emfs.values()) {
      rrf.createResource().getInstance().close();
    }
    emfs.clear();
  }

  @Override
  public ResourceReferenceFactory<EntityManager> registerPersistenceContextInjectionPoint(
      InjectionPoint injectionPoint) {
    return null;
  }

  @Override
  public ResourceReferenceFactory<EntityManagerFactory> registerPersistenceUnitInjectionPoint(
      InjectionPoint injectionPoint) {
    return null;
  }

  @Override
  public EntityManager resolvePersistenceContext(InjectionPoint injectionPoint) {
    return null;
  }

  @Override
  public EntityManagerFactory resolvePersistenceUnit(InjectionPoint injectionPoint) {
    return null;
  }

}
