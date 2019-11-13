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

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.corant.suites.ddd.message.Message.MessageMetadata;
import org.corant.suites.ddd.model.AbstractAggregate.DefaultAggregateIdentifier;
import org.corant.suites.ddd.model.AbstractDefaultAggregate;
import org.corant.suites.ddd.model.Aggregate;
import org.corant.suites.ddd.model.Aggregate.AggregateIdentifier;

/**
 * corant-suites-ddd
 *
 * @author bingo 上午10:08:17
 *
 */
public class AggregateMessageMetadata implements MessageMetadata {
  private static final long serialVersionUID = -3045014218500057397L;
  protected Map<String, Serializable> attributes = new HashMap<>();
  protected long versionNumber;
  protected long sequenceNumber = 0;
  private AggregateIdentifier source;
  private Instant occurredTime = Instant.now();

  public AggregateMessageMetadata(Aggregate aggregate) {
    source = new DefaultAggregateIdentifier(aggregate);
    if (aggregate instanceof AbstractDefaultAggregate) {
      sequenceNumber = ((AbstractDefaultAggregate) aggregate).getMn();
    }
    versionNumber = aggregate.getVn();
  }

  /**
   * @param source
   * @param versionNumber
   * @param sequenceNumber
   */
  public AggregateMessageMetadata(AggregateIdentifier source, long versionNumber,
      long sequenceNumber) {
    super();
    this.versionNumber = versionNumber;
    this.sequenceNumber = sequenceNumber;
    this.source = source;
  }

  protected AggregateMessageMetadata() {

  }

  public Map<String, Serializable> getAttributes() {
    return Collections.unmodifiableMap(attributes);
  }

  @Override
  public Instant getOccurredTime() {
    return occurredTime;
  }

  public long getSequenceNumber() {
    return sequenceNumber;
  }

  @Override
  public AggregateIdentifier getSource() {
    return source;
  }

  public long getVersionNumber() {
    return versionNumber;
  }

  public void resetSequenceNumber(long sequenceNumber) {
    this.sequenceNumber = sequenceNumber;
  }

}
