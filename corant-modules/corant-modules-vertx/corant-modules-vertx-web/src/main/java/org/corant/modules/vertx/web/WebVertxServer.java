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
package org.corant.modules.vertx.web;

import javax.enterprise.event.Observes;
import javax.inject.Singleton;
import org.corant.Corant;
import org.corant.context.ContainerEvents.PreContainerStopEvent;
import org.corant.kernel.event.PostCorantReadyEvent;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;

/**
 * corant-modules-vertx-web
 *
 * @author bingo 下午3:54:43
 *
 */
@Singleton
public class WebVertxServer {

  Vertx vertx = Vertx.vertx();
  Verticle verticle = new WebVerticle();

  public static void main(String... args) {
    Corant.startup();
  }

  void onPostCorantReady(@Observes PostCorantReadyEvent e) {
    vertx.deployVerticle(verticle);
  }

  void onPreContainerStopEvent(@Observes PreContainerStopEvent event) {
    vertx.close();
  }
}
