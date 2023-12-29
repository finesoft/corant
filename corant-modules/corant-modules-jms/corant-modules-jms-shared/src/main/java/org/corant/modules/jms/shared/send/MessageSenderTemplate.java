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
  protected Boolean disableMessageID;
  protected Boolean disableMessageTimestamp;
  protected String jmsType;
  protected String clientId;
  protected ExceptionListener exceptionListener;

  public MessageSenderTemplate() {
    connectionFactoryId = EMPTY;
  }

  public static MessageSenderTemplate use(String connectionFactoryId) {
    return new MessageSenderTemplate().connectionFactoryId(connectionFactoryId);
  }

  public MessageSenderTemplate clientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

  public MessageSenderTemplate connectionFactoryId(String connectionFactoryId) {
    this.connectionFactoryId = connectionFactoryId;
    return this;
  }

  public MessageSenderTemplate deliveryDelay(long deliveryDelay) {
    this.deliveryDelay = deliveryDelay;
    return this;
  }

  public MessageSenderTemplate deliveryMode(int deliveryMode) {
    this.deliveryMode = deliveryMode;
    return this;
  }

  public MessageSenderTemplate destination(String destination) {
    this.destination = shouldNotBlank(destination);
    return this;
  }

  public MessageSenderTemplate disableMessageID(boolean disableMessageID) {
    this.disableMessageID = disableMessageID;
    return this;
  }

  public MessageSenderTemplate disableMessageTimestamp(boolean disableMessageTimestamp) {
    this.disableMessageTimestamp = disableMessageTimestamp;
    return this;
  }

  public MessageSenderTemplate exceptionListener(ExceptionListener exceptionListener) {
    this.exceptionListener = exceptionListener;
    return this;
  }

  public MessageSenderTemplate jmsType(String jmsType) {
    this.jmsType = jmsType;
    return this;
  }

  public MessageSenderTemplate multicast(boolean multicast) {
    this.multicast = multicast;
    return this;
  }

  public MessageSenderTemplate priority(int priority) {
    this.priority = priority;
    return this;
  }

  public MessageSenderTemplate properties(Map<String, Object> properties) {
    if (isNotEmpty(properties)) {
      this.properties.clear();
      this.properties.putAll(properties);
    }
    return this;
  }

  public MessageSenderTemplate queue(String queue) {
    destination = shouldNotBlank(queue);
    multicast = false;
    return this;
  }

  public MessageSenderTemplate replyTo(String destination, boolean multicast) {
    replyTo = destination;
    multicastReplyTo = multicast;
    return this;
  }

  public MessageSenderTemplate sessionMode(int sessionMode) {
    this.sessionMode = sessionMode;
    return this;
  }

  public MessageSenderTemplate setAsync(CompletionListener completionListener) {
    this.completionListener = completionListener;
    return this;
  }

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

  public MessageSenderTemplate timeToLive(long timeToLive) {
    this.timeToLive = timeToLive;
    return this;
  }

  public MessageSenderTemplate topic(String topic) {
    destination = shouldNotBlank(topic);
    multicast = true;
    return this;
  }

  public MessageSenderTemplate txType(TxType txType) {
    this.txType = txType;
    return this;
  }

  public void zippedAndSend(InputStream message) throws IOException {
    try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
      Compressors.compress(message, buffer);
      byte[] bytes = buffer.toByteArray();
      send(bytes);
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
    if (disableMessageID != null) {
      producer.setDisableMessageID(disableMessageID);
    }
    if (disableMessageTimestamp != null) {
      producer.setDisableMessageTimestamp(disableMessageTimestamp);
    }
    if (isNotBlank(jmsType)) {
      producer.setJMSType(jmsType);
    }
    if (isNotBlank(clientId)) {
      jmsc.setClientID(clientId);
    }
    if (exceptionListener != null) {
      jmsc.setExceptionListener(exceptionListener);
    }
    super.configure(jmsc, producer);
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

  void afterDispatched(JMSContext ctx, boolean occurrError) {
    if (ctx != null) {
      if (!occurrError) {
        if (sessionMode == JMSContext.CLIENT_ACKNOWLEDGE) {
          ctx.acknowledge();
        } else if (sessionMode == JMSContext.SESSION_TRANSACTED) {
          ctx.commit();
        }
      } else {
        if (sessionMode == JMSContext.CLIENT_ACKNOWLEDGE) {
          ctx.recover();
        } else if (sessionMode == JMSContext.SESSION_TRANSACTED) {
          ctx.rollback();
        }
      }
    }
  }

  ConnectionFactory connectionFactory() {
    return findNamed(ConnectionFactory.class, connectionFactoryId).orElseThrow(
        () -> new CorantRuntimeException("Can not find any JMS connection factory for %s.",
            connectionFactoryId));
  }

}
