/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.suites.webserver.shared;

import static org.corant.shared.util.CollectionUtils.isEmpty;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import org.corant.kernel.event.PostContainerStartedEvent;
import org.corant.kernel.event.PostCorantReadyEvent;
import org.corant.shared.exception.CorantRuntimeException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-suites-webserver-shared
 *
 * @author bingo 下午3:13:36
 *
 */
@ApplicationScoped
public class WebServerBootstrapper {

  @Inject
  Logger logger;

  @Inject
  BeanManager beanManager;

  @Inject
  WebServerExtension extension;

  @Inject
  @ConfigProperty(name = "webserver.auto-start", defaultValue = "true")
  Boolean autoStart;

  WebServer server;

  @PostConstruct
  protected void onPostConstruct() {
    Set<Bean<?>> beans = beanManager.getBeans(WebServer.class);
    if (isEmpty(beans)) {
      throw new CorantRuntimeException("Can not find web server!");
    } else if (beans.size() > 1) {
      throw new CorantRuntimeException("Only one web server is allowed!");
    }
    Bean<?> bean = beanManager.resolve(beans);
    CreationalContext<?> creationalContext = beanManager.createCreationalContext(bean);
    server = (WebServer) beanManager.getReference(bean, WebServer.class, creationalContext);
  }

  protected void onPostContainerStartedEvent(@Observes PostContainerStartedEvent event) {
    if (autoStart) {
      if (server == null) {
        throw new CorantRuntimeException("Web server not initialized yet!");
      }
      logger.info(() -> String.format("Start web server %s ", server));
      server.start();
      beanManager.fireEvent(new PostCorantReadyEvent());
    }
  }

  @PreDestroy
  protected void onPreDestroy() {
    if (server != null) {
      logger.info(() -> String.format("Stop web server %s ", server));
      server.stop();
    }
  }
}
