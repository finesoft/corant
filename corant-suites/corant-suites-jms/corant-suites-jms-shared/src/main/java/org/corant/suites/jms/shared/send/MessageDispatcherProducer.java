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

import static org.corant.shared.util.Assertions.shouldNotEmpty;
import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.jms.JMSDestinationDefinition;
import javax.jms.JMSDestinationDefinitions;
import javax.jms.JMSSessionMode;
import org.corant.kernel.util.CDIs;
import org.corant.suites.jms.shared.annotation.MessageDispatch;
import org.corant.suites.jms.shared.send.MessageDispatcher.GroupMessageDispatcherImpl;
import org.corant.suites.jms.shared.send.MessageDispatcher.MessageDispatcherImpl;

/**
 * corant-suites-jms-shared
 *
 * @author bingo 下午12:12:26
 *
 */
@ApplicationScoped
public class MessageDispatcherProducer {

  @Produces
  public MessageDispatcher produce(final InjectionPoint ip) {
    List<MessageDispatcher> dispatchers = new ArrayList<>();
    final MessageDispatch at = CDIs.getAnnotated(ip).getAnnotation(MessageDispatch.class);
    if (at != null) {
      dispatchers.add(new MessageDispatcherImpl(at));
    }
    // nonstandard
    final JMSDestinationDefinition jmd =
        CDIs.getAnnotated(ip).getAnnotation(JMSDestinationDefinition.class);
    final JMSSessionMode jmsd = CDIs.getAnnotated(ip).getAnnotation(JMSSessionMode.class);
    if (jmd != null) {
      if (jmsd != null) {
        dispatchers.add(new MessageDispatcherImpl(jmd, jmsd));
      } else {
        dispatchers.add(new MessageDispatcherImpl(jmd, jmsd));
      }
    }
    final JMSDestinationDefinitions jmds =
        CDIs.getAnnotated(ip).getAnnotation(JMSDestinationDefinitions.class);
    for (JMSDestinationDefinition jmdss : jmds.value()) {
      if (jmdss != null) {
        dispatchers.add(new MessageDispatcherImpl(jmdss, jmsd));
      } else {
        dispatchers.add(new MessageDispatcherImpl(jmdss, jmsd));
      }
    }
    return new GroupMessageDispatcherImpl(shouldNotEmpty(dispatchers));
  }
}
