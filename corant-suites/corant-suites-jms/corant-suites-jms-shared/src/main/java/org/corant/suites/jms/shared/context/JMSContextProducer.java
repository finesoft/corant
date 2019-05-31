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
package org.corant.suites.jms.shared.context;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.jms.JMSContext;
import org.corant.suites.jms.shared.context.JMSContextManager.RequestScopeContextManager;
import org.corant.suites.jms.shared.context.JMSContextManager.TransactionScopeContextManager;

/**
 * corant-suites-jms-artemis
 *
 * @author bingo 下午5:36:38
 *
 */
@ApplicationScoped
public class JMSContextProducer {

  @Inject
  @Any
  RequestScopeContextManager requestScopeContextManager;

  @Inject
  @Any
  TransactionScopeContextManager transactionScopeContextManager;

  public JMSContext create(final String connectionFactoryId, final int sessionMode) {
    return new ExtendedJMSContext(new JMSContextKey(connectionFactoryId, sessionMode),
        getRequestScopeContextManager(), getTransactionScopeContextManager());
  }

  /**
   *
   * @return the requestScopeContextManager
   */
  public RequestScopeContextManager getRequestScopeContextManager() {
    return requestScopeContextManager;
  }

  /**
   *
   * @return the transactionScopeContextManager
   */
  public TransactionScopeContextManager getTransactionScopeContextManager() {
    return transactionScopeContextManager;
  }

  @Produces
  JMSContext produce(final InjectionPoint ip) {
    return new ExtendedJMSContext(JMSContextKey.of(ip), getRequestScopeContextManager(),
        getTransactionScopeContextManager());
  }

}