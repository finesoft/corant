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
package org.corant.context;

import java.util.logging.Logger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.weld.environment.se.events.ContainerBeforeShutdown;
import org.jboss.weld.environment.se.events.ContainerInitialized;

/**
 * corant-context
 *
 * @author bingo 下午2:00:05
 *
 */
@ApplicationScoped
public class ContainerEvents {

  protected static final Logger logger = Logger.getLogger(ContainerEvents.class.getName());

  @Inject
  protected Event<ContainerEvent> events;

  protected void onContainerBeforeShutdown(@Observes ContainerBeforeShutdown e) {
    events.fire(new PreContainerStopEvent(e.getContainerId()));
  }

  protected void onContainerInitialized(@Observes ContainerInitialized e) {
    events.fire(new PostContainerStartedEvent(e.getContainerId()));
    events.fireAsync(new PostContainerStartedAsyncEvent(e.getContainerId()));
  }

  public interface ContainerEvent {
  }

  public static class PostContainerStartedAsyncEvent implements ContainerEvent {

    public final String containerId;

    public PostContainerStartedAsyncEvent(String containerId) {
      this.containerId = containerId;
    }

  }

  public static class PostContainerStartedEvent implements ContainerEvent {

    public final String containerId;

    public PostContainerStartedEvent(String containerId) {
      this.containerId = containerId;
    }

  }

  public static class PreContainerStopEvent implements ContainerEvent {

    public final String containerId;

    public PreContainerStopEvent(String containerId) {
      this.containerId = containerId;
    }

  }

}
