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
package org.corant.suites.vertx.web;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import org.eclipse.microprofile.config.Config;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * corant-suites-vertx-web
 *
 * @author bingo 下午12:13:35
 *
 */
public class WebServerInitializer {

  HttpServer httpServer;

  void init(@Observes Vertx vertx, BeanManager beanManager, Config config) {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    httpServer = vertx.createHttpServer().requestHandler(router).listen();
  }
}
