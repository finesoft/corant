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
import javax.jms.CompletionListener;
import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.XAConnectionFactory;
import javax.jms.XAJMSContext;
import javax.transaction.Transactional.TxType;
import org.corant.modules.jms.shared.context.SerialSchema;
import org.corant.modules.jms.shared.send.MessageDispatcher.MessageDispatcherImpl;
import org.corant.modules.jta.shared.SynchronizationAdapter;
import org.corant.modules.jta.shared.TransactionService;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Compressors;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午8:18:33
 *
 */
public class MessageDispatcherTemplate extends MessageDispatcherImpl {

  static final Logger logger = Logger.getLogger(MessageDispatcherTemplate.class.getName());

  protected TxType txType;
  protected String replyTo;
  protected boolean multicastReplyTo;
  protected int priority = -1;
  protected CompletionListener completionListener;
  protected int sessionMode;
  protected boolean disableMessageID;
  protected boolean disableMessageTimestamp;
  protected String jmsType;
  protected String clientId;
  protected ExceptionListener exceptionListener;

  public MessageDispatcherTemplate() {
    connectionFactoryId = EMPTY;
  }

  public static MessageDispatcherTemplate use(String connectionFactoryId) {
    return new MessageDispatcherTemplate().connectionFactoryId(connectionFactoryId);
  }

  public MessageDispatcherTemplate clientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

  public MessageDispatcherTemplate connectionFactoryId(String connectionFactoryId) {
    this.connectionFactoryId = connectionFactoryId;
    return this;
  }

  public MessageDispatcherTemplate deliveryDelay(long deliveryDelay) {
    this.deliveryDelay = deliveryDelay;
    return this;
  }

  public MessageDispatcherTemplate deliveryMode(int deliveryMode) {
    this.deliveryMode = deliveryMode;
    return this;
  }

  public MessageDispatcherTemplate destination(String destination) {
    this.destination = shouldNotBlank(destination);
    return this;
  }

  public MessageDispatcherTemplate disableMessageID(boolean disableMessageID) {
    this.disableMessageID = disableMessageID;
    return this;
  }

  public MessageDispatcherTemplate disableMessageTimestamp(boolean disableMessageTimestamp) {
    this.disableMessageTimestamp = disableMessageTimestamp;
    return this;
  }

  public MessageDispatcherTemplate exceptionListener(ExceptionListener exceptionListener) {
    this.exceptionListener = exceptionListener;
    return this;
  }

  public MessageDispatcherTemplate jmsType(String jmsType) {
    this.jmsType = jmsType;
    return this;
  }

  public MessageDispatcherTemplate multicast(boolean multicast) {
    this.multicast = multicast;
    return this;
  }

  public MessageDispatcherTemplate priority(int priority) {
    this.priority = priority;
    return this;
  }

  public MessageDispatcherTemplate properties(Map<String, Object> properties) {
    if (isNotEmpty(properties)) {
      this.properties.clear();
      this.properties.putAll(properties);
    }
    return this;
  }

  public MessageDispatcherTemplate replyTo(String destination, boolean multicast) {
    replyTo = destination;
    multicastReplyTo = multicast;
    return this;
  }

  public MessageDispatcherTemplate sessionMode(int sessionMode) {
    this.sessionMode = sessionMode;
    return this;
  }

  public MessageDispatcherTemplate setAsync(CompletionListener completionListener) {
    this.completionListener = completionListener;
    return this;
  }

  public void streamDispatch(SerialSchema serializationSchema, Stream<?> messages) {
    if (txType != null) {
      final XAJMSContext ctx = ((XAConnectionFactory) connectionFactory()).createXAContext();
      TransactionService.actuator()
          .synchronization(SynchronizationAdapter.afterCompletion(ctx::close)).txType(txType)
          .run(() -> {
            TransactionService.enlistXAResourceToCurrentTransaction(ctx.getXAResource());
            doStreamDispatch(ctx, messages, serializationSchema);
          });
    } else {
      JMSContext ctx = null;
      try {
        ctx = connectionFactory().createContext(sessionMode);
        doStreamDispatch(ctx, messages, serializationSchema);
        doDispatched(ctx, false);
      } catch (RuntimeException e) {
        doDispatched(ctx, true);
        throw e;
      } finally {
        if (ctx != null) {
          ctx.close();
        }
      }
    }
  }

  public MessageDispatcherTemplate timeToLive(long timeToLive) {
    this.timeToLive = timeToLive;
    return this;
  }

  public MessageDispatcherTemplate txType(TxType txType) {
    this.txType = txType;
    return this;
  }

  public void zippedDispatch(InputStream message) throws IOException {
    try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
      Compressors.compress(message, buffer);
      byte[] bytes = buffer.toByteArray();
      dispatch(bytes);
    }
  }

  @Override
  protected void configurate(JMSContext jmsc, JMSProducer producer) {
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
    if (disableMessageID) {
      producer.setDisableMessageID(disableMessageID);
    }
    if (disableMessageTimestamp) {
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
    super.configurate(jmsc, producer);
  }

  @Override
  protected void doDispatch(SerialSchema serializationSchema, Object... messages) {
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
            doDispatch(ctx, serializationSchema, messages);
          });
      // }
    } else {
      JMSContext ctx = null;
      try {
        ctx = connectionFactory().createContext(sessionMode);
        doDispatch(ctx, serializationSchema, messages);
        doDispatched(ctx, false);
      } catch (RuntimeException e) {
        doDispatched(ctx, true);
        throw e;
      } finally {
        if (ctx != null) {
          ctx.close();
        }
      }
    }
  }

  ConnectionFactory connectionFactory() {
    return findNamed(ConnectionFactory.class, connectionFactoryId).orElseThrow(
        () -> new CorantRuntimeException("Can not find any JMS connection factory for %s.",
            connectionFactoryId));
  }

  void doDispatched(JMSContext ctx, boolean occurrError) {
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

}
