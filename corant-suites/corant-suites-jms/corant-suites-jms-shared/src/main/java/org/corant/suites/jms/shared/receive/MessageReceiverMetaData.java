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
package org.corant.suites.jms.shared.receive;

import static org.corant.context.Instances.select;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.max;
import static org.corant.shared.util.Strings.defaultTrim;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNoneBlank;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import javax.jms.ConnectionFactory;
import org.corant.config.ConfigUtils;
import org.corant.context.proxy.ContextualMethodHandler;
import org.corant.shared.ubiquity.Sortable;
import org.corant.suites.jms.shared.AbstractJMSExtension;
import org.corant.suites.jms.shared.annotation.MessageReceive;
import org.corant.suites.jms.shared.context.JMSExceptionListener;

/**
 * corant-suites-jms-shared
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
  private final long loopIntervalMs;
  private final Duration breakedDuration;
  private final boolean xa;

  MessageReceiverMetaData(ContextualMethodHandler method, String destinationName) {
    this.method = method;
    final MessageReceive ann = method.getMethod().getAnnotation(MessageReceive.class);
    acknowledge = ann.acknowledge();
    clientID = ann.clientId();
    connectionFactoryId = defaultTrim(ann.connectionFactoryId());
    destination = destinationName;
    multicast = ann.multicast();
    selector = ann.selector();
    subscriptionDurable = ann.subscriptionDurable();
    type = defaultObject(ann.type(), String.class);
    cacheLevel = ann.cacheLevel();
    receiveTimeout = ann.receiveTimeout();
    receiveThreshold = max(1, ann.receiveThreshold());
    failureThreshold = max(4, ann.failureThreshold());
    tryThreshold = max(2, ann.tryThreshold());
    loopIntervalMs = max(500L, ann.loopIntervalMs());
    breakedDuration = max(isBlank(ann.breakedDuration()) ? Duration.ofMinutes(15)
        : Duration.parse(ann.breakedDuration()), Duration.ofSeconds(8L));
    xa = ann.xa();
  }

  public static Set<MessageReceiverMetaData> of(ContextualMethodHandler method) {
    final MessageReceive ann =
        shouldNotNull(shouldNotNull(method).getMethod().getAnnotation(MessageReceive.class));
    shouldBeTrue(isNoneBlank(ann.destinations()));
    Set<String> dests = new LinkedHashSet<>();
    for (String dest : ann.destinations()) {
      dests.addAll(ConfigUtils.assemblyStringConfigProperties(dest));
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

  /**
   *
   * @return the acknowledge
   */
  public int getAcknowledge() {
    return acknowledge;
  }

  /**
   *
   * @return the breakedDuration
   */
  public Duration getBreakedDuration() {
    return breakedDuration;
  }

  /**
   *
   * @return the cacheLevel
   */
  public int getCacheLevel() {
    return cacheLevel;
  }

  /**
   *
   * @return the clientID
   */
  public String getClientID() {
    return clientID;
  }

  /**
   *
   * @return the connectionFactoryId
   */
  public String getConnectionFactoryId() {
    return connectionFactoryId;
  }

  /**
   *
   * @return the destination
   */
  public String getDestination() {
    return destination;
  }

  public int getFailureThreshold() {
    return failureThreshold;
  }

  /**
   *
   * @return the loopIntervalMs
   */
  public long getLoopIntervalMs() {
    return loopIntervalMs;
  }

  /**
   *
   * @return the method
   */
  public ContextualMethodHandler getMethod() {
    return method;
  }

  /**
   *
   * @return the receiveThreshold
   */
  public int getReceiveThreshold() {
    return receiveThreshold;
  }

  public long getReceiveTimeout() {
    return receiveTimeout;
  }

  /**
   *
   * @return the selector
   */
  public String getSelector() {
    return selector;
  }

  /**
   *
   * @return the tryThreshold
   */
  public int getTryThreshold() {
    return tryThreshold;
  }

  /**
   *
   * @return the type
   */
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

  /**
   *
   * @return the multicast
   */
  public boolean isMulticast() {
    return multicast;
  }

  /**
   *
   * @return the subscriptionDurable
   */
  public boolean isSubscriptionDurable() {
    return subscriptionDurable;
  }

  @Override
  public String toString() {
    return "MessageReceiverMetaData [method=" + method + ", acknowledge=" + acknowledge
        + ", clientID=" + clientID + ", connectionFactoryId=" + connectionFactoryId
        + ", destination=" + destination + ", multicast=" + multicast + ", selector=" + selector
        + ", subscriptionDurable=" + subscriptionDurable + ", type=" + type + ", cacheLevel="
        + cacheLevel + ", receiveTimeout=" + receiveTimeout + ", receiveThreshold="
        + receiveThreshold + ", failureThreshold=" + failureThreshold + ", tryThreshold="
        + tryThreshold + ", loopIntervalMs=" + loopIntervalMs + ", breakedDuration="
        + breakedDuration + "]";
  }

  ConnectionFactory connectionFactory() {
    return AbstractJMSExtension.getConnectionFactory(connectionFactoryId);

  }

  Optional<JMSExceptionListener> exceptionListener() {
    return select(JMSExceptionListener.class).stream().sorted(Sortable::compare).findFirst();
  }

  boolean xa() {
    return xa;
  }
}
