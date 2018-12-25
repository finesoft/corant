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

import static org.corant.shared.util.ObjectUtils.ifNull;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.corant.shared.normal.Names.PersistenceNames;
import org.jboss.weld.injection.spi.ResourceReference;

/**
 * corant-suites-jpa-shared
 *
 * @author bingo 下午6:46:11
 *
 */
public class EntityManagerFactoryReference
    implements ResourceReference<EntityManagerFactory> {

  protected final String persistenceUnitName;
  protected final Map<String, Object> properties = new HashMap<>();

  protected volatile EntityManagerFactory entityManagerFactory;

  public EntityManagerFactoryReference(String persistenceUnitName) {
    super();
    this.persistenceUnitName = ifNull(persistenceUnitName, PersistenceNames.PU_DFLT_NME);
  }

  public EntityManagerFactoryReference(String persistenceUnitName,
      Map<String, Object> properties) {
    this(persistenceUnitName);
    if (properties != null) {
      this.properties.putAll(properties);
    }
  }

  @Override
  public synchronized EntityManagerFactory getInstance() {
    if (entityManagerFactory == null) {
      entityManagerFactory =
          Persistence.createEntityManagerFactory(persistenceUnitName, properties);
    }
    return entityManagerFactory;
  }

  @Override
  public void release() {

  }

}
