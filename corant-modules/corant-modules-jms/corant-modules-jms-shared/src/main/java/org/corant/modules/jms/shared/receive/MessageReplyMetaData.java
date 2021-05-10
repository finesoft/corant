/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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

import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Streams.streamOf;
import org.corant.modules.jms.shared.annotation.MessageReply;
import org.corant.modules.jms.shared.annotation.MessageSerialization.SerializationSchema;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午5:55:15
 *
 */
public class MessageReplyMetaData {

  public static final MessageReplyMetaData[] EMPTY_ARRAY = new MessageReplyMetaData[0];

  private final int deliveryMode;// () default DeliveryMode.PERSISTENT;

  private final String destination;// ();

  private final boolean multicast;// () default false;

  private final SerializationSchema serialization;// () default SerializationSchema.JSON_STRING;

  protected MessageReplyMetaData(int deliveryMode, String destination, boolean multicast,
      SerializationSchema serialization) {
    this.deliveryMode = deliveryMode;
    this.destination = destination;
    this.multicast = multicast;
    this.serialization = serialization;
  }

  protected MessageReplyMetaData(MessageReply ann) {
    this(ann.deliveryMode(), ann.destination(), ann.multicast(), ann.serialization());
  }

  public static MessageReplyMetaData[] from(MessageReply... anns) {
    if (isNotEmpty(anns)) {
      return streamOf(anns).map(MessageReplyMetaData::new).toArray(MessageReplyMetaData[]::new);
    }
    return EMPTY_ARRAY;
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
    MessageReplyMetaData other = (MessageReplyMetaData) obj;
    if (destination == null) {
      if (other.destination != null) {
        return false;
      }
    } else if (!destination.equals(other.destination)) {
      return false;
    }
    return true;
  }

  public int getDeliveryMode() {
    return deliveryMode;
  }

  public String getDestination() {
    return destination;
  }

  public SerializationSchema getSerialization() {
    return serialization;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (destination == null ? 0 : destination.hashCode());
    return result;
  }

  public boolean isMulticast() {
    return multicast;
  }

}
