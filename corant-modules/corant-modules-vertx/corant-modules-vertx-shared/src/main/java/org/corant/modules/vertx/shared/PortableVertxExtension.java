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

import static org.corant.shared.util.Strings.isNotBlank;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import org.corant.config.Configs;
import org.corant.context.ContainerEvents.PreContainerStopEvent;
import org.corant.shared.exception.CorantRuntimeException;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;

/**
 * corant-modules-vertx-shared
 *
 * @author bingo 下午2:31:19
 *
 */
public class PortableVertxExtension extends VertxExtension {

  public static final String OPTIONS_PATH_KEY = "corant.vertx.shared.vertx-options-path";
  public static final String CLOSE_TIME_OUT_KEY = "corant.vertx.shared.vertx-close-timeout";
  public static final String CLUSTERED_KEY = "corant.vertx.shared.use-clustered";

  public PortableVertxExtension() {
    super(resolveVertx());
  }

  static Vertx resolveVertx() {
    JsonObject configJson = null;
    String path = Configs.getValue(OPTIONS_PATH_KEY, String.class);
    boolean cluster = Configs.getValue(CLUSTERED_KEY, Boolean.class, false);
    if (isNotBlank(path)) {
      try {
        final Vertx configVertx = Vertx.vertx();
        configJson = ConfigRetriever
            .create(configVertx,
                new ConfigRetrieverOptions().addStore(new ConfigStoreOptions().setType("file")
                    .setConfig(new JsonObject().put("path", path))))
            .getConfig().toCompletionStage().whenComplete((d, t) -> configVertx.close())
            .toCompletableFuture().get();
      } catch (InterruptedException | ExecutionException e) {
        throw new CorantRuntimeException(e);
      }
    }
    if (cluster) {
      try {
        return Vertx
            .clusteredVertx(configJson == null ? new VertxOptions() : new VertxOptions(configJson))
            .toCompletionStage().toCompletableFuture().get();
      } catch (InterruptedException | ExecutionException e) {
        throw new CorantRuntimeException(e);
      }
    } else {
      return Vertx.vertx(configJson == null ? new VertxOptions() : new VertxOptions(configJson));
    }
  }

  void onPreContainerStopEvent(
      @Observes @Priority(Integer.MAX_VALUE - 5) PreContainerStopEvent event) {
    try {
      Duration timeout =
          Configs.getValue(CLOSE_TIME_OUT_KEY, Duration.class, Duration.ofSeconds(8));
      vertx.close().toCompletionStage().whenComplete((v, t) -> {
        if (t != null) {
          LOGGER.log(Level.WARNING, "Close vertx instance occurred error.", t);
        } else {
          LOGGER.info("Close vertx instance.");
        }
      }).toCompletableFuture().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      LOGGER.log(Level.WARNING, "Close vertx instance occurred error.", e);
    }
  }

}
