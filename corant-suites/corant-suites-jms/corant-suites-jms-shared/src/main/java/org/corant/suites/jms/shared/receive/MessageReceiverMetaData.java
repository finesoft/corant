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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.CollectionUtils.linkedHashSetOf;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.shared.util.StringUtils.isNoneBlank;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.transaction.Transactional;
import org.corant.suites.jms.shared.annotation.MessageReceive;
import org.corant.suites.jta.shared.TransactionalMetaUtils;

/**
 * corant-suites-jms-shared
 *
 * @author bingo 下午3:33:51
 *
 */
public class MessageReceiverMetaData {

  private final AnnotatedMethod<?> method;
  private final int acknowledge;
  private final String clientId;
  private final String connectionFactoryId;
  private final String destination;
  private final boolean multicast;
  private final String selector;
  private final boolean subscriptionDurable;
  private final Class<?> type;
  private final Transactional transactional;
  private final int cacheLevel;
  private final long receiveTimeout;

  MessageReceiverMetaData(AnnotatedMethod<?> method, String destinationName) {
    this.method = shouldNotNull(method);
    AccessController.doPrivileged((PrivilegedAction<?>) () -> {
      method.getJavaMember().setAccessible(true);
      return null;
    });
    shouldBeTrue(method.isAnnotationPresent(MessageReceive.class));
    final MessageReceive ann = method.getAnnotation(MessageReceive.class);
    acknowledge = ann.acknowledge();
    clientId = ann.clientId();
    connectionFactoryId = ann.connectionFactoryId();
    destination = destinationName;
    multicast = ann.multicast();
    selector = ann.selector();
    subscriptionDurable = ann.subscriptionDurable();
    type = defaultObject(ann.type(), String.class);
    transactional = TransactionalMetaUtils.getTransactionalAnnotationRecursive(method);
    cacheLevel = ann.cacheLevel();
    receiveTimeout = ann.receiveTimeout();
  }

  public static Set<MessageReceiverMetaData> of(AnnotatedMethod<?> method) {
    shouldBeTrue(method.isAnnotationPresent(MessageReceive.class));
    final MessageReceive ann = method.getAnnotation(MessageReceive.class);
    shouldBeTrue(isNoneBlank(ann.destinations()));
    Set<MessageReceiverMetaData> beans = new LinkedHashSet<>();
    linkedHashSetOf(ann.destinations()).forEach(d -> {
      shouldBeTrue(beans.add(new MessageReceiverMetaData(method, d)),
          "The message receive method %s dup!", method.toString());
    });
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
   * @return the cacheLevel
   */
  public int getCacheLevel() {
    return cacheLevel;
  }

  /**
   *
   * @return the clientId
   */
  public String getClientId() {
    return clientId;
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

  /**
   *
   * @return the method
   */
  public AnnotatedMethod<?> getMethod() {
    return method;
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
   * @return the transactional
   */
  public Transactional getTransactional() {
    return transactional;
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
    return "MessageReceiverMetaData [method=" + method + ", connectionFactoryId="
        + connectionFactoryId + ", destination=" + destination + ", multicast=" + multicast
        + ", selector=" + selector + ", cacheLevel=" + cacheLevel + "]";
  }

}
