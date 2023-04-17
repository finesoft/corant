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
package org.corant.modules.vertx.shared;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;

/**
 * <b> NOTE: The code in this class comes from the Weld-Vertx project, since the Weld-Vertx project
 * is no longer updated, we have partially modified the source code to update it synchronously with
 * the dependent library. If there is any infringement, please inform me(finesoft@gmail.com). </b>
 */
class VertxMessageImpl implements VertxMessage {

  private final String address;

  private final EventBus eventBus;

  private DeliveryOptions deliveryOptions;

  VertxMessageImpl(String address, EventBus eventBus) {
    this.address = address;
    this.eventBus = eventBus;
  }

  @Override
  public void publish(Object message) {
    if (deliveryOptions != null) {
      eventBus.publish(address, message, deliveryOptions);
    } else {
      eventBus.publish(address, message);
    }
  }

  @Override
  public void request(Object message, Handler<AsyncResult<Message<Object>>> replyHandler) {
    if (deliveryOptions != null) {
      eventBus.request(address, message, deliveryOptions, replyHandler);
    } else {
      eventBus.request(address, message, replyHandler);
    }
  }

  @Override
  public void send(Object message) {
    if (deliveryOptions != null) {
      eventBus.send(address, message, deliveryOptions);
    } else {
      eventBus.send(address, message);
    }
  }

  @Override
  public VertxMessage setDeliveryOptions(DeliveryOptions deliveryOptions) {
    this.deliveryOptions = deliveryOptions;
    return this;
  }

}
