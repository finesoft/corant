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
package org.corant.asosat.ddd.event;

import org.corant.asosat.ddd.annotation.stereotype.Events;
import org.corant.asosat.ddd.model.Aggregate;
import org.corant.asosat.ddd.model.Aggregate.LifcyclePhase;

/**
 * Every aggregate that extends AbstractAggregate when life cycle change then will fire
 * LifecycleEvent, the infrastructure service will listen this event and do persist or remove the
 * aggregate.
 *
 * @author bingo 上午9:39:28
 */
@Events
public class LifecycleEvent extends AbstractEvent {

  private static final long serialVersionUID = -5079236126615952794L;

  private final LifcyclePhase phase;

  private final boolean effectImmediately;

  public LifecycleEvent(Aggregate source, LifcyclePhase lifcyclehase) {
    this(source, lifcyclehase, false);
  }

  public LifecycleEvent(Aggregate source, LifcyclePhase lifcyclehase, boolean effectImmediately) {
    super(source);
    phase = lifcyclehase;
    this.effectImmediately = effectImmediately;
  }

  public LifcyclePhase getPhase() {
    return phase;
  }

  public boolean isEffectImmediately() {
    return effectImmediately;
  }

  @Override
  public String toString() {
    return "LifecycleEvent [phase=" + phase + ", effectImmediately=" + effectImmediately + "]";
  }

}
