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
package org.corant.modules.jms.shared.send;

import static org.corant.shared.util.Assertions.shouldNotEmpty;
import java.util.ArrayList;
import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import org.corant.context.CDIs;
import org.corant.modules.jms.annotation.MessageSend;
import org.corant.modules.jms.metadata.MessageSendMetaData;
import org.corant.modules.jms.send.GroupMessageSender;
import org.corant.modules.jms.send.MessageSender;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午12:12:26
 *
 */
@ApplicationScoped
public class MessageSenderProducer {

  @Produces
  @Dependent
  protected MessageSender produce(final InjectionPoint ip) {
    List<MessageSender> senders = new ArrayList<>();
    final MessageSend at = CDIs.getAnnotation(ip, MessageSend.class);
    if (at != null) {
      senders.add(new DefaultMessageSender(MessageSendMetaData.of(at)));
    }
    return new GroupMessageSender(shouldNotEmpty(senders));
  }
}
