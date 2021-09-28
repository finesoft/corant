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
package org.corant.modules.vertx.web;

import org.corant.Corant;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

/**
 * corant-modules-vertx-web
 *
 * @author bingo 下午12:13:35
 *
 */
public class WebVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startFuture) throws Exception {
    vertx.executeBlocking(future -> {
      try {
        // TODO FIXME
        future.complete();
      } catch (Exception e) {
        future.fail(e);
      }
    }, result -> {
      if (result.succeeded()) {
        startFuture.complete();
      } else {
        startFuture.fail(result.cause());
      }
    });
  }

  @Override
  public void stop(Promise<Void> stopFuture) throws Exception {
    if (Corant.current() != null && Corant.current().isRunning()) {
      vertx.executeBlocking(future -> {
        try {
          // TODO FIXME
          future.complete();
        } catch (Exception e) {
          future.fail(e);
        }
      }, result -> {
        if (result.succeeded()) {
          stopFuture.complete();
        } else {
          stopFuture.fail(result.cause());
        }
      });
    } else {
      stopFuture.complete();
    }
  }

}
