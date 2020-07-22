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
package org.corant.suites.jms.shared.send;

import static org.corant.context.Instances.resolveApply;
import static org.corant.shared.util.Annotations.findAnnotation;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Lists.listOf;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.jms.JMSContext;
import javax.jms.Message;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.suites.jms.shared.annotation.MessageSend;
import org.corant.suites.jms.shared.annotation.MessageSends;
import org.corant.suites.jms.shared.context.JMSContextProducer;

/**
 * corant-suites-jms-shared
 *
 * @author bingo 下午5:00:02
 *
 */
public abstract class AnnotatedMessageSender extends AbstractMessageSender {

  public void send(Serializable... annotatedPayloads) {
    Map<Pair<String, Integer>, JMSContext> jmscs = new HashMap<>();
    for (Serializable annotatedPayload : annotatedPayloads) {
      Class<?> payloadClass = getUserClass(shouldNotNull(annotatedPayload));
      Set<MessageSenderMetaData> sends = resolveMetaDatas(payloadClass);
      shouldBeTrue(isNotEmpty(sends),
          "The payload class %s must include either MessageSend or MessageSends annotaion.",
          payloadClass);
      for (MessageSenderMetaData send : sends) {
        JMSContext jmsc = jmscs.computeIfAbsent(send.getFactoryKey(),
            s -> resolveApply(JMSContextProducer.class, b -> b.create(s.getKey(), s.getValue())));
        Message message = resolveMessage(jmsc, annotatedPayload, send.getSerialization());
        super.send(jmsc, message, send.getDestination(), send.isMulticast());
        logger.fine(() -> String.format("Send message %s to %s %s", payloadClass,
            send.getConnectionFactoryId(), send.getDestination()));
      }
    }
  }

  protected Set<MessageSenderMetaData> resolveMetaDatas(Class<?> messageClass) {
    Set<MessageSenderMetaData> sends = new LinkedHashSet<>();
    MessageSends anns = findAnnotation(messageClass, MessageSends.class, false);// FIXME inherit
    if (anns == null) {
      MessageSend ann = findAnnotation(messageClass, MessageSend.class, false);// FIXME inherit
      if (ann != null) {
        sends.add(new MessageSenderMetaData(ann));
      }
    } else {
      listOf(anns.value()).stream().map(MessageSenderMetaData::new).forEach(sends::add);
    }
    return sends;
  }
}
