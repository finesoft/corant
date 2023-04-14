/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.vertx.shared;

import javax.enterprise.event.Event;
import javax.enterprise.inject.Vetoed;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;

/**
 * corant-modules-vertx-shared
 * <p>
 * <b> NOTE: The code in this class comes from the Weld-Vertx project, if there is any infringement,
 * please inform me(finesoft@gmail.com) </b>
 * <p>
 * An instance of this handler is registered per each address found by {@link VertxExtension}.
 *
 * @author Martin Kouba
 */
@Vetoed
public class VertxHandler implements Handler<Message<Object>> {

  private final Vertx vertx;

  private final Event<VertxEvent> event;

  private VertxHandler(Vertx vertx, Event<VertxEvent> event) {
    this.vertx = vertx;
    this.event = event;
  }

  static VertxHandler from(Vertx vertx, Event<Object> event, String address) {
    return new VertxHandler(vertx,
        event.select(VertxEvent.class, VertxConsumer.Literal.of(address)));
  }

  @Override
  public void handle(Message<Object> message) {
    // Notification is potentially a blocking code
    // The execution of the blocking code is not ordered - see
    // Vertx.executeBlocking(Handler<Promise<T>>, boolean, Handler<AsyncResult<T>>) javadoc
    vertx.executeBlocking(promise -> {
      VertxEventImpl vertxEvent = new VertxEventImpl(message, vertx.eventBus());
      try {
        // Synchronously notify all the observer methods for a specific address
        event.fire(vertxEvent);
        promise.complete(vertxEvent.getReply());
      } catch (Exception e) {
        promise.fail(e);
      }
    }, false, result -> {
      if (result.succeeded()) {
        message.reply(result.result());
      } else {
        Throwable cause = result.cause();
        if (cause instanceof RecipientFailure) {
          RecipientFailure recipientFailure = (RecipientFailure) cause;
          message.fail(recipientFailure.code, recipientFailure.getMessage());
        } else {
          message.fail(VertxEvent.OBSERVER_FAILURE_CODE, cause.getMessage());
        }
      }
    });
  }

}
