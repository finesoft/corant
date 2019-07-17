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
import org.corant.suites.ddd.model.AbstractAggregation.DefaultAggregationIdentifier;
import org.corant.suites.ddd.model.AbstractDefaultAggregation;
import org.corant.suites.ddd.model.Aggregation;
import org.corant.suites.ddd.model.Aggregation.Lifecycle;
import org.corant.suites.ddd.model.Value.SimpleValueMap;

/**
 * corant-suites-ddd
 *
 * @author bingo 下午5:48:49
 *
 */
public class AggregationLifecycleMessage implements MergableMessage {

  private static final long serialVersionUID = -5988315884617833263L;
  private final AggregationLifecycleMessageMetadata metadata;
  private final Lifecycle lifecycle;
  private final SimpleValueMap payload;

  public AggregationLifecycleMessage(Aggregation aggregation, Lifecycle lifecycle) {
    metadata = new AggregationLifecycleMessageMetadata(aggregation);
    this.lifecycle = lifecycle;
    if (aggregation instanceof AggregationLifecycleMessageBuilder) {
      payload = ((AggregationLifecycleMessageBuilder) aggregation).buildLifecycleMessagePayload();
    } else {
      payload = SimpleValueMap.empty();
    }
  }

  public AggregationLifecycleMessage(Aggregation aggregation, Lifecycle lifecycle,
      SimpleValueMap payload) {
    metadata = new AggregationLifecycleMessageMetadata(aggregation);
    this.lifecycle = lifecycle;
    this.payload = defaultObject(payload, SimpleValueMap.empty());
  }

  /**
   * @param metadata
   * @param lifecycle
   * @param payload
   */
  public AggregationLifecycleMessage(AggregationLifecycleMessageMetadata metadata, Lifecycle lifecycle,
      SimpleValueMap payload) {
    super();
    this.metadata = metadata;
    this.lifecycle = lifecycle;
    this.payload = payload;
  }

  public Lifecycle getLifecycle() {
    return lifecycle;
  }

  @Override
  public AggregationLifecycleMessageMetadata getMetadata() {
    return metadata;
  }

  @Override
  public SimpleValueMap getPayload() {
    return payload;
  }

  @Override
  public AggregationLifecycleMessage merge(MergableMessage other) {
    return this;
  }

  @FunctionalInterface
  public interface AggregationLifecycleMessageBuilder {
    SimpleValueMap buildLifecycleMessagePayload();
  }

  public static class AggregationLifecycleMessageMetadata implements MessageMetadata {

    private static final long serialVersionUID = 5896162140881655490L;

    private final DefaultAggregationIdentifier source;
    private final Instant occurredTime = Instant.now();
    private final long versionNumber;
    private long sequenceNumber = 0;

    public AggregationLifecycleMessageMetadata(Aggregation aggregation) {
      source = new DefaultAggregationIdentifier(aggregation);
      if (aggregation instanceof AbstractDefaultAggregation) {
        sequenceNumber = AbstractDefaultAggregation.class.cast(aggregation).getMn();
      }
      versionNumber = aggregation.getVn();
    }

    /**
     * @param source
     * @param versionNumber
     * @param sequenceNumber
     */
    public AggregationLifecycleMessageMetadata(DefaultAggregationIdentifier source, long versionNumber,
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
    public DefaultAggregationIdentifier getSource() {
      return source;
    }

    @Override
    public void resetSequenceNumber(long sequenceNumber) {
      this.sequenceNumber = sequenceNumber;
    }
  }

}
