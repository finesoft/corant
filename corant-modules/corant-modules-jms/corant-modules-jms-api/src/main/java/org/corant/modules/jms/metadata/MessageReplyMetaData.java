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
package org.corant.modules.jms.metadata;

import static org.corant.modules.jms.metadata.MetaDataPropertyResolver.getBoolean;
import static org.corant.modules.jms.metadata.MetaDataPropertyResolver.getInt;
import static org.corant.modules.jms.metadata.MetaDataPropertyResolver.getString;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Lists.listOf;
import java.lang.reflect.AnnotatedElement;
import java.util.LinkedHashSet;
import java.util.Set;
import org.corant.modules.jms.annotation.MessageReply;

/**
 * corant-modules-jms-api
 *
 * @author bingo 下午4:14:39
 *
 */
public class MessageReplyMetaData {

  private final int deliveryMode;

  private final String destination;

  private final String marshaller;

  private final boolean multicast;

  public MessageReplyMetaData(int deliveryMode, String destination, String marshaller,
      boolean multicast) {
    this.deliveryMode = deliveryMode;
    this.destination = MetaDataPropertyResolver.get(destination, String.class);
    this.marshaller = MetaDataPropertyResolver.get(marshaller, String.class);
    this.multicast = multicast;
  }

  public static Set<MessageReplyMetaData> from(AnnotatedElement clazz) {
    return of(shouldNotNull(clazz).getAnnotationsByType(MessageReply.class));
  }

  public static MessageReplyMetaData of(MessageReply annotation) {
    shouldNotNull(annotation);
    return new MessageReplyMetaData(getInt(annotation.deliveryMode()),
        getString(annotation.destination()), getString(annotation.marshaller()),
        getBoolean(annotation.multicast()));
  }

  public static Set<MessageReplyMetaData> of(MessageReply[] annotations) {
    Set<MessageReplyMetaData> metas = new LinkedHashSet<>();
    listOf(annotations).stream().map(MessageReplyMetaData::of).forEach(metas::add);
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
    MessageReplyMetaData other = (MessageReplyMetaData) obj;
    if (deliveryMode != other.deliveryMode) {
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

  public int getDeliveryMode() {
    return deliveryMode;
  }

  public String getDestination() {
    return destination;
  }

  public String getMarshaller() {
    return marshaller;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + deliveryMode;
    result = prime * result + (destination == null ? 0 : destination.hashCode());
    return prime * result + (multicast ? 1231 : 1237);
  }

  public boolean isMulticast() {
    return multicast;
  }

}
