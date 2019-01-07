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
package org.corant.asosat.ddd.saga;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.transaction.Transactional;
import org.corant.suites.ddd.annotation.qualifier.MQ.MQLiteral;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.suites.ddd.message.Message;
import org.corant.suites.ddd.saga.Saga;
import org.corant.suites.ddd.saga.SagaService;

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
  public void trigger(Message message) {
    getManagers(MQLiteral.of(message.queueName())).forEach(sm -> {
      Saga saga = sm.begin(message);
      persist(saga);
    });
  }

}
