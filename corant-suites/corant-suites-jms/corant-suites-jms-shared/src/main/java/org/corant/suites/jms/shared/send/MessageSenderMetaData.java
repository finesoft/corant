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
package org.corant.suites.jms.shared.send;

import static org.corant.shared.util.Assertions.shouldNotNull;
import org.corant.config.Configs;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.suites.jms.shared.annotation.MessageSend;
import org.corant.suites.jms.shared.annotation.MessageSend.SerializationSchema;

/**
 * corant-suites-jms-shared
 *
 * @author bingo 下午4:51:54
 *
 */
public class MessageSenderMetaData {

  private final String connectionFactoryId;

  private final String destination;

  private final boolean multicast;

  private final int sessionMode;

  private final SerializationSchema serialization;

  private final int deliveryMode;

  public MessageSenderMetaData(MessageSend annotation) {
    this(shouldNotNull(annotation).connectionFactoryId(), annotation.destination(),
        annotation.multicast(), annotation.sessionMode(), annotation.serialization(),
        annotation.deliveryMode());
  }

  /**
   * @param connectionFactoryId
   * @param destination
   * @param multicast
   * @param sessionMode
   * @param serialization
   * @param deliveryMode
   */
  public MessageSenderMetaData(String connectionFactoryId, String destination, boolean multicast,
      int sessionMode, SerializationSchema serialization, int deliveryMode) {
    super();
    this.connectionFactoryId = Configs.assemblyStringConfigProperty(connectionFactoryId);
    this.destination = Configs.assemblyStringConfigProperty(destination);
    this.multicast = multicast;
    this.sessionMode = sessionMode;
    this.serialization = serialization;
    this.deliveryMode = deliveryMode;
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
    MessageSenderMetaData other = (MessageSenderMetaData) obj;
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
    return true;
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
   * @return the deliveryMode
   */
  public int getDeliveryMode() {
    return deliveryMode;
  }

  /**
   *
   * @return the destination
   */
  public String getDestination() {
    return destination;
  }

  public Pair<String, Integer> getFactoryKey() {
    return Pair.of(connectionFactoryId, sessionMode);
  }

  /**
   *
   * @return the serialization
   */
  public SerializationSchema getSerialization() {
    return serialization;
  }

  /**
   *
   * @return the sessionMode
   */
  public int getSessionMode() {
    return sessionMode;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (connectionFactoryId == null ? 0 : connectionFactoryId.hashCode());
    result = prime * result + (destination == null ? 0 : destination.hashCode());
    result = prime * result + (multicast ? 1231 : 1237);
    return result;
  }

  /**
   *
   * @return the multicast
   */
  public boolean isMulticast() {
    return multicast;
  }
}
