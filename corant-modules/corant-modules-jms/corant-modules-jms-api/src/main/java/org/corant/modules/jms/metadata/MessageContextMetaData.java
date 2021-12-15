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
import static org.corant.shared.util.Assertions.shouldNotNull;
import org.corant.modules.jms.annotation.MessageContext;

/**
 * corant-modules-jms-api
 *
 * @author bingo 下午4:05:29
 *
 */
public class MessageContextMetaData {

  private final String connectionFactoryId;

  private final boolean dupsOkAck;

  public MessageContextMetaData(String connectionFactoryId, boolean dupsOkAck) {
    this.connectionFactoryId = MetaDataPropertyResolver.get(connectionFactoryId, String.class);
    this.dupsOkAck = dupsOkAck;
  }

  public static MessageContextMetaData of(MessageContext ctx) {
    shouldNotNull(ctx);
    return new MessageContextMetaData(ctx.connectionFactoryId(), getBoolean(ctx.dupsOkAck()));
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
    MessageContextMetaData other = (MessageContextMetaData) obj;
    if (connectionFactoryId == null) {
      if (other.connectionFactoryId != null) {
        return false;
      }
    } else if (!connectionFactoryId.equals(other.connectionFactoryId)) {
      return false;
    }
    if (dupsOkAck != other.dupsOkAck) {
      return false;
    }
    return true;
  }

  public String getConnectionFactoryId() {
    return connectionFactoryId;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (connectionFactoryId == null ? 0 : connectionFactoryId.hashCode());
    return prime * result + (dupsOkAck ? 1231 : 1237);
  }

  public boolean isDupsOkAck() {
    return dupsOkAck;
  }

}
