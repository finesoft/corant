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

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isNotEmpty;
import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import org.corant.shared.util.Objects;

/**
 * corant-modules-ddd
 *
 * @author bingo 上午10:41:55
 */
public interface Message extends Serializable {

  MessageDestination[] EMPTY_DESTINATIONS = new MessageDestination[0];

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

  interface ExchangedMessage extends Message {

    MessageIdentifier getOriginalMessage();

  }

  class MessageDestination implements Serializable {

    private static final long serialVersionUID = -7393678217089712798L;

    protected Serializable destination;
    protected boolean multicast;

    /**
     * @param destination
     * @param multicast
     */
    public MessageDestination(Serializable destination, boolean multicast) {
      this.destination = shouldNotNull(destination);
      this.multicast = multicast;
    }

    protected MessageDestination() {

    }

    public static MessageDestination[] anycastTo(Serializable... destinations) {
      if (isNotEmpty(destinations)) {
        return Arrays.stream(destinations).filter(Objects::isNotNull)
            .map(x -> new MessageDestination(x, false)).toArray(MessageDestination[]::new);
      }
      return EMPTY_DESTINATIONS;
    }

    public static MessageDestination[] multicastTo(Serializable... destinations) {
      if (isNotEmpty(destinations)) {
        return Arrays.stream(destinations).filter(Objects::isNotNull)
            .map(x -> new MessageDestination(x, true)).toArray(MessageDestination[]::new);
      }
      return EMPTY_DESTINATIONS;
    }

    /**
     *
     * @return the destination
     */
    public Serializable getDestination() {
      return destination;
    }

    /**
     *
     * @return the multicast
     */
    public boolean isMulticast() {
      return multicast;
    }

  }

  interface MessageHandling extends Serializable {

    Object getDestination(); // Should we have this property?

    Instant getHandledTime();

    Object getHandler();

    Object getMessageId();

    boolean isSuccess();
  }

  interface MessageIdentifier {

    Serializable getId();

    Object getQueue();

    Serializable getType();

  }

  interface MessageMetadata extends Serializable {

    default MessageDestination[] getDestinations() {
      return EMPTY_DESTINATIONS;
    }

    Instant getOccurredTime();

    Serializable getSource();
  }

  interface MessageQueues {

    String DFLT = "default";

  }

}
