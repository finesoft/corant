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
package org.corant.modules.jms.shared.send;

import static org.corant.context.Instances.findNamed;
import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Strings.EMPTY;
import static org.corant.shared.util.Strings.isNotBlank;
import java.util.Map;
import java.util.logging.Logger;
import javax.jms.CompletionListener;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.XAConnectionFactory;
import javax.jms.XAJMSContext;
import org.corant.modules.jms.shared.annotation.MessageSerialization.SerializationSchema;
import org.corant.modules.jms.shared.send.MessageDispatcher.MessageDispatcherImpl;
import org.corant.modules.jta.shared.TransactionService;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午8:18:33
 *
 */
public class SimpleMessageDispatcher extends MessageDispatcherImpl {

  static final Logger logger = Logger.getLogger(SimpleMessageDispatcher.class.getName());

  protected boolean xa;
  protected String replyTo;
  protected boolean multicastReplyTo;
  protected int priority = -1;
  protected CompletionListener completionListener;

  public SimpleMessageDispatcher() {
    connectionFactoryId = EMPTY;
  }

  public static SimpleMessageDispatcher use(String connectionFactoryId) {
    return new SimpleMessageDispatcher().connectionFactoryId(connectionFactoryId);
  }

  public SimpleMessageDispatcher connectionFactoryId(String connectionFactoryId) {
    this.connectionFactoryId = connectionFactoryId;
    return this;
  }

  public SimpleMessageDispatcher deliveryDelay(long deliveryDelay) {
    this.deliveryDelay = deliveryDelay;
    return this;
  }

  public SimpleMessageDispatcher deliveryMode(int deliveryMode) {
    this.deliveryMode = deliveryMode;
    return this;
  }

  public SimpleMessageDispatcher destination(String destination) {
    this.destination = shouldNotBlank(destination);
    return this;
  }

  public SimpleMessageDispatcher multicast(boolean multicast) {
    this.multicast = multicast;
    return this;
  }

  public SimpleMessageDispatcher priority(int priority) {
    this.priority = priority;
    return this;
  }

  public SimpleMessageDispatcher properties(Map<String, Object> properties) {
    if (isNotEmpty(properties)) {
      this.properties.clear();
      this.properties.putAll(properties);
    }
    return this;
  }

  public SimpleMessageDispatcher replyTo(String destination, boolean multicast) {
    replyTo = destination;
    multicastReplyTo = multicast;
    return this;
  }

  public SimpleMessageDispatcher sessionMode(int sessionMode) {
    this.sessionMode = sessionMode;
    return this;
  }

  public SimpleMessageDispatcher setAsync(CompletionListener completionListener) {
    this.completionListener = completionListener;
    return this;
  }

  public SimpleMessageDispatcher timeToLive(long timeToLive) {
    this.timeToLive = timeToLive;
    return this;
  }

  public SimpleMessageDispatcher xa(boolean xa) {
    this.xa = xa;
    return this;
  }

  @Override
  protected void configurateProducer(JMSContext jmsc, JMSProducer producer) {
    if (isNotBlank(replyTo)) {
      producer
          .setJMSReplyTo(multicastReplyTo ? jmsc.createTopic(replyTo) : jmsc.createQueue(replyTo));
    }
    if (priority > -1) {
      producer.setPriority(priority);
    }
    if (completionListener != null) {
      producer.setAsync(completionListener);
    }
    super.configurateProducer(jmsc, producer);
  }

  @Override
  protected void doDispatch(Object message, SerializationSchema serializationSchema) {
    if (xa) {
      try (XAJMSContext ctx = ((XAConnectionFactory) connectionFactory()).createXAContext()) {
        TransactionService.actuator().required().run(() -> {
          TransactionService.enlistXAResourceToCurrentTransaction(ctx.getXAResource());
          doDispatch(ctx, serializationSchema, message);
        });
      }
    } else {
      try (JMSContext ctx = connectionFactory().createContext(sessionMode)) {
        doDispatch(ctx, serializationSchema, message);
      }
    }
  }

  ConnectionFactory connectionFactory() {
    return findNamed(ConnectionFactory.class, connectionFactoryId).orElseThrow(
        () -> new CorantRuntimeException("Can not find any JMS connection factory for %s.",
            connectionFactoryId));
  }

}
