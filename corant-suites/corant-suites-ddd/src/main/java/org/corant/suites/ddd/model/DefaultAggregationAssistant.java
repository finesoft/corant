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
package org.corant.suites.ddd.model;

import static org.corant.kernel.util.Preconditions.requireGaet;
import static org.corant.kernel.util.Preconditions.requireNotNull;
import java.lang.annotation.Annotation;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.corant.Corant;
import org.corant.suites.bundle.GlobalMessageCodes;
import org.corant.suites.ddd.event.Event;
import org.corant.suites.ddd.message.Message;
import org.corant.suites.ddd.message.MessageUtils;

/**
 * @author bingo 上午10:57:03
 */
public class DefaultAggregationAssistant implements AggregationAssistant {

  private static final String FIRE_LOG = "Fire event [%s] to event listener!";
  private static final String RISE_LOG = "Register integration message [%s] to message queue!";

  protected final transient Logger logger = Logger.getLogger(this.getClass().toString());
  protected transient final Aggregation aggregation;

  protected transient final Queue<Message> messages = new LinkedList<>();
  protected transient volatile long lastMessageSequenceNumber = 1L;

  public DefaultAggregationAssistant(Aggregation aggregation) {
    this.aggregation = requireNotNull(aggregation, GlobalMessageCodes.ERR_PARAM);
  }

  public DefaultAggregationAssistant(Aggregation aggregation, long lastMessageSequenceNumber) {
    this(aggregation);
    this.lastMessageSequenceNumber =
        requireGaet(lastMessageSequenceNumber, 0L, GlobalMessageCodes.ERR_PARAM);
  }

  @Override
  public void clearMessages() {
    messages.clear();
  }

  @Override
  public List<Message> dequeueMessages(boolean flush) {
    final AtomicLong counter = new AtomicLong(lastMessageSequenceNumber);
    List<Message> exMsgs = messages.stream().map(m -> {
      m.getMetadata().resetSequenceNumber(counter.incrementAndGet());
      return m;
    }).collect(Collectors.toList());
    if (flush) {
      clearMessages();
    }
    return exMsgs;
  }

  @Override
  public void enqueueMessages(Message... messages) {
    if (aggregation.getId() != null) {
      for (Message msg : messages) {
        if (msg != null) {
          logger.fine(() -> String.format(RISE_LOG, msg.toString()));
          MessageUtils.mergeToQueue(this.messages, msg);
        }
      }
      // FIXME
      if (aggregation instanceof AbstractDefaultAggregation) {
        AbstractDefaultAggregation.class.cast(aggregation)
            .setMn(lastMessageSequenceNumber + this.messages.size());
      }
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (this.getClass() != obj.getClass()) {
      return false;
    }
    DefaultAggregationAssistant other = (DefaultAggregationAssistant) obj;
    if (aggregation == null) {
      if (other.aggregation != null) {
        return false;
      }
    } else if (!aggregation.equals(other.aggregation)) {
      return false;
    }
    return true;
  }

  @Override
  public void fireAsyncEvent(Event event, Annotation... qualifiers) {
    if (event != null) {
      logger.fine(() -> String.format(FIRE_LOG, event.toString()));
      Corant.fireAsyncEvent(event, qualifiers);
    }
  }

  @Override
  public void fireEvent(Event event, Annotation... qualifiers) {
    if (event != null) {
      logger.fine(() -> String.format(FIRE_LOG, event.toString()));
      Corant.fireEvent(event, qualifiers);
    }
  }

  @Override
  public Aggregation getAggregation() {
    return aggregation;
  }

  public long getLastMessageSequenceNumber() {
    return lastMessageSequenceNumber;
  }

  @Override
  public long getMessageSequenceNumber() {
    return messages.size() + getLastMessageSequenceNumber();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (aggregation == null ? 0 : aggregation.hashCode());
    return result;
  }

}
