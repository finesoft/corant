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
package org.corant.modules.ddd.shared.event;

import static org.corant.shared.util.Objects.forceCast;
import javax.persistence.EntityManager;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;
import org.corant.modules.ddd.AbstractEvent;
import org.corant.modules.ddd.Aggregate;
import org.corant.modules.ddd.Aggregate.AggregateIdentifier;
import org.corant.modules.ddd.Aggregate.Lifecycle;
import org.corant.modules.ddd.DefaultAggregateIdentifier;
import org.corant.modules.ddd.annotation.Events;

/**
 * corant-modules-ddd-shared
 *
 * <p>
 * Every aggregate that extends AbstractAggregate when life cycle on persistence change then will
 * asynchronous fire AggregatePersistEvent . The Event triggers occur when the aggregate is
 * persisted/deleted/updated and has been updated to the persistence layer and the JTA transaction
 * has been finished successfully.
 *
 * @see EntityManager#flush()
 * @see TransactionSynchronizationRegistry
 * @see Synchronization#afterCompletion(int)
 *
 * @author bingo 上午9:39:28
 */
@Events
public class AggregatePersistEvent extends AbstractEvent {

  private static final long serialVersionUID = -5079236126615952794L;

  private final Lifecycle lifecycle;

  public AggregatePersistEvent(Aggregate source) {
    this(source, source.getLifecycle());
  }

  public AggregatePersistEvent(Aggregate source, Lifecycle lifecycle) {
    this(new DefaultAggregateIdentifier(source), lifecycle);
  }

  public AggregatePersistEvent(AggregateIdentifier source, Lifecycle lifecycle) {
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
