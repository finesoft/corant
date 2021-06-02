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
package org.corant.modules.jms.shared.receive;

import static org.corant.shared.util.Assertions.shouldBeFalse;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotEmpty;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Objects.max;
import static org.corant.shared.util.Sets.setOf;
import static org.corant.shared.util.Strings.isBlank;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.corant.context.proxy.ContextualMethodHandler;
import org.corant.context.qualifier.Qualifiers;
import org.corant.modules.jms.metadata.MessageDestinationMetaData;
import org.corant.modules.jms.metadata.MessageDrivenMetaData;
import org.corant.modules.jms.metadata.MessageReplyMetaData;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Retry.BackoffAlgorithm;
import org.corant.shared.util.Retry.RetryInterval;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午3:33:51
 *
 */
public class MessageReceivingMetaData {

  static final Logger logger = Logger.getLogger(MessageReceivingMetaData.class.getName());

  private final ContextualMethodHandler method;
  private final int acknowledge;
  private final String connectionFactoryId;
  private final String destination;
  private final boolean multicast;
  private final String selector;
  private final int cacheLevel;
  private final long receiveTimeout;
  private final int receiveThreshold;
  private final int failureThreshold;
  private final int tryThreshold;
  private final RetryInterval brokenInterval;
  private final long loopIntervalMs;
  private final boolean xa;
  private final int txTimeout;
  private final Collection<MessageReplyMetaData> replies;

  MessageReceivingMetaData(ContextualMethodHandler method, MessageDestinationMetaData dest,
      MessageDrivenMetaData driven) {
    this.method = method;
    // the destination
    connectionFactoryId = Qualifiers.resolveName(dest.getConnectionFactoryId());
    destination = dest.getName();
    multicast = dest.isMulticast();
    // the driven
    acknowledge = driven.getAcknowledge();
    selector = driven.getSelector();
    cacheLevel = driven.getCacheLevel();
    receiveTimeout = driven.getReceiveTimeout();
    receiveThreshold = max(1, driven.getReceiveThreshold());
    failureThreshold = max(2, driven.getFailureThreshold());
    tryThreshold = max(1, driven.getTryThreshold());
    loopIntervalMs = max(500L, driven.getLoopIntervalMs());
    String bds = driven.getBrokenDuration();
    String maxBds = driven.getMaxBrokenDuration();
    Duration brokenDuration =
        max(isBlank(bds) ? Duration.ofMinutes(15) : Duration.parse(bds), Duration.ofSeconds(1L));
    if (driven.getBrokenBackoffAlgo() == BackoffAlgorithm.NONE) {
      brokenInterval = RetryInterval.noBackoff(brokenDuration);
    } else if (driven.getBrokenBackoffAlgo() == BackoffAlgorithm.EXPO) {
      brokenInterval = RetryInterval.expoBackoff(brokenDuration, Duration.parse(maxBds),
          driven.getBrokenBackoffFactor());
    } else if (driven.getBrokenBackoffAlgo() == BackoffAlgorithm.EXPO_DECORR) {
      brokenInterval = RetryInterval.expoBackoffDecorr(brokenDuration, Duration.parse(maxBds),
          driven.getBrokenBackoffFactor());
    } else if (driven.getBrokenBackoffAlgo() == BackoffAlgorithm.EXPO_EQUAL_JITTER) {
      brokenInterval = RetryInterval.expoBackoffEqualJitter(brokenDuration, Duration.parse(maxBds),
          driven.getBrokenBackoffFactor());
    } else {
      brokenInterval = RetryInterval.expoBackoffFullJitter(brokenDuration, Duration.parse(maxBds),
          driven.getBrokenBackoffFactor());
    }
    xa = driven.isXa();
    txTimeout = driven.getTxTimeout();
    replies = driven.getReply();
    shouldBeTrue(setOf(replies).size() == replies.size(),
        "The reply destination in annotation must be unique.");
    for (MessageReplyMetaData r : replies) {
      if (isBlank(r.getDestination()) || r.getDestination().equals(destination)) {
        throw new CorantRuntimeException(
            "The reply destination %s can't blank and must not equals receive destination.",
            r.getDestination());
      }
    }
    if (isNotEmpty(replies)) {
      shouldBeFalse(method.getMethod().getReturnType().equals(Void.TYPE),
          "The message receiving method %s requires a return type because the method is configured with a reply.",
          method.getMethod());
    } else {
      if (!method.getMethod().getReturnType().equals(Void.TYPE)) {
        logger.warning(() -> String.format(
            "The message receiving method %s has a return type, but the method is configured without a reply.",
            method.getMethod()));
      }
    }
  }

  public static Set<MessageReceivingMetaData> of(ContextualMethodHandler method) {
    MessageDrivenMetaData driven =
        MessageDrivenMetaData.from(method.getMethod(), method.getQualifiers());
    Set<MessageDestinationMetaData> dests = MessageDestinationMetaData.from(method.getMethod());
    if (isEmpty(dests)) {
      dests = MessageDestinationMetaData.from(method.getMethod().getParameterTypes()[0]);
    }
    shouldNotEmpty(dests);
    Set<MessageReceivingMetaData> metas = new LinkedHashSet<>(dests.size());
    for (MessageDestinationMetaData dest : dests) {
      shouldBeTrue(metas.add(new MessageReceivingMetaData(method, dest, driven)),
          "The message receive method %s dup!", method.toString());
    }
    return metas;
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
    MessageReceivingMetaData other = (MessageReceivingMetaData) obj;
    if (connectionFactoryId == null) {
      if (other.connectionFactoryId != null) {
        return false;
      }
    } else if (!connectionFactoryId.equals(other.connectionFactoryId)) {
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
    if (selector == null) {
      if (other.selector != null) {
        return false;
      }
    } else if (!selector.equals(other.selector)) {
      return false;
    }
    return true;
  }

  public int getAcknowledge() {
    return acknowledge;
  }

  public RetryInterval getBrokenInterval() {
    return brokenInterval;
  }

  public int getCacheLevel() {
    return cacheLevel;
  }

  public String getConnectionFactoryId() {
    return connectionFactoryId;
  }

  public String getDestination() {
    return destination;
  }

  public int getFailureThreshold() {
    return failureThreshold;
  }

  public long getLoopIntervalMs() {
    return loopIntervalMs;
  }

  public ContextualMethodHandler getMethod() {
    return method;
  }

  public int getReceiveThreshold() {
    return receiveThreshold;
  }

  public long getReceiveTimeout() {
    return receiveTimeout;
  }

  public Collection<MessageReplyMetaData> getReplies() {
    return replies;
  }

  public String getSelector() {
    return selector;
  }

  public int getTryThreshold() {
    return tryThreshold;
  }

  public int getTxTimeout() {
    return txTimeout;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (connectionFactoryId == null ? 0 : connectionFactoryId.hashCode());
    result = prime * result + (destination == null ? 0 : destination.hashCode());
    result = prime * result + (multicast ? 1231 : 1237);
    result = prime * result + (selector == null ? 0 : selector.hashCode());
    return result;
  }

  public boolean isMulticast() {
    return multicast;
  }

  public boolean isXa() {
    return xa;
  }

  @Override
  public String toString() {
    return "destination=[" + (isBlank(connectionFactoryId) ? "" : connectionFactoryId + "#")
        + destination + "], method=" + method.getMethod().getDeclaringClass().getCanonicalName()
        + "#" + method.getMethod().getName() + "]";
  }

}
