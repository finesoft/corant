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
package org.corant.suites.jms.shared.context;

import java.io.Serializable;
import javax.jms.BytesMessage;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.XAJMSContext;
import javax.transaction.xa.XAResource;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.jms.shared.context.JMSContextManager.RsJMSContextManager;
import org.corant.suites.jms.shared.context.JMSContextManager.TsJMSContextManager;
import org.corant.suites.jta.shared.TransactionService;

/**
 * corant-suites-jms-artemis
 *
 * <pre>
 * If an injected JMSContext is used in a JTA transaction (both bean-managed and container-managed),
 * its scope will be that of the transaction. This means that: The JMSContext object will be
 * automatically created the first time it is used within the transaction.
 *
 * The JMSContext object will be automatically closed when the transaction is committed.
 *
 * If, within the same JTA transaction, different beans, or different methods within the same bean,
 * use an injected JMSContext which is injected using identical annotations then they will all share
 * the same JMSContext object.
 *
 * If an injected JMSContext is used when there is no JTA transaction then its scope will be the
 * existing CDI scope @RequestScoped. This means that: The JMSContext object will be created the
 * first time it is used within a request.
 *
 * The JMSContext object will be closed when the request ends.
 *
 * If, within the same request, different beans, or different methods within the same bean, use an
 * injected JMSContext which is injected using identical annotations then they will all share the
 * same JMSContext object.
 *
 * If injected JMSContext is used both in a JTA transaction and outside a JTA transaction then
 * separate JMSContext objects will be used, with a separate JMSContext object being used for each
 * JTA transaction as described above.
 * </pre>
 *
 * {@link <a href="https://javaee.github.io/jms-spec/pages/JMSContextScopeProposalsv4p4">Proposed
 * changes to JMSContext to support injection (Option 4)</a>} <br/>
 * {@link <a href="https://javaee.github.io/jms-spec/pages/JMSContextScopeProposalsv4p1">Injection
 * of JMSContext objects - Proposals (version 4)</a>}
 *
 * @author bingo 下午5:24:17
 *
 */
public class ExtendedJMSContext implements JMSContext, Serializable {

  private static final long serialVersionUID = 6221324367688591392L;

  final JMSContextKey key;

  final RsJMSContextManager reqCtxManager;

  final TsJMSContextManager txCtxManager;

  /**
   * @param key
   * @param reqCtxManager
   * @param txCtxManager
   */
  public ExtendedJMSContext(JMSContextKey key, RsJMSContextManager reqCtxManager,
      TsJMSContextManager txCtxManager) {
    super();
    this.key = key;
    this.reqCtxManager = reqCtxManager;
    this.txCtxManager = txCtxManager;
  }

  @Override
  public void acknowledge() {
    context().acknowledge();
  }

  @Override
  public void close() {
    JMSContext ctx = context();
    // When manually called close() then delistResource from current transaction if necessarily
    if (ctx != null && key.isXa() && TransactionService.isCurrentTransactionActive()) {
      try {
        TransactionService.delistXAResourceFromCurrentTransaction(
            XAJMSContext.class.cast(ctx).getXAResource(), XAResource.TMSUCCESS);
      } catch (Exception e) {
        throw new CorantRuntimeException(e);
      }
    }
    context().close();
  }

  @Override
  public void commit() {
    context().commit();
  }

  @Override
  public QueueBrowser createBrowser(final Queue queue) {
    return context().createBrowser(queue);
  }

  @Override
  public QueueBrowser createBrowser(final Queue queue, final String messageSelector) {
    return context().createBrowser(queue, messageSelector);
  }

  @Override
  public BytesMessage createBytesMessage() {
    return context().createBytesMessage();
  }

  @Override
  public JMSConsumer createConsumer(final Destination destination) {
    return context().createConsumer(destination);
  }

  @Override
  public JMSConsumer createConsumer(final Destination destination, final String messageSelector) {
    return context().createConsumer(destination, messageSelector);
  }

  @Override
  public JMSConsumer createConsumer(final Destination destination, final String messageSelector,
      final boolean noLocal) {
    return context().createConsumer(destination, messageSelector, noLocal);
  }

  @Override
  public JMSContext createContext(final int sessionMode) {
    return context().createContext(sessionMode);
  }

  @Override
  public JMSConsumer createDurableConsumer(final Topic topic, final String name) {
    return context().createDurableConsumer(topic, name);
  }

  @Override
  public JMSConsumer createDurableConsumer(final Topic topic, final String name,
      final String messageSelector, final boolean noLocal) {
    return context().createDurableConsumer(topic, name, messageSelector, noLocal);
  }

  @Override
  public MapMessage createMapMessage() {
    return context().createMapMessage();
  }

  @Override
  public Message createMessage() {
    return context().createMessage();
  }

  @Override
  public ObjectMessage createObjectMessage() {
    return context().createObjectMessage();
  }

  @Override
  public ObjectMessage createObjectMessage(final Serializable object) {
    return context().createObjectMessage(object);
  }

  @Override
  public JMSProducer createProducer() {
    return context().createProducer();
  }

  @Override
  public Queue createQueue(final String queueName) {
    return context().createQueue(queueName);
  }

  @Override
  public JMSConsumer createSharedConsumer(final Topic topic, final String sharedSubscriptionName) {
    return context().createSharedConsumer(topic, sharedSubscriptionName);
  }

  @Override
  public JMSConsumer createSharedConsumer(final Topic topic, final String sharedSubscriptionName,
      final String messageSelector) {
    return context().createSharedConsumer(topic, sharedSubscriptionName, messageSelector);
  }

  @Override
  public JMSConsumer createSharedDurableConsumer(final Topic topic, final String name) {
    return context().createSharedDurableConsumer(topic, name);
  }

  @Override
  public JMSConsumer createSharedDurableConsumer(final Topic topic, final String name,
      final String messageSelector) {
    return context().createSharedDurableConsumer(topic, name, messageSelector);
  }

  @Override
  public StreamMessage createStreamMessage() {
    return context().createStreamMessage();
  }

  @Override
  public TemporaryQueue createTemporaryQueue() {
    return context().createTemporaryQueue();
  }

  @Override
  public TemporaryTopic createTemporaryTopic() {
    return context().createTemporaryTopic();
  }

  @Override
  public TextMessage createTextMessage() {
    return context().createTextMessage();
  }

  @Override
  public TextMessage createTextMessage(final String text) {
    return context().createTextMessage(text);
  }

  @Override
  public Topic createTopic(final String topicName) {
    return context().createTopic(topicName);
  }

  @Override
  public boolean getAutoStart() {
    return context().getAutoStart();
  }

  @Override
  public String getClientID() {
    return context().getClientID();
  }

  @Override
  public ExceptionListener getExceptionListener() {
    return context().getExceptionListener();
  }

  @Override
  public ConnectionMetaData getMetaData() {
    return context().getMetaData();
  }

  @Override
  public int getSessionMode() {
    return context().getSessionMode();
  }

  @Override
  public boolean getTransacted() {
    return context().getTransacted();
  }

  @Override
  public void recover() {
    context().recover();
  }

  @Override
  public void rollback() {
    context().rollback();
  }

  @Override
  public void setAutoStart(final boolean autoStart) {
    context().setAutoStart(autoStart);
  }

  @Override
  public void setClientID(final String clientID) {
    context().setClientID(clientID);
  }

  @Override
  public void setExceptionListener(final ExceptionListener listener) {
    context().setExceptionListener(listener);
  }

  @Override
  public void start() {
    context().start();
  }

  @Override
  public void stop() {
    context().stop();
  }

  @Override
  public void unsubscribe(final String name) {
    context().unsubscribe(name);
  }

  private synchronized JMSContext context() {
    if (TransactionService.isCurrentTransactionActive()) {
      return txCtxManager.computeIfAbsent(key, JMSContextKey::create);
    }
    return reqCtxManager.computeIfAbsent(key, JMSContextKey::create);
  }
}
