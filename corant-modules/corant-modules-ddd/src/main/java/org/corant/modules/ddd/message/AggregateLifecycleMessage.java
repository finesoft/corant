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
package org.corant.modules.ddd.message;

import org.corant.modules.ddd.model.Aggregate;
import org.corant.modules.ddd.model.Aggregate.Lifecycle;
import org.corant.modules.ddd.model.Value.SimpleValueMap;

/**
 * corant-modules-ddd
 *
 * @author bingo 下午5:48:49
 *
 */
public class AggregateLifecycleMessage extends AbstractAggregateMessage implements MergableMessage {

  private static final long serialVersionUID = -5988315884617833263L;
  private final Lifecycle lifecycle;
  private final SimpleValueMap payload;

  public AggregateLifecycleMessage(Aggregate aggregate, Lifecycle lifecycle) {
    super(aggregate);
    this.lifecycle = lifecycle;
    if (aggregate instanceof AggregateLifecycleMessageBuilder) {
      payload = ((AggregateLifecycleMessageBuilder) aggregate).buildLifecycleMessagePayload();
    } else {
      payload = SimpleValueMap.empty();
    }
  }

  public Lifecycle getLifecycle() {
    return lifecycle;
  }

  public SimpleValueMap getPayload() {
    return payload;
  }

  @Override
  public AggregateLifecycleMessage merge(MergableMessage other) {
    return this;
  }

  @FunctionalInterface
  public interface AggregateLifecycleMessageBuilder {
    SimpleValueMap buildLifecycleMessagePayload();
  }

}
