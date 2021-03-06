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
package org.corant.modules.ddd;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.time.Instant;

/**
 * corant-modules-ddd-api
 *
 * @author bingo 上午10:41:55
 */
public interface Message extends Serializable {

  interface BinaryMessage extends Message {
    InputStream openStream() throws IOException;
  }

  interface ExchangedMessage extends Message {

    MessageIdentifier getOriginalMessage();

  }

  interface MessageHandling extends Serializable {

    Object getDestination(); // Should we have this property?

    Instant getHandledTime();

    Object getHandler();

    Object getMessageId();

    boolean isSuccess();
  }

  interface MessageIdentifier {

    Serializable getId();

    Object getQueue();

    Serializable getType();

  }

  interface MessageMetadata extends Serializable {

    Instant getOccurredTime();

    Serializable getSource();
  }

}
