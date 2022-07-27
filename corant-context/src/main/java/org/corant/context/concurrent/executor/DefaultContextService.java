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

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import org.glassfish.enterprise.concurrent.ContextServiceImpl;
import org.glassfish.enterprise.concurrent.spi.ContextSetupProvider;
import org.glassfish.enterprise.concurrent.spi.TransactionSetupProvider;

/**
 * corant-context
 *
 * @author bingo 上午10:18:55
 *
 */
public class DefaultContextService extends ContextServiceImpl {

  private static final long serialVersionUID = -2380561596716132958L;

  /**
   * Create a context service with given context setup provider
   *
   * @param name use for CDI Named qualifier or JNDI lookup
   * @param contextSetupProvider a provider for setting up proper executioncontext before running a
   *        task, and also for resetting the execution contextafter running a task.
   *
   * @see DefaultContextService#DefaultContextService(String, ContextSetupProvider,
   *      TransactionSetupProvider)
   */
  public DefaultContextService(String name, ContextSetupProvider contextSetupProvider) {
    super(name, contextSetupProvider);
  }

  /**
   * Create a context service with given context setup provider and transaction setup provider
   *
   * @param name use for CDI Named qualifier or JNDI lookup
   * @param contextSetupProvider a provider for setting up proper executioncontext before running a
   *        task, and also for resetting the execution contextafter running a task.
   * @param transactionSetupProvider a provider for performing proper transactionsetup before
   *        invoking a proxy method of a contextual proxy object
   * 
   * @see ContextSetupProvider
   * @see TransactionSetupProvider
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

  @SuppressWarnings("unchecked")
  public <T> Callable<T> wrapContextualCallable(Callable<T> original,
      Map<String, String> executionProperties) {
    shouldNotNull(original, "The original callable can't null!");
    return createContextualProxy(original::call, executionProperties, Callable.class);
  }

  public Runnable wrapContextualRunnable(Runnable original,
      Map<String, String> executionProperties) {
    shouldNotNull(original, "The original runnable can't null!");
    return createContextualProxy(original::run, executionProperties, Runnable.class);
  }

  @SuppressWarnings("unchecked")
  public <T> Supplier<T> wrapContextualSupplier(Supplier<T> original,
      Map<String, String> executionProperties) {
    shouldNotNull(original, "The original supplier can't null!");
    return createContextualProxy(original::get, executionProperties, Supplier.class);
  }

}
