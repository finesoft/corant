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
package org.corant.suites.jms.shared;

import static java.util.Collections.newSetFromMap;
import static org.corant.Corant.instance;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isEmpty;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.jms.ConnectionFactory;
import org.corant.kernel.util.Unnamed;
import org.corant.suites.jms.shared.annotation.MessageReceive;
import org.corant.suites.jms.shared.annotation.MessageStream;

/**
 * corant-suites-jms-shared
 *
 * @author bingo 下午3:53:23
 *
 */
public abstract class AbstractJMSExtension implements Extension {

  protected final Logger logger = Logger.getLogger(getClass().getName());
  protected final Set<AnnotatedMethod<?>> receiverMethods =
      newSetFromMap(new ConcurrentHashMap<>());
  protected final Set<AnnotatedMethod<?>> streamProcessorMethods =
      newSetFromMap(new ConcurrentHashMap<>());

  public static ConnectionFactory retriveConnectionFactory(String connectionFactory) {
    if (isEmpty(connectionFactory)) {
      if (instance().select(ConnectionFactory.class).isResolvable()) {
        return instance().select(ConnectionFactory.class).get();
      }
      return instance().select(ConnectionFactory.class, Unnamed.INST).get();
    } else {
      return shouldNotNull(
          instance().select(ConnectionFactory.class, NamedLiteral.of(connectionFactory)).get());
    }
  }

  public Set<AnnotatedMethod<?>> receiverMethods() {
    return receiverMethods;
  }

  protected boolean enable() {
    return true;
  }

  protected void onProcessAnnotatedType(@Observes @WithAnnotations({MessageReceive.class,
      MessageStream.class}) ProcessAnnotatedType<?> pat) {
    if (!enable()) {
      return;
    }
    logger.info(() -> String.format("Scanning message consumer type: %s",
        pat.getAnnotatedType().getJavaClass().getName()));
    final AnnotatedType<?> annotatedType = pat.getAnnotatedType();
    for (AnnotatedMethod<?> am : annotatedType.getMethods()) {
      if (am.isAnnotationPresent(MessageReceive.class)) {
        logger.info(() -> "Found annotated message consumer method, adding for further processing");
        receiverMethods.add(am);
      } else if (am.isAnnotationPresent(MessageStream.class)) {
        logger.info(() -> "Found annotated message stream method, adding for further processing");
        streamProcessorMethods.add(am);
      }
    }
  }

}
