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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Function;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.serviceproxy.ServiceProxyBuilder;

/**
 * <p>
 * <b> NOTE: The code in this class comes from the Weld-Vertx project, since the Weld-Vertx project
 * is no longer updated, we have partially modified the source code to update it synchronously with
 * the dependent library. If there is any infringement, please inform me(finesoft@gmail.com). </b>
 * <p>
 * This invocation handler delegates to a service proxy instance.
 * <p>
 * The result handler is wrapped and executed using {@link ServiceProxySupport#getExecutor()}.
 *
 * @author Martin Kouba
 */
public class ServiceProxyInvocationHandler implements InvocationHandler {

  private static final Function<Method, Integer> HANDLER_POSITION_FUNCTION = m -> {
    for (int i = 0; i < m.getGenericParameterTypes().length; i++) {
      Type paramType = m.getGenericParameterTypes()[i];
      if ((paramType instanceof ParameterizedType handlerType)
          && (handlerType.getRawType().equals(Handler.class)
              && handlerType.getActualTypeArguments()[0] instanceof ParameterizedType eventType)) {
        if (eventType.getRawType().equals(AsyncResult.class)) {
          return i;
        }
      }
    }
    return Integer.MIN_VALUE;
  };

  private final Executor executor;

  private final Object delegate;

  private final Map<Method, Integer> handlerParamPositionCache;

  ServiceProxyInvocationHandler(ServiceProxySupport serviceProxySupport, Class<?> serviceInterface,
      String address) {
    executor = serviceProxySupport.getExecutor();
    DeliveryOptions deliveryOptions =
        serviceProxySupport.getDefaultDeliveryOptions(serviceInterface);
    ServiceProxyBuilder builder =
        new ServiceProxyBuilder(serviceProxySupport.getVertx()).setAddress(address);
    if (deliveryOptions != null) {
      builder.setOptions(deliveryOptions);
    }
    delegate = builder.build(serviceInterface);
    handlerParamPositionCache = new ConcurrentHashMap<>();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    int handlerParamPosition =
        handlerParamPositionCache.computeIfAbsent(method, HANDLER_POSITION_FUNCTION);
    if (handlerParamPosition > 0) {
      final Handler<AsyncResult<?>> handler = (Handler<AsyncResult<?>>) args[handlerParamPosition];
      args[handlerParamPosition] =
          (Handler<AsyncResult<Object>>) event -> executor.execute(() -> handler.handle(event));
    }
    return method.invoke(delegate, args);
  }

}
