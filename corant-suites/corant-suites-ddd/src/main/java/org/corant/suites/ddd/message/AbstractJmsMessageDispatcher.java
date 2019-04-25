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
package org.corant.suites.ddd.message;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.ObjectUtils.isNotNull;
import static org.corant.shared.util.StreamUtils.copy;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.transaction.Transactional;

/**
 * corant-suites-ddd
 *
 * @author bingo 下午12:01:54
 *
 */
@ApplicationScoped
@Transactional
public abstract class AbstractJmsMessageDispatcher implements MessageDispatcher {

  @Override
  public void prepare() {
    shouldNotNull(obtainJmsContext(), "Can not find any JMS Context!");
  }

  protected Destination createDestination(boolean multicast, String destination) {
    return multicast ? obtainJmsContext().createTopic(destination)
        : obtainJmsContext().createQueue(destination);
  }

  protected abstract JMSContext obtainJmsContext();

  protected JMSProducer obtainJmsProducer() {
    return obtainJmsContext().createProducer();
  }

  protected void send(boolean multicast, String destination, byte[] payload) {
    if (isNotEmpty(payload)) {
      obtainJmsProducer().send(createDestination(multicast, destination), payload);
    }
  }

  protected void send(boolean multicast, String destination, InputStream payload)
      throws IOException {
    try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
      copy(payload, buffer);
      byte[] bytes = buffer.toByteArray();
      send(multicast, destination, bytes);
    }
  }

  protected void send(boolean multicast, String destination, Map<String, Object> payload) {
    if (isNotEmpty(payload)) {
      obtainJmsProducer().send(createDestination(multicast, destination), payload);
    }
  }

  protected void send(boolean multicast, String destination, Serializable payload) {
    if (isNotNull(payload)) {
      obtainJmsProducer().send(createDestination(multicast, destination), payload);
    }
  }

  protected void send(boolean multicast, String destination, String payload) {
    if (isNotNull(payload)) {
      obtainJmsProducer().send(createDestination(multicast, destination), payload);
    }
  }
}
