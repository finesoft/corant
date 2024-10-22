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

import static org.corant.context.Beans.findNamed;
import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Strings.EMPTY;
import static org.corant.shared.util.Strings.isNotBlank;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;
import jakarta.jms.CompletionListener;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.XAConnectionFactory;
import jakarta.jms.XAJMSContext;
import jakarta.transaction.Transactional.TxType;
import org.corant.modules.jms.marshaller.MessageMarshaller;
import org.corant.modules.jta.shared.SynchronizationAdapter;
import org.corant.modules.jta.shared.TransactionService;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Compressors;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午8:18:33
 */
public class MessageSenderTemplate extends DefaultMessageSender {

  static final Logger logger = Logger.getLogger(MessageSenderTemplate.class.getName());

  protected TxType txType;
  protected String replyTo;
  protected boolean multicastReplyTo;
  protected int priority = -1;
  protected CompletionListener completionListener;
  protected int sessionMode;
  protected Boolean disableMessageId;
  protected Boolean disableMessageTimestamp;
  protected String jmsType;
  protected String clientId;
  protected Object correlationId;
  protected ExceptionListener exceptionListener;

  public MessageSenderTemplate() {
    connectionFactoryId = EMPTY;
  }

  public static MessageSenderTemplate use(String connectionFactoryId) {
    return new MessageSenderTemplate().connectionFactoryId(connectionFactoryId);
  }

  /**
   * Sets the client identifier for the JMSContext's connection.
   *
   * @param clientId the unique client identifier
   * @see JMSContext#setClientID(String)
   */
  public MessageSenderTemplate clientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

  /**
   * Sets the connection factory id that may be retrieved from configuration properties
   *
   * @param connectionFactoryId the connection factory id
   */
  public MessageSenderTemplate connectionFactoryId(String connectionFactoryId) {
    this.connectionFactoryId = connectionFactoryId;
    return this;
  }

  /**
   * Sets the minimum length of time in milliseconds that must elapse after a message is sent before
   * the Jakarta Messaging provider may deliver the message to a consumer.
   *
   * @param deliveryDelay the delivery delay in milliseconds.
   * @see JMSProducer#setDeliveryDelay(long)
   */

  public MessageSenderTemplate deliveryDelay(long deliveryDelay) {
    this.deliveryDelay = deliveryDelay;
    return this;
  }

  /**
   * Specifies the delivery mode of messages that are sent using this JMSProducer
   * <p>
   * Delivery mode is set to PERSISTENT by default.
   *
   * @param deliveryMode the message delivery mode to be used; legal values are
   *        {@code DeliveryMode.NON_PERSISTENT} and {@code DeliveryMode.PERSISTENT}
   * @see JMSProducer#setDeliveryMode(int)
   */
  public MessageSenderTemplate deliveryMode(int deliveryMode) {
    this.deliveryMode = deliveryMode;
    return this;
  }

  /**
   * Sets the message delivery destination
   *
   * @param destination the message delivery destination name
   */
  public MessageSenderTemplate destination(String destination) {
    this.destination = shouldNotBlank(destination);
    return this;
  }

  /**
   * Specifies whether message IDs may be disabled for messages that are sent using current
   * {@code JMSProducer}
   *
   * @param disableMessageId indicates whether message IDs may be disabled
   * @see JMSProducer#setDisableMessageID(boolean)
   */
  public MessageSenderTemplate disableMessageId(boolean disableMessageId) {
    this.disableMessageId = disableMessageId;
    return this;
  }

  /**
   * Specifies whether message time stamps may be disabled for messages that are sent using current
   * {@code JMSProducer}.
   *
   * @param disableMessageTimestamp indicates whether message time stamps may be disabled
   * @see JMSProducer#setDisableMessageTimestamp(boolean)
   */
  public MessageSenderTemplate disableMessageTimestamp(boolean disableMessageTimestamp) {
    this.disableMessageTimestamp = disableMessageTimestamp;
    return this;
  }

  /**
   * Sets an exception listener for the JMSContext's connection.
   *
   * @param exceptionListener the exception listener
   * @see JMSContext#setExceptionListener(ExceptionListener)
   */
  public MessageSenderTemplate exceptionListener(ExceptionListener exceptionListener) {
    this.exceptionListener = exceptionListener;
    return this;
  }

  /**
   * Specifies that messages sent using this JMSProducer will have their JMSCorrelationID header
   * value set to the specified correlation ID, where correlation ID is specified as an array of
   * bytes.
   *
   * @param correlationId the JMSCorrelationID
   * @see JMSProducer#setJMSCorrelationIDAsBytes(byte[])
   */
  public MessageSenderTemplate jmsCorrelationId(byte[] correlationId) {
    this.correlationId = correlationId;
    return this;
  }

  /**
   * Specifies that messages sent using this JMSProducer will have their JMSCorrelationID header
   * value set to the specified correlation ID, where correlation ID is specified as a String.
   *
   * @param correlationId the message ID of a message being referred to
   * @see JMSProducer#setJMSCorrelationID(String)
   */
  public MessageSenderTemplate jmsCorrelationId(String correlationId) {
    this.correlationId = correlationId;
    return this;
  }

  /**
   * Specifies that messages sent using this JMSProducer will have their JMSType header value set to
   * the specified message type.
   *
   * @param jmsType the message type
   * @see JMSProducer#setJMSType(String)
   */
  public MessageSenderTemplate jmsType(String jmsType) {
    this.jmsType = jmsType;
    return this;
  }

  /**
   * Specifies whether the message delivery destination is JMS queue or topic
   *
   * @param multicast true means topic false means queue default is false
   * @see #destination(String)
   */
  public MessageSenderTemplate multicast(boolean multicast) {
    this.multicast = multicast;
    return this;
  }

  /**
   * Specifies the priority of messages that are sent using current JMSProducer.
   *
   * @param priority the message priority to be used; must be a value between 0 and 9
   * @see JMSProducer#setPriority(int)
   */
  public MessageSenderTemplate priority(int priority) {
    this.priority = priority;
    return this;
  }

  /**
   * Specifies that messages sent using current JMSProducer will have the specified properties.
   *
   * @param properties the properties
   * @see JMSProducer#setProperty(String, Object)
   */
  public MessageSenderTemplate properties(Map<String, Object> properties) {
    if (isNotEmpty(properties)) {
      this.properties.clear();
      this.properties.putAll(properties);
    }
    return this;
  }

  /**
   * Sets the message delivery queue name
   *
   * @param queue the queue name
   */
  public MessageSenderTemplate queue(String queue) {
    destination = shouldNotBlank(queue);
    multicast = false;
    return this;
  }

  /**
   * Specifies that messages sent using current JMSProducer will have their JMSReplyTo header value
   * set to the specified destination.
   *
   * @param destination the destination name
   * @param multicast whether the destination is topic or queue
   * @see JMSProducer#setJMSReplyTo(jakarta.jms.Destination)
   */
  public MessageSenderTemplate replyTo(String destination, boolean multicast) {
    replyTo = destination;
    multicastReplyTo = multicast;
    return this;
  }

  /**
   * Sets the session model that use for JMSContext creating.
   *
   * @param sessionMode indicates which of four possible session modes will be used.
   * @see ConnectionFactory#createContext(int)
   */
  public MessageSenderTemplate sessionMode(int sessionMode) {
    this.sessionMode = sessionMode;
    return this;
  }

  /**
   * Sets the asynchronous {@code CompletionListener}
   *
   * @param completionListener If asynchronous send behaviour is required, this should be set to a
   *        CompletionListener to be notified when the sending has completed. If synchronous send
   *        behaviour is required, this should be set to {@code null}
   * @see JMSProducer#setAsync(CompletionListener)
   */
  public MessageSenderTemplate setAsync(CompletionListener completionListener) {
    this.completionListener = completionListener;
    return this;
  }

  /**
   * Send the given message stream to the specified destination
   *
   * @param marshallerName the name of message marshaller, the default marshaller is java object
   *        serialization
   * @param messages the message stream
   *
   * @see MessageMarshaller
   * @see MessageMarshaller#serialize(JMSContext, Object)
   */
  public void streamingSend(String marshallerName, Stream<?> messages) {
    if (txType != null) {
      final XAJMSContext ctx = ((XAConnectionFactory) connectionFactory()).createXAContext();
      TransactionService.actuator()
          .synchronization(SynchronizationAdapter.afterCompletion(ctx::close)).txType(txType)
          .run(() -> {
            TransactionService.enlistXAResourceToCurrentTransaction(ctx.getXAResource());
            doStreamingSend(ctx, messages, marshallerName);
          });
    } else {
      JMSContext ctx = null;
      try {
        ctx = connectionFactory().createContext(sessionMode);
        doStreamingSend(ctx, messages, marshallerName);
        afterDispatched(ctx, false);
      } catch (RuntimeException e) {
        afterDispatched(ctx, true);
        throw e;
      } finally {
        if (ctx != null) {
          ctx.close();
        }
      }
    }
  }

  /**
   * Specifies the time to live of messages that are sent using current JMSProducer. This is used to
   * determine the expiration time of a message.
   *
   * @param timeToLive the message time to live to be used, in milliseconds; a value of zero means
   *        that a message never expires.
   * @see JMSProducer#setTimeToLive(long)
   */
  public MessageSenderTemplate timeToLive(long timeToLive) {
    this.timeToLive = timeToLive;
    return this;
  }

  /**
   * Sets the message delivery topic name
   *
   * @param topic the topic name
   */
  public MessageSenderTemplate topic(String topic) {
    destination = shouldNotBlank(topic);
    multicast = true;
    return this;
  }

  /**
   * Sets the message delivery transaction type, use for JTA/XA message delivery.
   *
   * @param txType the transaction type
   */
  public MessageSenderTemplate txType(TxType txType) {
    this.txType = txType;
    return this;
  }

  /**
   * Compress and send the given input stream message.
   *
   * @param message the input stream message
   * @throws IOException is error occur
   * @see Compressors#compress(InputStream, java.io.OutputStream)
   */
  public void zippedAndSend(InputStream message) throws IOException {
    try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
      Compressors.compress(message, buffer);
      byte[] bytes = buffer.toByteArray();
      sendBytes(bytes);
    }
  }

  protected void afterDispatched(JMSContext ctx, boolean occurError) {
    if (ctx != null) {
      if (!occurError) {
        if (sessionMode == JMSContext.CLIENT_ACKNOWLEDGE) {
          ctx.acknowledge();
        } else if (sessionMode == JMSContext.SESSION_TRANSACTED) {
          ctx.commit();
        }
      } else if (sessionMode == JMSContext.CLIENT_ACKNOWLEDGE) {
        ctx.recover();
      } else if (sessionMode == JMSContext.SESSION_TRANSACTED) {
        ctx.rollback();
      }
    }
  }

  @Override
  protected void configure(JMSContext jmsc, JMSProducer producer) {
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
    if (disableMessageId != null) {
      producer.setDisableMessageID(disableMessageId);
    }
    if (disableMessageTimestamp != null) {
      producer.setDisableMessageTimestamp(disableMessageTimestamp);
    }
    if (isNotBlank(jmsType)) {
      producer.setJMSType(jmsType);
    }
    if (correlationId != null) {
      if (correlationId instanceof String cid) {
        producer.setJMSCorrelationID(cid);
      } else if (correlationId instanceof byte[] bcid) {
        producer.setJMSCorrelationIDAsBytes(bcid);
      }
    }
    if (isNotBlank(clientId)) {
      jmsc.setClientID(clientId);
    }
    if (exceptionListener != null) {
      jmsc.setExceptionListener(exceptionListener);
    }
    super.configure(jmsc, producer);
  }

  protected ConnectionFactory connectionFactory() {
    return findNamed(ConnectionFactory.class, connectionFactoryId).orElseThrow(
        () -> new CorantRuntimeException("Can not find any JMS connection factory for %s.",
            connectionFactoryId));
  }

  @Override
  protected void doSend(String marshallerName, Object... messages) {
    if (isEmpty(messages)) {
      return;
    }
    if (txType != null) {
      // try (XAJMSContext ctx = ((XAConnectionFactory) connectionFactory()).createXAContext()) {
      final XAJMSContext ctx = ((XAConnectionFactory) connectionFactory()).createXAContext();
      TransactionService.actuator()
          .synchronization(SynchronizationAdapter.afterCompletion(ctx::close)).txType(txType)
          .run(() -> {
            TransactionService.enlistXAResourceToCurrentTransaction(ctx.getXAResource());
            doSend(ctx, marshallerName, messages);
          });
      // }
    } else {
      JMSContext ctx = null;
      try {
        ctx = connectionFactory().createContext(sessionMode);
        doSend(ctx, marshallerName, messages);
        afterDispatched(ctx, false);
      } catch (RuntimeException e) {
        afterDispatched(ctx, true);
        throw e;
      } finally {
        if (ctx != null) {
          ctx.close();
        }
      }
    }
  }

}
