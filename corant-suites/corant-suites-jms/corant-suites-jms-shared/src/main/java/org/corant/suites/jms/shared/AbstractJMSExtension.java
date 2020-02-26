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
import static org.corant.suites.cdi.Instances.findNamed;
import static org.corant.suites.cdi.Instances.select;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.jms.ConnectionFactory;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.cdi.Qualifiers.NamedQualifierObjectManager;
import org.corant.suites.cdi.proxy.ContextualMethodHandler;
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
  protected final Set<ContextualMethodHandler> receiveMethods =
      newSetFromMap(new ConcurrentHashMap<>());
  protected final Set<ContextualMethodHandler> streamMethods =
      newSetFromMap(new ConcurrentHashMap<>());
  protected volatile NamedQualifierObjectManager<? extends AbstractJMSConfig> configManager =
      NamedQualifierObjectManager.empty();

  public static AbstractJMSConfig getConfig(String connectionFactoryId) {
    if (select(AbstractJMSExtension.class).isResolvable()) {
      return select(AbstractJMSExtension.class).get().getConfigManager().get(connectionFactoryId);
    }
    return null;
  }

  public static ConnectionFactory getConnectionFactory(String connectionFactoryId) {
    return findNamed(ConnectionFactory.class, connectionFactoryId).orElseThrow(
        () -> new CorantRuntimeException("Can not find any JMS connection factory for %s",
            connectionFactoryId));
  }

  public NamedQualifierObjectManager<? extends AbstractJMSConfig> getConfigManager() {
    return configManager;
  }

  public Set<ContextualMethodHandler> getReceiveMethods() {
    return Collections.unmodifiableSet(receiveMethods);
  }

  public Set<ContextualMethodHandler> getStreamMethods() {
    return Collections.unmodifiableSet(streamMethods);
  }

  protected void onProcessAnnotatedType(@Observes @WithAnnotations({MessageReceive.class,
      MessageStream.class}) ProcessAnnotatedType<?> pat) {
    final Class<?> beanClass = pat.getAnnotatedType().getJavaClass();
    logger.info(() -> String.format("Scanning JMS message consumer type: %s", beanClass.getName()));
    ContextualMethodHandler.from(beanClass, m -> m.isAnnotationPresent(MessageReceive.class))
        .forEach(cm -> {
          logger.info(() -> String.format(
              "Found annotated JMS message consumer method %s.%s, adding for further processing.",
              beanClass.getName(), cm.getMethod().getName()));
          receiveMethods.add(cm);
        });
    ContextualMethodHandler.from(beanClass, m -> m.isAnnotationPresent(MessageStream.class))
        .forEach(cm -> {
          logger.info(() -> String.format(
              "Found annotated JMS message stream method %s.%s, for now we do not support it.",
              beanClass.getName(), cm.getMethod().getName()));
          streamMethods.add(cm);
        });
  }

  void validate(@Observes AfterDeploymentValidation adv, BeanManager bm) {
    // TODO FIXME validate config
  }

}
