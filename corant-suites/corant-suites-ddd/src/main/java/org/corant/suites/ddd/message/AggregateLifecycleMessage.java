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
package org.corant.suites.ddd.message;

import static org.corant.shared.util.MapUtils.mapOf;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import java.time.Instant;
import java.util.Map;
import org.corant.suites.ddd.model.AbstractAggregate.DefaultAggregateIdentifier;
import org.corant.suites.ddd.model.AbstractDefaultAggregate;
import org.corant.suites.ddd.model.Aggregate;
import org.corant.suites.ddd.model.Aggregate.Lifecycle;
import org.corant.suites.ddd.model.Value.SimpleValueMap;

/**
 * corant-suites-ddd
 *
 * @author bingo 下午5:48:49
 *
 */
public class AggregateLifecycleMessage implements MergableMessage {

  private static final long serialVersionUID = -5988315884617833263L;
  private final AggregateLifecycleMessageMetadata metadata;
  private final Lifecycle lifecycle;
  private final SimpleValueMap payload;

  public AggregateLifecycleMessage(Aggregate aggregate, Lifecycle lifecycle) {
    metadata = new AggregateLifecycleMessageMetadata(aggregate);
    this.lifecycle = lifecycle;
    if (aggregate instanceof AggregateLifecycleMessageBuilder) {
      payload = ((AggregateLifecycleMessageBuilder) aggregate).buildLifecycleMessagePayload();
    } else {
      payload = SimpleValueMap.empty();
    }
  }

  public AggregateLifecycleMessage(Aggregate aggregate, Lifecycle lifecycle,
      SimpleValueMap payload) {
    metadata = new AggregateLifecycleMessageMetadata(aggregate);
    this.lifecycle = lifecycle;
    this.payload = defaultObject(payload, SimpleValueMap.empty());
  }

  public Lifecycle getLifecycle() {
    return lifecycle;
  }

  @Override
  public AggregateLifecycleMessageMetadata getMetadata() {
    return metadata;
  }

  @Override
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

  public static class AggregateLifecycleMessageMetadata implements MessageMetadata {

    private static final long serialVersionUID = 5896162140881655490L;

    private final DefaultAggregateIdentifier source;
    private final Instant occurredTime = Instant.now();
    private final long versionNumber;
    private long sequenceNumber = 0;

    public AggregateLifecycleMessageMetadata(Aggregate aggregate) {
      source = new DefaultAggregateIdentifier(aggregate);
      if (aggregate instanceof AbstractDefaultAggregate) {
        sequenceNumber = AbstractDefaultAggregate.class.cast(aggregate).getMn();
      }
      versionNumber = aggregate.getVn();
    }

    /**
     * @param source
     * @param versionNumber
     * @param sequenceNumber
     */
    public AggregateLifecycleMessageMetadata(DefaultAggregateIdentifier source, long versionNumber,
        long sequenceNumber) {
      super();
      this.source = source;
      this.versionNumber = versionNumber;
      this.sequenceNumber = sequenceNumber;
    }

    @Override
    public Map<String, Object> getAttributes() {
      return mapOf("versionNumber", versionNumber);
    }

    @Override
    public Instant getOccurredTime() {
      return occurredTime;
    }

    @Override
    public long getSequenceNumber() {
      return sequenceNumber;
    }

    @Override
    public DefaultAggregateIdentifier getSource() {
      return source;
    }

    @Override
    public void resetSequenceNumber(long sequenceNumber) {
      this.sequenceNumber = sequenceNumber;
    }
  }

}
