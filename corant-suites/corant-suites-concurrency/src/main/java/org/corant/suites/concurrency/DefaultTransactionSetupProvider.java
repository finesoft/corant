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
package org.corant.suites.concurrency;

import org.glassfish.enterprise.concurrent.spi.TransactionHandle;
import org.glassfish.enterprise.concurrent.spi.TransactionSetupProvider;

/**
 * corant-suites-concurrency
 *
 * @author bingo 上午10:26:12
 *
 */
public class DefaultTransactionSetupProvider implements TransactionSetupProvider {

  private static final long serialVersionUID = 1194633142409438228L;

  @Override
  public void afterProxyMethod(TransactionHandle handle, String transactionExecutionProperty) {
    // TODO Auto-generated method stub

  }

  @Override
  public TransactionHandle beforeProxyMethod(String transactionExecutionProperty) {
    // TODO Auto-generated method stub
    return null;
  }

}
