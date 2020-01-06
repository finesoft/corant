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
package org.corant.suites.rsocket.server;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.corant.kernel.event.PostContainerStartedEvent;
import org.corant.shared.exception.CorantRuntimeException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.server.TcpServerTransport;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

/**
 * corant-suites-rsocket
 *
 * @author bingo 下午6:36:03
 *
 */
@ApplicationScoped
public class RSocketServer {

  private AtomicReference<Disposable> server = new AtomicReference<>();

  @Inject
  Logger logger;

  @Inject
  @ConfigProperty(name = "rsocket.server.auto-start", defaultValue = "true")
  boolean autoStart;

  @Inject
  @ConfigProperty(name = "rsocket.server.host", defaultValue = "0.0.0.0")
  String host;

  @Inject
  @ConfigProperty(name = "rsocket.server.port", defaultValue = "9090")
  int port;

  public synchronized void start() {
    if (server.get() != null) {
      throw new CorantRuntimeException("The RSocket server [%s:%s] was started!", host, port);
    }
    server.set(RSocketFactory.receive()
        .acceptor((setupPayload, reactiveSocket) -> Mono.just(new RSocketImpl()))
        .transport(TcpServerTransport.create(host, port)).start().subscribe());
  }

  public synchronized void stop() {
    if (server.get() != null) {
      server.get().dispose();
      server.set(null);
    }
  }

  synchronized void onPostContainerStartedEvent(@Observes PostContainerStartedEvent event) {
    if (autoStart) {
      start();
      logger.info(() -> String.format("Start RSocket server [%s:%s]", host, port));
    }
  }

  @PreDestroy
  synchronized void onPreDestroy() {
    stop();
    logger.info(() -> String.format("Stop RSocket server %s [%s:%s]", host, port));
  }
}
