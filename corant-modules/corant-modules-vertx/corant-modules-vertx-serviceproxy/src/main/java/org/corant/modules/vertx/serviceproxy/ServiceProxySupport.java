/*
 * JBoss, Home of Professional Open Source Copyright 2016, Red Hat, Inc., and individual
 * contributors by the @authors tag. See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.corant.modules.vertx.serviceproxy;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;

/**
 * <p>
 * <b> NOTE: The code in this class comes from the Weld-Vertx project, since the Weld-Vertx project
 * is no longer updated, we have partially modified the source code to update it synchronously with
 * the dependent library. If there is any infringement, please inform me(finesoft@gmail.com). </b>
 *
 * @author Martin Kouba
 */
public interface ServiceProxySupport {

  /**
   * @param serviceInterface the service interface
   * @return the delivery options used for a particular service proxy bean instance or
   *         <code>null</code>
   */
  default DeliveryOptions getDefaultDeliveryOptions(Class<?> serviceInterface) {
    return null;
  }

  /**
   * By default, the service result handler is executed as blocking code.
   *
   * @return the executor used to execute a service result handler
   * @see Vertx#executeBlocking(Callable, boolean, Handler)
   */
  default Executor getExecutor() {
    return command -> getVertx().executeBlocking(() -> {
      command.run();
      return null;
    }, false, null);
  }

  /**
   * @return the vertx instance
   * @see #getExecutor()
   */
  Vertx getVertx();

  @ApplicationScoped
  class DefaultServiceProxySupport implements ServiceProxySupport {

    @Inject
    private Vertx vertx;

    @Override
    public Vertx getVertx() {
      return vertx;
    }

  }

}
