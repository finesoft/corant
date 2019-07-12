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
import org.corant.suites.ddd.model.Aggregate.AggregateIdentifier;
import org.corant.suites.ddd.model.Aggregate.Lifecycle;

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

  private final Lifecycle lifecycle;

  public LifecycleEvent(AggregateIdentifier source, Lifecycle lifecycle) {
    super(source);
    this.lifecycle = lifecycle;
  }

  public Lifecycle getLifecycle() {
    return lifecycle;
  }

  @Override
  public AggregateIdentifier getSource() {
    return forceCast(super.getSource());
  }

}
