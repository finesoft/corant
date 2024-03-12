/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.jms.send;

import static org.corant.shared.util.Assertions.shouldNotEmpty;
import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.Map;
import java.util.Set;
import org.corant.modules.jms.annotation.MessageDestination;
import org.corant.modules.jms.metadata.MessageDestinationMetaData;

/**
 * corant-modules-jms-api
 *
 * @author bingo 11:05:37
 */
public interface MessageDispatcher {

  /**
   * Dispatch a message object, the given message class must have a {@link MessageDestination}
   * annotation.
   *
   * @param message the message to be dispatch
   */
  default void dispatch(Object message) {
    shouldNotNull(message, "Message to dispatch can't null");
    Set<MessageDestinationMetaData> metas = MessageDestinationMetaData.from(message.getClass());
    shouldNotEmpty(metas, "Message to dispatch must have a message destination annotation");
    for (MessageDestinationMetaData meta : metas) {
      dispatch(meta.getConnectionFactoryId(), meta.getName(), meta.isMulticast(), message,
          meta.getProperties());
    }
  }

  default void dispatch(String connectionFactoryId, String destination, boolean multicast,
      Object message) {
    dispatch(connectionFactoryId, destination, multicast, message, null);
  }

  void dispatch(String connectionFactoryId, String destination, boolean multicast, Object message,
      Map<String, Object> properties);

  void dispatchBytes(String connectionFactoryId, String destination, boolean multicast,
      byte[] message, Object... messageProperties);

  void dispatchMap(String connectionFactoryId, String destination, boolean multicast,
      Map<String, Object> message, Object... messageProperties);

  void dispatchText(String connectionFactoryId, String destination, boolean multicast,
      String message, Object... messageProperties);
}
