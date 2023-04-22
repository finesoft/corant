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

import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.EMPTY_ARRAY;
import java.util.Arrays;
import java.util.function.Consumer;
import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import org.corant.Corant;
import org.corant.shared.util.Functions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

/**
 * corant-modules-vertx-shared
 *
 * @author bingo 上午11:01:22
 *
 */
@Vetoed
public class CorantVerticle extends AbstractVerticle {

  private final Consumer<SeContainerInitializer> initializer;
  private final String[] arguments;

  public CorantVerticle(Consumer<SeContainerInitializer> initializer, String... arguments) {
    this.arguments = arguments == null ? EMPTY_ARRAY : Arrays.copyOf(arguments, arguments.length);
    this.initializer = defaultObject(initializer, Functions::emptyConsumer);
  }

  public CorantVerticle(String... arguments) {
    this(null, arguments);
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    vertx.executeBlocking(promise -> {
      try {
        Corant.startup(sc -> {
          initializer.accept(sc);
          sc.addExtensions(new VertxExtension(vertx, context));
        }, arguments);
        postCorantStarted(null);
        promise.complete();
      } catch (Exception e) {
        postCorantStarted(e);
        promise.fail(e);
      }
    }, result -> {
      if (result.succeeded()) {
        startPromise.complete();
      } else {
        startPromise.fail(result.cause());
      }
    });
  }

  @Override
  public void stop(Promise<Void> stopPromise) throws Exception {
    if (Corant.current() != null) {
      vertx.executeBlocking(promise -> {
        try {
          preCorantStop();
          Corant.shutdown();
          promise.complete();
        } catch (Exception e) {
          promise.fail(e);
        }
      }, result -> {
        if (result.succeeded()) {
          stopPromise.complete();
        } else {
          stopPromise.fail(result.cause());
        }
      });
    } else {
      stopPromise.complete();
    }
  }

  protected void postCorantStarted(Exception e) {

  }

  protected void preCorantStop() {

  }
}
