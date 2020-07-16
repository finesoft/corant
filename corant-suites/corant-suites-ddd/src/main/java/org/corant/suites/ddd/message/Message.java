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

/**
 * corant-suites-ddd
 *
 * @author bingo 上午10:41:55
 */
public interface Message extends Serializable {

  static int compare(Message m1, Message m2) {
    int occuredTimeCmpr = compareOccurredTime(m1, m2);
    return occuredTimeCmpr == 0 ? compareSequenceNumber(m1, m2) : occuredTimeCmpr;
  }

  static int compareOccurredTime(Message m1, Message m2) {
    return m1.getMetadata().getOccurredTime().compareTo(m2.getMetadata().getOccurredTime());
  }

  static int compareSequenceNumber(Message m1, Message m2) {
    if (m1 instanceof AbstractAggregateMessage && m2 instanceof AbstractAggregateMessage) {
      return Long.compare(((AbstractAggregateMessage) m1).getMetadata().getVersionNumber(),
          ((AbstractAggregateMessage) m2).getMetadata().getVersionNumber());
    }
    return 0;
  }

  MessageMetadata getMetadata();

  public interface ExchangedMessage extends Message {

    MessageIdentifier getOriginalMessage();

  }

  public interface MessageHandling extends Serializable {

    Object getDestination(); // Should we have this property?

    Instant getHandledTime();

    Object getHandler();

    Object getMessageId();

    boolean isSuccess();
  }

  public interface MessageIdentifier {

    Serializable getId();

    Object getQueue();

    Serializable getType();

  }

  public interface MessageMetadata extends Serializable {

    Instant getOccurredTime();

    Object getSource();

  }

  public interface MessageQueues {

    String DFLT = "default";

  }

}
