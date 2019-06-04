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
package org.corant.suites.ddd.model;

import static org.corant.shared.util.ClassUtils.tryAsClass;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.List;
import org.corant.suites.ddd.event.Event;
import org.corant.suites.ddd.message.Message;

/**
 * @author bingo 下午4:23:02
 * @since
 */
public interface Aggregate extends Entity {

  /**
   * If flush is true then the integration event queue will be clear
   */
  List<Message> extractMessages(boolean flush);

  /**
   * the lifeCycle of being
   */
  Lifecycle getLifecycle();

  /**
   * the evolution version number, use as persistence version
   */
  Long getVn();

  /**
   * In this case, it means whether it is persisted or not
   */
  default boolean isEnabled() {
    return getLifecycle() == Lifecycle.ENABLED;
  }

  /**
   * The aggregate isn't persisted, or is destroyed, but still live in memory until the GC recycle.
   */
  Boolean isPhantom();

  /**
   * Raise events.
   */
  void raise(Event event, Annotation... qualifiers);

  /**
   * Raise messages
   */
  void raise(Message... messages);

  /**
   * Raise asynchronous events
   */
  void raiseAsync(Event event, Annotation... qualifiers);

  interface AggregateIdentifier extends EntityIdentifier {

    @Override
    Serializable getId();

    @Override
    String getType();

    default Class<?> getTypeCls() {
      return tryAsClass(getType());
    }
  }

  interface AggregateReference<T extends Aggregate> extends EntityReference<T> {

    Long getVn();

  }

  @FunctionalInterface
  interface Destroyable<P, T> {
    void destroy(P param, DestroyHandler<P, T> handler);
  }

  @FunctionalInterface
  interface DestroyHandler<P, T> {
    void preDestroy(P param, T destroyable);
  }

  @FunctionalInterface
  interface Enabling<P, T> {
    T enable(P param, EnablingHandler<P, T> handler);
  }

  @FunctionalInterface
  interface EnablingHandler<P, T> {
    void preEnable(P param, T enabling);
  }

  public enum Lifecycle {
    INITIAL, ENABLED, DESTROYED
  }

  public enum LifecyclePhase {
    ENABLED, DESTROYED
  }
}
