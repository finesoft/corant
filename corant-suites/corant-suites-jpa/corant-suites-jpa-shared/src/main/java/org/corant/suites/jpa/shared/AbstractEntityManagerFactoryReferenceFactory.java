package org.corant.suites.jpa.shared;

import javax.persistence.EntityManagerFactory;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

public abstract class AbstractEntityManagerFactoryReferenceFactory
    implements ResourceReferenceFactory<EntityManagerFactory> {

  @Override
  public ResourceReference<EntityManagerFactory> createResource() {
    return null;
  }


}
