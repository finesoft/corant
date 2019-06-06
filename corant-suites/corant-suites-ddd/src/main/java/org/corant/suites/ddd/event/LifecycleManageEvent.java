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
package org.corant.suites.ddd.event;

import static org.corant.shared.util.ObjectUtils.forceCast;
import org.corant.suites.ddd.annotation.stereotype.Events;
import org.corant.suites.ddd.model.Aggregate;

/**
 * The lifecycle manage event, the infrastructure service will listen this event and do persist or
 * destroy the aggregate.
 *
 * @author bingo 上午9:39:28
 */
@Events
public class LifecycleManageEvent extends AbstractEvent {

  private static final long serialVersionUID = 2441297731044122118L;

  private final boolean destroy;

  private final boolean effectImmediately;

  public LifecycleManageEvent(Aggregate source, boolean destroy) {
    this(source, destroy, false);
  }

  public LifecycleManageEvent(Aggregate source, boolean destroy, boolean effectImmediately) {
    super(source);
    this.destroy = destroy;
    this.effectImmediately = effectImmediately;
  }

  @Override
  public Aggregate getSource() {
    return forceCast(super.getSource());
  }

  public boolean isDestroy() {
    return destroy;
  }

  public boolean isEffectImmediately() {
    return effectImmediately;
  }

  @Override
  public String toString() {
    return "LifecycleEvent [destroy=" + destroy + ", effectImmediately=" + effectImmediately + "]";
  }

}
