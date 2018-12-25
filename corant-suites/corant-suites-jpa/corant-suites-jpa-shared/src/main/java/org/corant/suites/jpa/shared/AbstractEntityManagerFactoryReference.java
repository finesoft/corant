package org.corant.suites.jpa.shared;

import javax.persistence.EntityManagerFactory;
import org.jboss.weld.injection.spi.ResourceReference;

public abstract class AbstractEntityManagerFactoryReference
    implements ResourceReference<EntityManagerFactory> {

  @Override
  public EntityManagerFactory getInstance() {
    return null;
  }

  @Override
  public void release() {

  }

}
