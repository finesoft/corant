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
public interface VertxMessage {

  /**
   * Publish a message.
   * <p>
   * The message will be delivered to all handlers registered to the address.
   *
   * @param message the message to be published
   *
   * @see EventBus#publish(String, Object)
   * @see EventBus#publish(String, Object, DeliveryOptions)
   */
  void publish(Object message);

  /**
   * Sends a message and specify a {@code replyHandler} that will be called if the recipient
   * subsequently replies to the message.
   * <p>
   * The message will be delivered to at most one of the handlers registered to the address.
   *
   * @param message the message to be sent
   * @param replyHandler reply handler will be called when any reply from the recipient is received
   *
   * @see EventBus#request(String, Object, Handler)
   * @see EventBus#request(String, Object, DeliveryOptions, Handler)
   */
  void request(Object message, Handler<AsyncResult<Message<Object>>> replyHandler);

  /**
   * Sends a message.
   * <p>
   * The message will be delivered to at most one of the handlers registered to the address.
   *
   * @param message the message to sent, may be null
   *
   * @see EventBus#send(String, Object)
   * @see EventBus#send(String, Object, DeliveryOptions)
   */
  void send(Object message);

  /**
   * Specifying options that can be used to configure the message delivery.
   *
   * @param deliveryOptions delivery options
   * @return a reference to this, so the API can be used fluently
   */
  VertxMessage setDeliveryOptions(DeliveryOptions deliveryOptions);
}
