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
package org.corant.modules.ddd.shared.message;

import static org.corant.context.Instances.resolve;
import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.inject.Inject;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.transaction.Transactional;
import org.corant.modules.ddd.Message;
import org.corant.modules.ddd.MessageDispatcher;
import org.corant.modules.jms.JMSNames;
import org.corant.modules.jms.context.JMSContextService;
import org.corant.modules.jms.marshaller.MessageMarshaller;
import org.corant.modules.jms.metadata.MessageDestinationMetaData;
import org.corant.shared.exception.NotSupportedException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-modules-ddd-shared
 *
 * @author bingo 下午12:01:54
 *
 */
@ApplicationScoped
@Transactional
public class JMSMessageDispatcher implements MessageDispatcher {

  protected final Map<Class<?>, Set<MessageDestinationMetaData>> metas = new ConcurrentHashMap<>();

  @Inject
  @Any
  protected Instance<JMSContextService> contextService;

  @Inject
  @ConfigProperty(name = "corant.ddd.message.marshaller",
      defaultValue = JMSNames.MSG_MARSHAL_SCHAME_STD_JAVA)
  protected String marshallerName;

  protected MessageMarshaller marshaller;

  @Override
  public void accept(Message[] t) {
    for (Message msg : t) {
      for (MessageDestinationMetaData dest : from(msg.getClass())) {
        send(dest.getConnectionFactoryId(), dest.isMulticast(), dest.getName(),
            marshaller.serialize(obtainJmsContext(dest.getConnectionFactoryId()), msg));
      }
    }
  }

  public void send(String broker, boolean multicast, String destination,
      javax.jms.Message message) {
    JMSContext ctx = obtainJmsContext(broker);
    JMSProducer producer = ctx.createProducer();
    producer.send(createDestination(ctx, multicast, destination), message);
  }

  protected Destination createDestination(JMSContext ctx, boolean multicast, String destination) {
    return multicast ? ctx.createTopic(destination) : ctx.createQueue(destination);
  }

  protected Set<MessageDestinationMetaData> from(Class<?> clazz) {
    return metas.computeIfAbsent(clazz, MessageDestinationMetaData::from);
  }

  protected JMSContext obtainJmsContext(String broker) {
    if (contextService.isResolvable()) {
      return shouldNotNull(contextService.get().getJMSContext(broker));
    }
    throw new NotSupportedException();
  }

  @PostConstruct
  protected void onPostConstruct() {
    marshaller = resolve(MessageMarshaller.class, NamedLiteral.of(marshallerName));
  }

  @PreDestroy
  protected void onPreDestroy() {
    metas.clear();
  }
}
