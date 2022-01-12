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
package org.corant.kernel.event;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

/**
 * corant-kernel
 *
 * @author bingo 下午12:02:58
 *
 */
public interface CorantLifecycleEvent {

  @ApplicationScoped
  class LifecycleEventEmitter {
    @Inject
    protected Event<CorantLifecycleEvent> events;

    public void fire(CorantLifecycleEvent event, boolean async) {
      if (async) {
        events.fireAsync(event);
      } else {
        events.fire(event);
      }
    }

  }
}
