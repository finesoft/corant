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
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import org.corant.suites.ddd.annotation.qualifier.JPA;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.suites.ddd.message.MessageService;
import org.corant.suites.ddd.saga.SagaService;
import org.corant.suites.ddd.unitwork.JtaJpaUnitOfWorksManager;

/**
 * @author bingo 上午11:27:31
 *
 */
@JPA
@InfrastructureServices
@ApplicationScoped
public class TestUnitOfWorksManager extends JtaJpaUnitOfWorksManager {

  @Inject
  @PersistenceUnit(unitName = "dmmsPu")
  EntityManagerFactory entityManagerFactory;

  @Inject
  MessageService messageService;

  @Inject
  SagaService sagaService;


  public TestUnitOfWorksManager() {}

  @Override
  public EntityManagerFactory getEntityManagerFactory() {
    return this.entityManagerFactory;
  }

  @Override
  public MessageService getMessageService() {
    return this.messageService;
  }

  @Override
  public SagaService getSagaService() {
    return this.sagaService;
  }

}
