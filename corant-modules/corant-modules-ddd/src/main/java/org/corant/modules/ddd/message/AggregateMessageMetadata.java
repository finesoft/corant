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

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.corant.modules.ddd.message.Message.MessageMetadata;
import org.corant.modules.ddd.model.AbstractAggregate.DefaultAggregateIdentifier;
import org.corant.modules.ddd.model.Aggregate;
import org.corant.modules.ddd.model.Aggregate.AggregateIdentifier;

/**
 * corant-modules-ddd
 *
 * @author bingo 上午10:08:17
 *
 */
public class AggregateMessageMetadata implements MessageMetadata {
  private static final long serialVersionUID = -3045014218500057397L;
  protected Map<String, Serializable> attributes = new HashMap<>();
  protected long versionNumber;
  private AggregateIdentifier source;
  private final Instant occurredTime = Instant.now();

  public AggregateMessageMetadata(Aggregate aggregate) {
    source = new DefaultAggregateIdentifier(aggregate);
    versionNumber = aggregate.getVn();
  }

  /**
   * @param source
   * @param versionNumber
   */
  public AggregateMessageMetadata(AggregateIdentifier source, long versionNumber) {
    this.versionNumber = versionNumber;
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

  @Override
  public AggregateIdentifier getSource() {
    return source;
  }

  public long getVersionNumber() {
    return versionNumber;
  }

  @Override
  public String toString() {
    return "AggregateMessageMetadata [versionNumber=" + versionNumber + ", source=" + source
        + ", occurredTime=" + occurredTime + "]";
  }

}
