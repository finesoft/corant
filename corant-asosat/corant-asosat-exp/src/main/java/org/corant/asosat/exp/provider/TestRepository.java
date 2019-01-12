/*
 * Copyright (c) 2013-2018. BIN.CHEN
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
package org.corant.asosat.exp.provider;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.persistence.EntityManager;
import org.corant.suites.ddd.annotation.qualifier.JPA;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.suites.ddd.annotation.stereotype.Repositories;
import org.corant.suites.ddd.repository.AbstractJpaRepository;

/**
 * @author bingo 下午5:58:42
 *
 */
@JPA
@Default
@ApplicationScoped
@Repositories
@InfrastructureServices
public class TestRepository extends AbstractJpaRepository {

  public TestRepository() {}

  @Override
  public EntityManager getEntityManager() {
    return super.getEntityManager();
  }

}
