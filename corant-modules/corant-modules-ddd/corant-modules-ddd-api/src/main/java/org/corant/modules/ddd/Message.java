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
package org.corant.modules.ddd;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Maps.immutableMapOf;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.corant.modules.ddd.annotation.Messages;

/**
 * corant-modules-ddd-api
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

  interface AggregateMessage extends Message {

  }

  interface ExchangedMessage extends Message {

    MessageIdentifier getOriginalMessage();

  }

  class MessageDestination implements Serializable {

    private static final long serialVersionUID = -7393678217089712798L;

    protected final String broker;
    protected final String destination;
    protected final boolean multicast;
    protected final Map<String, String> properties;

    public MessageDestination(Messages messages) {
      broker = messages.broker();
      destination = shouldNotNull(messages.destination());
      multicast = messages.multicast();
      if (isNotEmpty(messages.properties())) {
        properties = immutableMapOf((Object[]) messages.properties());
      } else {
        properties = Collections.emptyMap();
      }
    }

    /**
     * @param broker the message broker name or id
     * @param destination the message destination
     * @param multicast whether the message destination is topic or queue
     * @param properties key and value pairs properties
     */
    public MessageDestination(String broker, String destination, boolean multicast,
        String... properties) {
      this.broker = broker;
      this.destination = shouldNotNull(destination);
      this.multicast = multicast;
      if (isNotEmpty(properties)) {
        this.properties = immutableMapOf((Object[]) properties);
      } else {
        this.properties = Collections.emptyMap();
      }
    }

    public static Set<MessageDestination> from(Class<?> clazz) {
      Messages[] metas = shouldNotNull(clazz).getAnnotationsByType(Messages.class);
      Set<MessageDestination> dests = new LinkedHashSet<>();
      if (isNotEmpty(metas)) {
        for (Messages meta : metas) {
          dests.add(new MessageDestination(meta));
        }
      }
      return dests;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      MessageDestination other = (MessageDestination) obj;
      if (broker == null) {
        if (other.broker != null) {
          return false;
        }
      } else if (!broker.equals(other.broker)) {
        return false;
      }
      if (destination == null) {
        if (other.destination != null) {
          return false;
        }
      } else if (!destination.equals(other.destination)) {
        return false;
      }
      if (multicast != other.multicast) {
        return false;
      }
      if (properties == null) {
        if (other.properties != null) {
          return false;
        }
      } else if (!properties.equals(other.properties)) {
        return false;
      }
      return true;
    }

    public String getBroker() {
      return broker;
    }

    public String getDestination() {
      return destination;
    }

    public Map<String, String> getProperties() {
      return properties;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (broker == null ? 0 : broker.hashCode());
      result = prime * result + (destination == null ? 0 : destination.hashCode());
      result = prime * result + (multicast ? 1231 : 1237);
      result = prime * result + (properties == null ? 0 : properties.hashCode());
      return result;
    }

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

}
