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

import static org.corant.shared.util.Objects.forceCast;
import javax.persistence.EntityManager;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;
import org.corant.suites.ddd.annotation.stereotype.Events;
import org.corant.suites.ddd.model.Aggregate;
import org.corant.suites.ddd.model.Aggregate.Lifecycle;

/**
 * corant-suites-ddd
 *
 * <p>
 * Every aggregate that extends AbstractAggregate when life cycle change then will fire
 * AggregateLifecycleEvent. The Event triggers occur when the aggregate is persisted/deleted/updated
 * and has been propagated to the persistence context and the JTA transaction has not yet finished.
 *
 * @see EntityManager#flush()
 * @see TransactionSynchronizationRegistry
 * @see Synchronization#beforeCompletion()
 *
 * @author bingo 上午9:39:28
 */
@Events
public class AggregateLifecycleEvent extends AbstractEvent {

  private static final long serialVersionUID = -5079236126615952794L;

  private final Lifecycle lifecycle;

  public AggregateLifecycleEvent(Aggregate source) {
    this(source, source.getLifecycle());
  }

  public AggregateLifecycleEvent(Aggregate source, Lifecycle lifecycle) {
    super(source);
    this.lifecycle = lifecycle;
  }

  public Lifecycle getLifecycle() {
    return lifecycle;
  }

  @Override
  public Aggregate getSource() {
    return forceCast(super.getSource());
  }

  public <T extends Aggregate> T getSourceAggregate() {
    return forceCast(super.getSource());
  }
}
