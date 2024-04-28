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
package org.corant.modules.ddd;

import static org.corant.shared.util.Objects.forceCast;
import java.util.function.Supplier;
import jakarta.persistence.LockModeType;
import org.corant.modules.ddd.AggregateLifecycleManager.LifecycleAction;
import org.corant.modules.ddd.annotation.Events;
import org.corant.shared.util.Classes;

/**
 * corant-modules-ddd-api
 *
 * <p>
 * The life cycle manage event, the infrastructure service will listen this event and do persist or
 * destroy the aggregate.
 *
 * @author bingo 上午9:39:28
 */
@Events
public class AggregateLifecycleManageEvent extends AbstractEvent {

  private static final long serialVersionUID = 2441297731044122118L;

  private final LifecycleAction action;

  private final boolean effectImmediately;

  private final LockModeType lockModeType;

  public AggregateLifecycleManageEvent(Aggregate source, LifecycleAction action) {
    this(source, action, false);
  }

  public AggregateLifecycleManageEvent(Aggregate source, LifecycleAction action,
      boolean effectImmediately) {
    this(source, action, effectImmediately, null);
  }

  public AggregateLifecycleManageEvent(Aggregate source, LifecycleAction action,
      boolean effectImmediately, LockModeType lockModeType) {
    super(source);
    this.action = action;
    this.effectImmediately = effectImmediately;
    this.lockModeType = lockModeType;
  }

  public AggregateLifecycleManageEvent(Aggregate source, LockModeType lockModeType) {
    this(source, LifecycleAction.LOCK, false, lockModeType);
  }

  public AggregateLifecycleManageEvent(Supplier<? extends Aggregate> supplier,
      LockModeType lockModeType) {
    this(supplier.get(), lockModeType);
  }

  public LifecycleAction getAction() {
    return action;
  }

  public LockModeType getLockModeType() {
    return lockModeType;
  }

  @Override
  public Aggregate getSource() {
    return forceCast(super.getSource());
  }

  public boolean isEffectImmediately() {
    return effectImmediately;
  }

  @Override
  public String toString() {
    Aggregate ai = getSource();
    String source =
        ai == null ? null : Classes.getUserClass(ai).getSimpleName() + " [id=" + ai.getId() + "]";
    return "AggregateLifecycleManageEvent [source=" + source + ", action=" + action
        + ", effectImmediately=" + effectImmediately + ", lockModeType=" + lockModeType + "]";
  }

}
