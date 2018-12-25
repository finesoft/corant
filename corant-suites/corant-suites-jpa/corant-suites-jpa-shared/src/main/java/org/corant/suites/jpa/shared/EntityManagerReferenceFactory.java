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

import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

public class EntityManagerReferenceFactory implements ResourceReferenceFactory<EntityManager> {

  protected final String persistenceUnitName;
  protected final Map<String, Object> properties = new HashMap<>();

  /**
   * @param persistenceUnitName
   */
  public EntityManagerReferenceFactory(String persistenceUnitName) {
    super();
    this.persistenceUnitName = persistenceUnitName;
  }

  @Override
  public ResourceReference<EntityManager> createResource() {
    return null;
  }

}
