/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.asosat.ddd.saga;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.transaction.Transactional;
import org.corant.shared.exception.NotSupportedException;
import org.corant.suites.ddd.annotation.qualifier.MsgQue.MsgQueLiteral;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.suites.ddd.message.Message;
import org.corant.suites.ddd.repository.JpaRepository;
import org.corant.suites.ddd.saga.Saga;
import org.corant.suites.ddd.saga.SagaService;
import org.corant.suites.jpa.shared.JpaUtils;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午2:49:32
 *
 */
@ApplicationScoped
@InfrastructureServices
public abstract class AbstractSagaService implements SagaService {

  @Inject
  protected Instance<SagaManager> sagaManagers;

  protected final Map<Class<?>, Boolean> persistSagaClasses =
      new ConcurrentHashMap<>(256, 0.75f, 256);

  @Override
  public Stream<SagaManager> getManagers(Annotation... annotations) {
    Instance<SagaManager> inst = sagaManagers.select(annotations);
    if (!inst.isUnsatisfied()) {
      return inst.stream();
    } else {
      return Stream.empty();
    }
  }

  @Transactional
  @Override
  public void persist(Saga saga) {
    if (persistSagaClasses.computeIfAbsent(saga.getClass(), JpaUtils::isPersistenceEntityClass)) {
      getRepo().persist(saga);
      getRepo().getEntityManager().flush();
    } else {
      throw new NotSupportedException();
    }
  }

  @Transactional
  @Override
  public void trigger(Message message) {
    getManagers(MsgQueLiteral.of(message.queueName())).forEach(sm -> {
      Saga saga = sm.begin(message);
      persist(saga);
    });
  }

  protected abstract JpaRepository getRepo();

}
