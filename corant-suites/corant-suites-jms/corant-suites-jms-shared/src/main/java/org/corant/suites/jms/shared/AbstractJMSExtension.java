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
import static org.corant.context.Instances.select;
import static org.corant.shared.util.Empties.isNotEmpty;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.jms.JMSConnectionFactory;
import org.corant.context.Qualifiers.NamedQualifierObjectManager;
import org.corant.context.proxy.ContextualMethodHandler;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.jms.shared.annotation.MessageDispatch;
import org.corant.suites.jms.shared.annotation.MessageReceive;
import org.corant.suites.jms.shared.annotation.MessageSend;
import org.corant.suites.jms.shared.annotation.MessageSends;
import org.corant.suites.jms.shared.annotation.MessageStream;

/**
 * corant-suites-jms-shared
 *
 * @author bingo 下午3:53:23
 *
 */
public abstract class AbstractJMSExtension implements Extension {

  protected final Logger logger = Logger.getLogger(getClass().getName());
  protected final Set<String> connectionFactories = newSetFromMap(new ConcurrentHashMap<>());
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

  public NamedQualifierObjectManager<? extends AbstractJMSConfig> getConfigManager() {
    return configManager;
  }

  public Set<ContextualMethodHandler> getReceiveMethods() {
    return Collections.unmodifiableSet(receiveMethods);
  }

  public Set<ContextualMethodHandler> getStreamMethods() {
    return Collections.unmodifiableSet(streamMethods);
  }

  protected void onBeforeShutdown(@Observes @Priority(0) BeforeShutdown bs) {
    configManager.destroy();
    receiveMethods.clear();
    streamMethods.clear();
  }

  protected void onProcessAnnotatedType(@Observes @WithAnnotations({MessageReceive.class,
      MessageStream.class, MessageSend.class, MessageSends.class}) ProcessAnnotatedType<?> pat) {
    final Class<?> beanClass = pat.getAnnotatedType().getJavaClass();

    MessageSend[] mss = beanClass.getAnnotationsByType(MessageSend.class);
    if (isNotEmpty(mss)) {
      for (MessageSend ms : mss) {
        connectionFactories.add(ms.connectionFactoryId());
      }
    }

    MessageSends ms = beanClass.getAnnotation(MessageSends.class);
    if (ms != null) {
      for (MessageSend m : ms.value()) {
        connectionFactories.add(m.connectionFactoryId());
      }
    }

    logger
        .fine(() -> String.format("Scanning JMS message consumer type: %s.", beanClass.getName()));
    ContextualMethodHandler
        .fromDeclared(beanClass, m -> m.isAnnotationPresent(MessageReceive.class)).forEach(cm -> {
          logger.fine(() -> String.format(
              "Found annotated JMS message consumer method %s.%s, adding for further processing.",
              beanClass.getName(), cm.getMethod().getName()));
          receiveMethods.add(cm);
          connectionFactories
              .add(cm.getMethod().getAnnotation(MessageReceive.class).connectionFactoryId());
        });
    ContextualMethodHandler.fromDeclared(beanClass, m -> m.isAnnotationPresent(MessageStream.class))
        .forEach(cm -> {
          logger.fine(() -> String.format(
              "Found annotated JMS message stream method %s.%s, for now we do not support it.",
              beanClass.getName(), cm.getMethod().getName()));
          streamMethods.add(cm);
          connectionFactories
              .add(cm.getMethod().getAnnotation(MessageReceive.class).connectionFactoryId());
        });
  }

  void onProcessInjectionPoint(@Observes ProcessInjectionPoint<?, ?> pip) {
    if (pip.getInjectionPoint().getAnnotated().isAnnotationPresent(JMSConnectionFactory.class)) {
      JMSConnectionFactory cf =
          pip.getInjectionPoint().getAnnotated().getAnnotation(JMSConnectionFactory.class);
      connectionFactories.add(cf.value());// FIXME JNDI
    } else if (pip.getInjectionPoint().getAnnotated().isAnnotationPresent(MessageDispatch.class)) {
      MessageDispatch md =
          pip.getInjectionPoint().getAnnotated().getAnnotation(MessageDispatch.class);
      connectionFactories.add(md.connectionFactoryId());// FIXME JNDI
    }
  }

  void validate(@Observes AfterDeploymentValidation adv, BeanManager bm) {
    for (String cf : connectionFactories) {
      if (configManager.get(cf) == null) {
        adv.addDeploymentProblem(new CorantRuntimeException(
            "Can not find JMS connection factory config by id [%s]", cf));
      }
    }
  }

}
