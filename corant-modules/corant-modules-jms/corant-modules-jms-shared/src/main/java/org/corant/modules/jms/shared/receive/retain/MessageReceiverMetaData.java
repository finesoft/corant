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
package org.corant.modules.jms.shared.receive.retain;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.max;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNoneBlank;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import org.corant.config.Configs;
import org.corant.context.proxy.ContextualMethodHandler;
import org.corant.context.qualifier.Qualifiers;
import org.corant.modules.jms.shared.annotation.MessageReceive;
import org.corant.shared.util.Retry.BackoffAlgorithm;
import org.corant.shared.util.Retry.RetryInterval;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午3:33:51
 *
 */
public class MessageReceiverMetaData {

  private final ContextualMethodHandler method;
  private final int acknowledge;
  private final String clientID;
  private final String connectionFactoryId;
  private final String destination;
  private final boolean multicast;
  private final String selector;
  private final boolean subscriptionDurable;
  private final Class<?> type;
  private final int cacheLevel;
  private final long receiveTimeout;
  private final int receiveThreshold;
  private final int failureThreshold;
  private final int tryThreshold;
  private final RetryInterval breakedInterval;
  private final long loopIntervalMs;
  private final boolean xa;
  private final int txTimeout;

  MessageReceiverMetaData(ContextualMethodHandler method, String destinationName) {
    this.method = method;
    final MessageReceive ann = method.getMethod().getAnnotation(MessageReceive.class);
    acknowledge = ann.acknowledge();
    clientID = Configs.assemblyStringConfigProperty(ann.clientId());
    connectionFactoryId =
        Qualifiers.resolveName(Configs.assemblyStringConfigProperty(ann.connectionFactoryId()));
    destination = Configs.assemblyStringConfigProperty(destinationName);
    multicast = ann.multicast();
    selector = Configs.assemblyStringConfigProperty(ann.selector());
    subscriptionDurable = ann.subscriptionDurable();
    type = defaultObject(ann.type(), String.class);
    cacheLevel = ann.cacheLevel();
    receiveTimeout = ann.receiveTimeout();
    receiveThreshold = max(1, ann.receiveThreshold());
    failureThreshold = max(4, ann.failureThreshold());
    tryThreshold = max(2, ann.tryThreshold());
    loopIntervalMs = max(500L, ann.loopIntervalMs());
    String bds = Configs.assemblyStringConfigProperty(ann.brokenDuration());
    String maxBds = Configs.assemblyStringConfigProperty(ann.maxBrokenDuration());
    Duration breakedDuration =
        max(isBlank(bds) ? Duration.ofMinutes(15) : Duration.parse(bds), Duration.ofSeconds(8L));
    if (ann.brokenBackoffAlgo() == BackoffAlgorithm.NONE) {
      breakedInterval = RetryInterval.noBackoff(breakedDuration);
    } else if (ann.brokenBackoffAlgo() == BackoffAlgorithm.EXPO) {
      breakedInterval = RetryInterval.expoBackoff(breakedDuration, Duration.parse(maxBds),
          ann.brokenBackoffFactor());
    } else if (ann.brokenBackoffAlgo() == BackoffAlgorithm.EXPO_DECORR) {
      breakedInterval = RetryInterval.expoBackoffDecorr(breakedDuration, Duration.parse(maxBds),
          ann.brokenBackoffFactor());
    } else if (ann.brokenBackoffAlgo() == BackoffAlgorithm.EXPO_EQUAL_JITTER) {
      breakedInterval = RetryInterval.expoBackoffEqualJitter(breakedDuration,
          Duration.parse(maxBds), ann.brokenBackoffFactor());
    } else {
      breakedInterval = RetryInterval.expoBackoffFullJitter(breakedDuration, Duration.parse(maxBds),
          ann.brokenBackoffFactor());
    }
    xa = ann.xa();
    txTimeout = ann.txTimeout();
  }

  public static Set<MessageReceiverMetaData> of(ContextualMethodHandler method) {
    final MessageReceive ann =
        shouldNotNull(shouldNotNull(method).getMethod().getAnnotation(MessageReceive.class));
    shouldBeTrue(isNoneBlank(ann.destinations()));
    Set<String> dests = new LinkedHashSet<>();
    for (String dest : ann.destinations()) {
      dests.addAll(Configs.assemblyStringConfigProperties(dest));
    }
    Set<MessageReceiverMetaData> beans = new LinkedHashSet<>(dests.size());
    dests.forEach(d -> shouldBeTrue(beans.add(new MessageReceiverMetaData(method, d)),
        "The message receive method %s dup!", method.toString()));
    return beans;
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
    MessageReceiverMetaData other = (MessageReceiverMetaData) obj;
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

  public RetryInterval getBreakedInterval() {
    return breakedInterval;
  }

  public int getCacheLevel() {
    return cacheLevel;
  }

  public String getClientID() {
    return clientID;
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

  public String getSelector() {
    return selector;
  }

  public int getTryThreshold() {
    return tryThreshold;
  }

  public int getTxTimeout() {
    return txTimeout;
  }

  public Class<?> getType() {
    return type;
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

  public boolean isSubscriptionDurable() {
    return subscriptionDurable;
  }

  public boolean isXa() {
    return xa;
  }

  @Override
  public String toString() {
    return "MessageReceiverMetaData [method=" + method.getMethod().toGenericString() + ", clientID="
        + clientID + ", connectionFactoryId=" + connectionFactoryId + ", destination=" + destination
        + "]";
  }

}