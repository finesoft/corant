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

import static org.corant.shared.util.Assertions.shouldNotNull;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import org.corant.kernel.util.CDIs;
import org.corant.suites.jms.shared.send.MessageDispatcher.MessageSenderImpl;

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
    final org.corant.suites.jms.shared.annotation.MessageDispatch at =
        shouldNotNull(CDIs.getAnnotated(ip)
            .getAnnotation(org.corant.suites.jms.shared.annotation.MessageDispatch.class));
    return new MessageSenderImpl(at);
  }
}
