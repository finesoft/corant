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

import java.util.function.Consumer;
import jakarta.enterprise.inject.se.SeContainerInitializer;
import org.corant.Corant;
import io.vertx.core.Vertx;

/**
 * corant-modules-vertx-shared
 *
 * @author bingo 下午3:46:50
 */
public class Verticles {

  public static void single(Consumer<SeContainerInitializer> initializer, String... arguments) {
    Vertx.vertx().deployVerticle(new CorantVerticle(initializer, arguments));
  }

  @SuppressWarnings("resource")
  public static Vertx vertx(Consumer<SeContainerInitializer> initializer, String... arguments) {
    final Vertx vertx = Vertx.vertx();
    new Corant(arguments).start(sc -> {
      sc.addExtensions(new VertxExtension(vertx, vertx.getOrCreateContext()));
      if (initializer != null) {
        initializer.accept(sc);
      }
    });
    return vertx;
  }
}
