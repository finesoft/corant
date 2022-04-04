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
package org.corant.modules.jms.shared.receive;

import static org.corant.shared.util.Objects.defaultObject;
import java.io.Serializable;
import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;
import org.corant.shared.exception.NotSupportedException;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午12:23:02
 *
 */
public class MessageReceivingSession implements Session {

  final Session session;
  final MessageReceivingExecutorConfig config;

  public MessageReceivingSession(Session session, MessageReceivingExecutorConfig config) {
    this.session = session;
    this.config = defaultObject(config, MessageReceivingExecutorConfig.DFLT_INST);
  }

  @Override
  public void close() throws JMSException {
    throw new JMSException("Not support close() on receiving session!");
  }

  @Override
  public void commit() throws JMSException {
    throw new JMSException("Not support commit() on receiving session!");
  }

  @Override
  public QueueBrowser createBrowser(Queue queue) throws JMSException {
    return session.createBrowser(queue);
  }

  @Override
  public QueueBrowser createBrowser(Queue queue, String messageSelector) throws JMSException {
    return session.createBrowser(queue, messageSelector);
  }

  @Override
  public BytesMessage createBytesMessage() throws JMSException {
    return session.createBytesMessage();
  }

  @Override
  public MessageConsumer createConsumer(Destination destination) throws JMSException {
    if (config.isAllowCreateConsumerOnReceivingMethod()) {
      return session.createConsumer(destination);
    }
    throw new JMSException("Not support createConsumer() on receiving session!");
  }

  @Override
  public MessageConsumer createConsumer(Destination destination, String messageSelector)
      throws JMSException {
    if (config.isAllowCreateConsumerOnReceivingMethod()) {
      return session.createConsumer(destination, messageSelector);
    }
    throw new JMSException("Not support createConsumer() on receiving session!");
  }

  @Override
  public MessageConsumer createConsumer(Destination destination, String messageSelector,
      boolean noLocal) throws JMSException {
    if (config.isAllowCreateConsumerOnReceivingMethod()) {
      return session.createConsumer(destination, messageSelector, noLocal);
    }
    throw new JMSException("Not support createConsumer() on receiving session!");
  }

  @Override
  public MessageConsumer createDurableConsumer(Topic topic, String name) throws JMSException {
    if (config.isAllowCreateConsumerOnReceivingMethod()) {
      return session.createDurableConsumer(topic, name);
    }
    throw new JMSException("Not support createDurableConsumer() on receiving session!");
  }

  @Override
  public MessageConsumer createDurableConsumer(Topic topic, String name, String messageSelector,
      boolean noLocal) throws JMSException {
    if (config.isAllowCreateConsumerOnReceivingMethod()) {
      return session.createDurableConsumer(topic, name, messageSelector, noLocal);
    }
    throw new JMSException("Not support createDurableConsumer() on receiving session!");
  }

  @Override
  public TopicSubscriber createDurableSubscriber(Topic topic, String name) throws JMSException {
    if (config.isAllowCreateSubscriberOnReceivingMethod()) {
      return session.createDurableSubscriber(topic, name);
    }
    throw new JMSException("Not support createDurableSubscriber() on receiving session!");
  }

  @Override
  public TopicSubscriber createDurableSubscriber(Topic topic, String name, String messageSelector,
      boolean noLocal) throws JMSException {
    if (config.isAllowCreateSubscriberOnReceivingMethod()) {
      return session.createDurableSubscriber(topic, name, messageSelector, noLocal);
    }
    throw new JMSException("Not support createDurableSubscriber() on receiving session!");
  }

  @Override
  public MapMessage createMapMessage() throws JMSException {
    return session.createMapMessage();
  }

  @Override
  public Message createMessage() throws JMSException {
    return session.createMessage();
  }

  @Override
  public ObjectMessage createObjectMessage() throws JMSException {
    return session.createObjectMessage();
  }

  @Override
  public ObjectMessage createObjectMessage(Serializable object) throws JMSException {
    return session.createObjectMessage(object);
  }

  @Override
  public MessageProducer createProducer(Destination destination) throws JMSException {
    return session.createProducer(destination);
  }

  @Override
  public Queue createQueue(String queueName) throws JMSException {
    return session.createQueue(queueName);
  }

  @Override
  public MessageConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName)
      throws JMSException {
    if (config.isAllowCreateConsumerOnReceivingMethod()) {
      return session.createSharedConsumer(topic, sharedSubscriptionName);
    }
    throw new JMSException("Not support createSharedConsumer() on receiving session!");
  }

  @Override
  public MessageConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName,
      String messageSelector) throws JMSException {
    if (config.isAllowCreateConsumerOnReceivingMethod()) {
      return session.createSharedConsumer(topic, sharedSubscriptionName, messageSelector);
    }
    throw new JMSException("Not support createSharedConsumer() on receiving session!");
  }

  @Override
  public MessageConsumer createSharedDurableConsumer(Topic topic, String name) throws JMSException {
    if (config.isAllowCreateConsumerOnReceivingMethod()) {
      return session.createSharedDurableConsumer(topic, name);
    }
    throw new JMSException("Not support createSharedConsumer() on receiving session!");
  }

  @Override
  public MessageConsumer createSharedDurableConsumer(Topic topic, String name,
      String messageSelector) throws JMSException {
    if (config.isAllowCreateConsumerOnReceivingMethod()) {
      return session.createSharedDurableConsumer(topic, name, messageSelector);
    }
    throw new JMSException("Not support createSharedConsumer() on receiving session!");
  }

  @Override
  public StreamMessage createStreamMessage() throws JMSException {
    return session.createStreamMessage();
  }

  @Override
  public TemporaryQueue createTemporaryQueue() throws JMSException {
    return session.createTemporaryQueue();
  }

  @Override
  public TemporaryTopic createTemporaryTopic() throws JMSException {
    return session.createTemporaryTopic();
  }

  @Override
  public TextMessage createTextMessage() throws JMSException {
    return session.createTextMessage();
  }

  @Override
  public TextMessage createTextMessage(String text) throws JMSException {
    return session.createTextMessage(text);
  }

  @Override
  public Topic createTopic(String topicName) throws JMSException {
    return session.createTopic(topicName);
  }

  @Override
  public int getAcknowledgeMode() throws JMSException {
    return session.getAcknowledgeMode();
  }

  @Override
  public MessageListener getMessageListener() throws JMSException {
    throw new JMSException("Not support getMessageListener() on receiving session!");
  }

  @Override
  public boolean getTransacted() throws JMSException {
    return session.getTransacted();
  }

  @Override
  public void recover() throws JMSException {
    throw new JMSException("Not support recover() receiving session!");
  }

  @Override
  public void rollback() throws JMSException {
    throw new JMSException("Not support rollback() receiving session!");
  }

  @Override
  public void run() {
    throw new NotSupportedException("Not support run() receiving session!");
  }

  @Override
  public void setMessageListener(MessageListener listener) throws JMSException {
    throw new JMSException("Not support setMessageListener() receiving session!");
  }

  @Override
  public void unsubscribe(String name) throws JMSException {
    if (config.isAllowUnsubscriberOnReceivingMethod()) {
      session.unsubscribe(name);
      return;
    }
    throw new JMSException("Not support unsubscribe() receiving session!");
  }

}
