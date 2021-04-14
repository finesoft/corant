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
package org.corant.context.concurrent.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import org.corant.context.ContainerEvents.PreContainerStopEvent;

/**
 * corant-context
 *
 * @author bingo 下午8:33:48
 *
 */
@ApplicationScoped
public class ExecutorServiceManager {

  protected static final Logger logger = Logger.getLogger(ExecutorServiceManager.class.getName());

  protected static final List<DefaultManagedExecutorService> managedExecutorService = new ArrayList<>();
  protected static final List<DefaultManagedScheduledExecutorService> managedSecheduledExecutorService =
      new ArrayList<>();

  public void register(DefaultManagedExecutorService service) {
    managedExecutorService.add(service);
  }

  public void register(DefaultManagedScheduledExecutorService service) {
    managedSecheduledExecutorService.add(service);
  }

  protected void beforeShutdown(@Observes final PreContainerStopEvent event) {
    for (DefaultManagedExecutorService service : managedExecutorService) {
      logger.info(() -> String.format("The managed executor service %s will be shutdown!",
          service.getName()));
      service.stop();
    }
    for (DefaultManagedScheduledExecutorService service : managedSecheduledExecutorService) {
      logger.info(() -> String.format("The managed scheduled executor service %s will be shutdown!",
          service.getName()));
      service.stop();
    }
    managedExecutorService.clear();
    managedSecheduledExecutorService.clear();
  }

}
