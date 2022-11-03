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
package org.corant.modules.webserver.jetty;

import static org.corant.shared.util.Classes.getUserClass;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import org.corant.modules.webserver.shared.AbstractWebServer;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.StopWatch;
import org.eclipse.jetty.cdi.CdiServletContainerInitializer;
import org.eclipse.jetty.cdi.CdiSpiDecorator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;

/**
 * corant-modules-webserver-jetty
 *
 * @author bingo 下午2:24:42
 *
 */
@ApplicationScoped
public class JettyWebServer extends AbstractWebServer {

  @Inject
  protected Logger logger;

  @Inject
  @Any
  protected Instance<JettyWebServerThreadPoolProvider> threadPoolProviders;

  protected Server server;

  @Override
  public void start() {
    try {
      StopWatch sw = StopWatch.press(getUserClass(getClass()).getSimpleName());
      server = resolveServer1();
      if (getPreStartHandlers().map(h -> h.onPreStart(this)).reduce(Boolean::logicalAnd)
          .orElse(Boolean.TRUE)) {
        server.start();
        getPostStartedHandlers().forEach(h -> h.onPostStarted(this));
        sw.destroy(t -> logger.info(
            () -> String.format("%s [%s] was started, takes %ss.", t.getLastTaskInfo().getName(),
                config.getDescription(), t.getLastTaskInfo().getTimeSeconds())));
        // server.join();
      } else {
        logger.info(() -> "Jetty can not start, due to some PreStartHandler interruption!");
      }
    } catch (Exception e) {
      throw new CorantRuntimeException(e, "Unable to launch jetty [%s]", config.getDescription());
    }
  }

  @Override
  public void stop() {
    if (server != null) {
      try {
        getPreStopHandlers().forEach(h -> h.onPreStop(this));
        server.stop();
        getPostStoppedHandlers().forEach(h -> h.onPostStopped(this));
      } catch (Exception e) {
        throw new CorantRuntimeException(e, "Unable to stop jetty ");
      }
    }
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    if (Server.class.isAssignableFrom(cls)) {
      return cls.cast(server);
    } else {
      throw new CorantRuntimeException("Jetty server can not unwrap %s ", cls);
    }
  }

  protected void resolveFilter(ServletContextHandler handler) {
    // TODO FIXME
  }

  protected void resolveListener(ServletContextHandler handler) {
    // TODO FIXME
  }

  protected synchronized Server resolveServer1() throws Exception {
    server = new Server(threadPoolProviders.isResolvable() ? threadPoolProviders.get().apply(config)
        : JettyWebServerThreadPoolProvider.DEFAULT.apply(config));

    // The HTTP configuration object.
    // HttpConfiguration httpConfig = new HttpConfiguration();
    // The ConnectionFactory for HTTP/1.1.
    // HttpConnectionFactory http11 = new HttpConnectionFactory(httpConfig);
    // The ConnectionFactory for clear-text HTTP/2.
    // HTTP2CServerConnectionFactory h2c = new HTTP2CServerConnectionFactory(httpConfig);

    ServerConnector connector = new ServerConnector(server);
    connector.setHost(config.getHost());
    connector.setPort(config.getPort());
    server.setConnectors(new Connector[] {connector});

    ServletContextHandler context = new ServletContextHandler();
    context.setContextPath(config.getContextPath());
    config.getDisplayName().ifPresent(context::setDisplayName);
    context.setClassLoader(getClass().getClassLoader());
    // context.setBaseResource(Resource.newResource(webRootUri));

    // Enable Weld + CDI
    context.setClassLoader(getClass().getClassLoader());
    context.setInitParameter(CdiServletContainerInitializer.CDI_INTEGRATION_ATTRIBUTE,
        CdiSpiDecorator.MODE);
    context.addServletContainerInitializer(new CdiServletContainerInitializer());
    // context.addServletContainerInitializer(new EnhancedListener());

    // Add to Server
    server.setHandler(context);
    return server;
  }

  protected void resolveServlet(ServletContextHandler handler) {
    // TODO FIXME
  }

  protected void resolveStaticContent(ServletContextHandler handler) {
    // TODO FIXME
  }

  protected void resolveWebSocket(ServletContextHandler handler) {
    // TODO FIXME
  }
}
