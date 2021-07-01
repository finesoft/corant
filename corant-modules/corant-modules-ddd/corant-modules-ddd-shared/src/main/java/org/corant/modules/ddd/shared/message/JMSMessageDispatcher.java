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

import static org.corant.context.Beans.resolve;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Functions.uncheckedBiConsumer;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.inject.Inject;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.transaction.Transactional;
import org.corant.context.required.RequiredConfiguration;
import org.corant.context.required.RequiredConfiguration.ValuePredicate;
import org.corant.modules.ddd.Message;
import org.corant.modules.ddd.Message.BinaryMessage;
import org.corant.modules.ddd.MessageDispatcher;
import org.corant.modules.jms.JMSNames;
import org.corant.modules.jms.context.JMSContextService;
import org.corant.modules.jms.marshaller.MessageMarshaller;
import org.corant.modules.jms.metadata.MessageDestinationMetaData;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.ubiquity.Sortable;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-modules-ddd-shared
 *
 * @author bingo 下午12:01:54
 *
 */
@ApplicationScoped
@Transactional
@RequiredConfiguration(key = "corant.ddd.message.disable-jms-dispatcher",
    predicate = ValuePredicate.EQ, type = Boolean.class, value = "false")
public class JMSMessageDispatcher implements MessageDispatcher {

  protected final Map<Class<?>, Set<MessageDestinationMetaData>> metas = new ConcurrentHashMap<>();

  protected final transient Logger logger = Logger.getLogger(this.getClass().toString());

  @Inject
  @Any
  protected Instance<JMSContextService> contextService;

  @Inject
  @Any
  protected Instance<JMSMessagePreDispatchHandler> preDispatchHandlers;

  @Inject
  @Any
  protected Instance<JMSMessageDestinationResolver> destinationResolvers;

  @Inject
  @ConfigProperty(name = "corant.ddd.message.marshaller",
      defaultValue = JMSNames.MSG_MARSHAL_SCHEMA_STD_JAVA)
  protected String marshallerName;

  @Inject
  @ConfigProperty(name = "corant.ddd.message.binary-marshaller",
      defaultValue = JMSNames.MSG_MARSHAL_SCHEMA_ZIP_BINARY)
  protected String binaryMarshallerName;

  protected MessageMarshaller marshaller;

  protected MessageMarshaller binaryMarshaller;

  @Override
  public void accept(Message[] messages) {
    for (Message msg : messages) {
      for (MessageDestinationMetaData dest : from(msg.getClass())) {
        send(dest.getConnectionFactoryId(), dest.isMulticast(), dest.getName(),
            dest.getProperties(), msg);
      }
    }
  }

  public void send(String broker, boolean multicast, String destination,
      Map<String, Object> properties, Message message) {
    JMSContext ctx = obtainJmsContext(broker);
    final Destination dest = resolveDestination(message, ctx, multicast, destination);
    logger.finer(() -> String.format("Resolved JMS message destination %s for domain message %",
        dest, message.getClass()));
    final javax.jms.Message jmsMsg = createJMSMessage(ctx, message);
    if (isNotEmpty(properties)) {
      properties.forEach(uncheckedBiConsumer(jmsMsg::setObjectProperty));
    }
    onPreDispatch(jmsMsg);
    ctx.createProducer().send(dest, jmsMsg);
  }

  protected Destination createDestination(JMSContext ctx, boolean multicast, String destination) {
    return multicast ? ctx.createTopic(destination) : ctx.createQueue(destination);
  }

  protected javax.jms.Message createJMSMessage(JMSContext ctx, Message message) {
    final javax.jms.Message jmsMsg;
    if (message instanceof BinaryMessage) {
      try (InputStream is = ((BinaryMessage) message).openStream()) {
        jmsMsg = binaryMarshaller.serialize(ctx, is);
        logger.finer(() -> String.format(
            "Convert the domain message %s to binary JMS message, serialize schema %s.",
            message.getClass(), binaryMarshallerName));
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
    } else {
      jmsMsg = marshaller.serialize(ctx, message);
      logger.finer(
          () -> String.format("Convert the domain message %s to JMS message, serialize schema %s",
              message.getClass(), marshallerName));
    }
    return jmsMsg;
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
    binaryMarshaller = resolve(MessageMarshaller.class, NamedLiteral.of(binaryMarshallerName));
  }

  @PreDestroy
  protected void onPreDestroy() {
    metas.clear();
  }

  protected void onPreDispatch(javax.jms.Message jmsMsg) {
    if (!preDispatchHandlers.isUnsatisfied()) {
      preDispatchHandlers.stream().sorted(Sortable::compare).forEach(h -> h.accept(jmsMsg));
      logger.finer(() -> "Complete the preprocessing before dispatching the message.");
    }
  }

  protected Destination resolveDestination(Message message, JMSContext ctx, boolean multicast,
      String destination) {
    if (!destinationResolvers.isUnsatisfied()) {
      Optional<JMSMessageDestinationResolver> destResolver = destinationResolvers.stream()
          .filter(r -> r.canResolve(message)).sorted(Sortable::compare).findFirst();
      if (destResolver.isPresent()) {
        Destination dest = destResolver.get().apply(ctx, message);
        if (dest != null) {
          return dest;
        }
      }
    }
    return createDestination(ctx, multicast, destination);
  }
}
