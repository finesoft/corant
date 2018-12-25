package org.corant.suites.jpa.shared;

import javax.persistence.EntityManager;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

public abstract class AbstractEntityManagerReferenceFactory
    implements ResourceReferenceFactory<EntityManager> {

  @Override
  public ResourceReference<EntityManager> createResource() {
    return null;
  }


}
