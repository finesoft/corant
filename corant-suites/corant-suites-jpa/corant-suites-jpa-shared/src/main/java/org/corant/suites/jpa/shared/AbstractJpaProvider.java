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
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManagerFactory;
import org.corant.suites.jpa.shared.metadata.PersistenceUnitMetaData;

/**
 * corant-suites-jpa-shared
 *
 * @author bingo 上午11:08:47
 *
 */
@ApplicationScoped
public abstract class AbstractJpaProvider {

  protected static final Map<PersistenceUnitMetaData, EntityManagerFactory> EMFS =
      new ConcurrentHashMap<>();

  protected Logger logger = Logger.getLogger(getClass().getName());

  public EntityManagerFactory get(PersistenceUnitMetaData metaData) {
    return EMFS.computeIfAbsent(metaData, this::build);
  }

  protected abstract EntityManagerFactory build(PersistenceUnitMetaData metaData);
}
