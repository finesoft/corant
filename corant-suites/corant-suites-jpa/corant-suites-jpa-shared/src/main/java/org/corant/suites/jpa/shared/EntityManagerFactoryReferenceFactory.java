/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.suites.jpa.shared;

import java.util.Map;
import javax.persistence.EntityManagerFactory;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

public class EntityManagerFactoryReferenceFactory
    implements ResourceReferenceFactory<EntityManagerFactory> {

  protected final EntityManagerFactoryReference emfr;

  /**
   * @param emfr
   */
  public EntityManagerFactoryReferenceFactory(EntityManagerFactoryReference emfr) {
    super();
    this.emfr = emfr;
  }

  public EntityManagerFactoryReferenceFactory(String persistenceUnitName) {
    emfr = new EntityManagerFactoryReference(persistenceUnitName);
  }

  public EntityManagerFactoryReferenceFactory(String persistenceUnitName,
      Map<String, Object> properties) {
    emfr = new EntityManagerFactoryReference(persistenceUnitName, properties);
  }

  @Override
  public ResourceReference<EntityManagerFactory> createResource() {
    return emfr;
  }

}
