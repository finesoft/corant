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
import static org.corant.modules.jms.metadata.MetaDataPropertyResolver.getLong;
import static org.corant.modules.jms.metadata.MetaDataPropertyResolver.getString;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Lists.newArrayList;
import java.util.Collection;
import java.util.Collections;
import org.corant.modules.jms.annotation.MessageSend;

/**
 * corant-modules-jms-api
 *
 * @author bingo 下午5:12:07
 *
 */
public class MessageSendMetaData {

  private final long deliveryDelay;

  private final int deliveryMode;

  private final MessageDestinationMetaData destination;

  private final boolean dupsOkAck;

  private final String marshaller;

  private final Collection<MessagePropertyMetaData> properties;

  private final long timeToLive;

  public MessageSendMetaData(long deliveryDelay, int deliveryMode,
      MessageDestinationMetaData destination, boolean dupsOkAck, String marshaller,
      Collection<MessagePropertyMetaData> properties, long timeToLive) {
    this.deliveryDelay = deliveryDelay;
    this.deliveryMode = deliveryMode;
    this.destination = destination;
    this.dupsOkAck = dupsOkAck;
    this.marshaller = MetaDataPropertyResolver.get(marshaller, String.class);
    this.properties = Collections.unmodifiableList(newArrayList(properties));
    this.timeToLive = timeToLive;
  }

  public static MessageSendMetaData of(MessageSend annotation) {
    shouldNotNull(annotation);
    return new MessageSendMetaData(getLong(annotation.deliveryDelay()),
        getInt(annotation.deliveryMode()), MessageDestinationMetaData.of(annotation.destination()),
        getBoolean(annotation.dupsOkAck()), getString(annotation.marshaller()),
        MessagePropertyMetaData.of(annotation.properties()), getLong(annotation.timeToLive()));
  }

  public long getDeliveryDelay() {
    return deliveryDelay;
  }

  public int getDeliveryMode() {
    return deliveryMode;
  }

  public MessageDestinationMetaData getDestination() {
    return destination;
  }

  public String getMarshaller() {
    return marshaller;
  }

  public Collection<MessagePropertyMetaData> getProperties() {
    return properties;
  }

  public long getTimeToLive() {
    return timeToLive;
  }

  public boolean isDupsOkAck() {
    return dupsOkAck;
  }

}
