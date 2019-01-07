/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.asosat.ddd.message;

import static org.corant.kernel.util.Preconditions.requireNotBlank;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;
import org.corant.suites.ddd.message.Message.MessageMetadata;
import org.corant.suites.ddd.model.Aggregate.AggregateIdentifier;
import org.corant.suites.ddd.model.Value;

/**
 * @author bingo 下午6:17:13
 *
 */
@Embeddable
@MappedSuperclass
public abstract class AbstractMessageMetadata implements MessageMetadata, Value {

  private static final long serialVersionUID = 7457915201808878868L;

  @Column(updatable = false, nullable = false)
  private String queue;

  @Column(updatable = false, nullable = false)
  private Instant occurredTime;

  @Column
  private String trackingToken;

  @Column
  private long sequenceNumber;

  protected AbstractMessageMetadata() {}

  protected AbstractMessageMetadata(String queue, String trackingToken, Instant occurredTime,
      long sequenceNumber) {
    super();
    setQueue(queue);
    setTrackingToken(trackingToken);
    setOccurredTime(occurredTime);
    setSequenceNumber(sequenceNumber);
  }

  protected AbstractMessageMetadata(String queue, String trackingToken, long sequenceNumber) {
    this(queue, trackingToken, Instant.now(), sequenceNumber);
  }

  @Override
  public Instant getOccurredTime() {
    return occurredTime;
  }

  @Override
  public String getQueue() {
    return queue;
  }

  @Override
  public long getSequenceNumber() {
    return sequenceNumber;
  }

  @Override
  public abstract AggregateIdentifier getSource();

  @Override
  public String getTrackingToken() {
    return trackingToken;
  }

  protected void setOccurredTime(Instant occurredTime) {
    this.occurredTime = occurredTime == null ? Instant.now() : occurredTime;
  }

  protected void setQueue(String queue) {
    this.queue = requireNotBlank(queue, PkgMsgCds.ERR_MSG_QUEUE_NULL);
  }

  protected void setSequenceNumber(long sequenceNumber) {
    this.sequenceNumber = sequenceNumber;
  }

  protected void setTrackingToken(String trackingToken) {
    this.trackingToken = trackingToken;
  }
}
