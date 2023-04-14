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

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import io.vertx.core.Vertx;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

/**
 * corant-modules-vertx-shared
 *
 * <p>
 * <b> NOTE: The code in this class comes from the Weld-Vertx project, if there is any infringement,
 * please inform me(finesoft@gmail.com) </b>
 * <p>
 * Allows to wrap a synchronous action as an asynchronous computation. The action is performed
 * either as blocking operation using a Vertx worker thread or as non-blocking operation using the
 * Vertx event-loop thread.
 *
 * <pre>
 * &#64;ApplicationScoped
 * class Hello {
 *
 *     &#64;Inject
 *     AsyncWorker worker;
 *
 *     &#64;Inject
 *     Service service;
 *
 *     CompletionStage&lt;String&gt; hello() {
 *         return worker.performBlocking(service::getNameFromFile()).thenApply((name) -> "Hello " + name + "!");
 *     }
 *
 * }
 * </pre>
 *
 * @author Martin Kouba
 */
@Dependent
public class VertxAsynchronousWorker {

  private final Vertx vertx;

  @Inject
  public VertxAsynchronousWorker(Vertx vertx) {
    this.vertx = vertx;
  }

  /**
   * Returns a non-contextual instance.
   *
   * @param vertx the vertx for performing
   * @return a new worker instance
   */
  public static VertxAsynchronousWorker from(Vertx vertx) {
    return new VertxAsynchronousWorker(vertx);
  }

  /**
   * Performs the specified action using the event-loop thread. The action should never block.
   *
   * @param action the specified action
   * @return a completion stage with the result of the specified action
   */
  public <V> CompletionStage<V> perform(Callable<V> action) {
    VertxCompletableFuture<V> future = new VertxCompletableFuture<>(vertx);
    vertx.runOnContext(v -> {
      try {
        future.complete(action.call());
      } catch (Throwable e) {
        future.completeExceptionally(e);
      }
    });
    return future;
  }

  /**
   * Performs the specified action using a thread from the worker pool.
   *
   * @param action the specified action
   * @return a completion stage with the result of the specified action
   * @see Vertx#executeBlocking(io.vertx.core.Handler, io.vertx.core.Handler)
   */
  public <V> CompletionStage<V> performBlocking(Callable<V> action) {
    VertxCompletableFuture<V> future = new VertxCompletableFuture<>(vertx);
    vertx.<V>executeBlocking((f -> {
      try {
        f.complete(action.call());
      } catch (Exception e) {
        f.fail(e);
      }
    }), false, r -> {
      if (r.succeeded()) {
        future.complete(r.result());
      } else {
        future.completeExceptionally(r.cause());
      }
    });
    return future;
  }

}
