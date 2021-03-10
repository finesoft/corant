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
package org.corant.context.concurrent.executor;

import java.util.Map;
import org.glassfish.enterprise.concurrent.ContextServiceImpl;
import org.glassfish.enterprise.concurrent.spi.ContextSetupProvider;
import org.glassfish.enterprise.concurrent.spi.TransactionSetupProvider;

/**
 * corant-suites-concurrency
 *
 * @author bingo 上午10:18:55
 *
 */
public class DefaultContextService extends ContextServiceImpl {

  private static final long serialVersionUID = -2380561596716132958L;

  /**
   * @param name
   * @param contextSetupProvider
   * @param transactionSetupProvider
   */
  public DefaultContextService(String name, ContextSetupProvider contextSetupProvider,
      TransactionSetupProvider transactionSetupProvider) {
    super(name, contextSetupProvider, transactionSetupProvider);
  }

  @Override
  public Object createContextualProxy(Object instance, Map<String, String> executionProperties,
      Class<?>... interfaces) {
    return super.createContextualProxy(instance, executionProperties, interfaces);
  }

  @Override
  public <T> T createContextualProxy(T instance, Map<String, String> executionProperties,
      Class<T> intf) {
    return super.createContextualProxy(instance, executionProperties, intf);
  }

}
