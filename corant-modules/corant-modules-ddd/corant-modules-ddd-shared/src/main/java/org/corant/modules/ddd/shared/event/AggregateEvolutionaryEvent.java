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
package org.corant.modules.ddd.shared.event;

import static org.corant.shared.util.Objects.forceCast;
import org.corant.modules.ddd.AbstractEvent;
import org.corant.modules.ddd.Aggregate;

/**
 * corant-modules-ddd-shared
 * <p>
 * An evolutionary event for an aggregate, fired at the last moment before the unit of work
 * completes (transaction commits)
 *
 * @author bingo 下午5:53:06
 */
public class AggregateEvolutionaryEvent extends AbstractEvent {

  private static final long serialVersionUID = 2268460523721510639L;

  public AggregateEvolutionaryEvent(Aggregate aggregate) {
    super(aggregate);
  }

  public <T extends Aggregate> T getAggregate() {
    return forceCast(getSource());

  }
}
