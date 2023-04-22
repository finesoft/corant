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
package org.corant.modules.vertx.jaxrs;

import static org.corant.context.Beans.resolve;
import static org.corant.shared.util.Objects.defaultObject;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jakarta.ws.rs.ApplicationPath;
import org.corant.config.Configs;
import org.corant.modules.jaxrs.shared.JaxrsExtension;
import org.corant.modules.vertx.shared.CorantVerticle;
import org.corant.shared.util.Classes;
import org.jboss.resteasy.plugins.server.embedded.SecurityDomain;
import org.jboss.resteasy.plugins.server.vertx.VertxRequestHandler;
import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyDeployment;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;

/**
 * corant-modules-vertx-jaxrs
 *
 * @author bingo 上午10:51:39
 *
 */
public class ResteasyJaxrsVerticle extends AbstractVerticle {

  protected volatile HttpServerOptions serverOptions;
  protected volatile SecurityDomain securityDomain;

  public static void main(String... args) {
    Vertx.vertx().deployVerticle(new ResteasyJaxrsVerticle());
  }

  public ResteasyJaxrsVerticle securityDomain(SecurityDomain securityDomain) {
    this.securityDomain = securityDomain;
    return this;
  }

  public ResteasyJaxrsVerticle serverOptions(HttpServerOptions serverOptions) {
    this.serverOptions = serverOptions;
    return this;
  }

  @Override
  public void start() throws Exception {
    CorantVerticle verticle = new CorantVerticle();
    vertx.deployVerticle(verticle, r -> {
      if (r.succeeded()) {
        JaxrsExtension jaxrs = resolve(JaxrsExtension.class);
        VertxResteasyDeployment deployment = new VertxResteasyDeployment();
        deployment.setInjectorFactoryClass("org.jboss.resteasy.cdi.CdiInjectorFactory");
        // register providers
        deployment.setScannedProviderClasses(jaxrs.getProviders().stream()
            .map(Classes::getUserClass).map(Class::getName).collect(Collectors.toList()));
        // register resources
        deployment.setScannedResourceClasses(jaxrs.getResources().stream()
            .map(Classes::getUserClass).map(Class::getName).collect(Collectors.toList()));
        deployment.start();

        vertx.createHttpServer(defaultObject(serverOptions, HttpServerOptions::new))
            .requestHandler(new VertxRequestHandler(vertx, deployment,
                resolveAppDeployment(deployment).orElse(""), securityDomain))
            .listen(Configs.getValue("corant.vertx.jaxrs.server.port", Integer.class, 8080), ar -> {
              if (ar.succeeded()) {
                Logger.getLogger(ResteasyJaxrsVerticle.class.getCanonicalName())
                    .info("Server started on port " + ar.result().actualPort());
              } else {
                Logger.getLogger(ResteasyJaxrsVerticle.class.getCanonicalName()).log(Level.SEVERE,
                    "Can't start Server!", ar.cause());
              }
            });
      } else {
        Logger.getLogger(ResteasyJaxrsVerticle.class.getCanonicalName()).log(Level.SEVERE,
            "Can't deploy resteasy verticle!", r.cause());
      }
    });
  }

  protected Optional<String> resolveAppDeployment(final ResteasyDeployment deployment) {
    ApplicationPath appPath = null;
    if (deployment.getApplicationClass() != null) {
      try {
        Class<?> clazz = Class.forName(deployment.getApplicationClass());
        appPath = clazz.getAnnotation(ApplicationPath.class);

      } catch (ClassNotFoundException e) {
        // todo how to handle
      }
    } else if (deployment.getApplication() != null) {
      appPath = deployment.getApplication().getClass().getAnnotation(ApplicationPath.class);
    }
    String aPath = null;
    if (appPath != null) {
      aPath = appPath.value();
    }
    return Optional.ofNullable(aPath);
  }
}
