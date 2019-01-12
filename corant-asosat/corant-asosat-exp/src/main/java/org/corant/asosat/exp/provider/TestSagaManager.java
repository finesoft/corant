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

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import org.corant.suites.ddd.annotation.qualifier.JPA;
import org.corant.suites.ddd.annotation.stereotype.DomainServices;
import org.corant.suites.ddd.message.Message;
import org.corant.suites.ddd.message.Message.MessageIdentifier;
import org.corant.suites.ddd.model.Aggregate.AggregateIdentifier;
import org.corant.suites.ddd.saga.Saga;
import org.corant.suites.ddd.saga.SagaService.SagaManager;

/**
 * @author bingo 下午6:05:25
 *
 */
@DomainServices
@ApplicationScoped
@Transactional
public class TestSagaManager implements SagaManager {

  @Inject
  @JPA
  protected TestRepository repo;

  public TestSagaManager() {}

  @Override
  public Saga begin(Message message) {
    return new Saga() {

      private static final long serialVersionUID = 7308472929657071999L;

      @Override
      public Collection<?> getAttributes() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Serializable getId() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Object getOriginal() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Collection<? extends MessageIdentifier> getRelevantMessages() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public String getTrackingToken() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public MessageIdentifier getTriggerMessage() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public boolean isActived() {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public Saga withTrackingToken(String trackingToken) {
        // TODO Auto-generated method stub
        return null;
      }

    };
  }

  @Override
  public void end(Message message) {

  }

  @Override
  public Saga get(String queue, String trackingToken) {
    return null;
  }

  @Override
  public List<Saga> select(AggregateIdentifier aggregateIdentifier) {
    return null;
  }

}
