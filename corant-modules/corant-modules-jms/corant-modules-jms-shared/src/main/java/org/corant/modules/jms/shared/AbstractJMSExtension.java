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
package org.corant.modules.jms.shared;

import static java.lang.String.format;
import static java.util.Collections.newSetFromMap;
import static org.corant.context.Beans.findNamed;
import static org.corant.context.Beans.select;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Lists.union;
import static org.corant.shared.util.Sets.setOf;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeShutdown;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.ProcessInjectionPoint;
import jakarta.enterprise.inject.spi.WithAnnotations;
import jakarta.jms.JMSConnectionFactory;
import jakarta.jms.Session;
import org.corant.context.proxy.ContextualMethodHandler;
import org.corant.context.proxy.ProxyBuilder;
import org.corant.context.qualifier.Qualifiers.NamedQualifierObjectManager;
import org.corant.modules.jms.annotation.MessageContext;
import org.corant.modules.jms.annotation.MessageDestination;
import org.corant.modules.jms.annotation.MessageDriven;
import org.corant.modules.jms.annotation.MessageReply;
import org.corant.modules.jms.annotation.MessageSend;
import org.corant.modules.jms.marshaller.MessageMarshaller;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Priorities;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.Services;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午3:53:23
 */
public abstract class AbstractJMSExtension implements Extension {

  protected final Logger logger = Logger.getLogger(getClass().getName());
  protected final Set<String> connectionFactories = newSetFromMap(new ConcurrentHashMap<>());
  protected final Set<String> marshallers = newSetFromMap(new ConcurrentHashMap<>());
  protected final Map<ContextualMethodHandler, Pair<Set<MessageDestination>, Set<MessageDestination>>> receiveMethods =
      new ConcurrentHashMap<>();
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

  public Map<String, ? extends AbstractJMSConfig> getConfigs() {
    return configManager.getAllWithNames();
  }

  public Set<ContextualMethodHandler> getReceiveMethods() {
    return Collections.unmodifiableSet(receiveMethods.keySet());
  }

  public Set<ContextualMethodHandler> getStreamMethods() {
    return Collections.unmodifiableSet(streamMethods);
  }

  protected NamedQualifierObjectManager<? extends AbstractJMSConfig> getConfigManager() {
    return configManager;
  }

  protected void onBeforeShutdown(
      @Observes @Priority(Priorities.FRAMEWORK_LOWER) BeforeShutdown bs) {
    configManager.destroy();
    receiveMethods.clear();
    streamMethods.clear();
  }

  protected void onProcessAnnotatedType(
      @Observes @WithAnnotations({MessageSend.class, MessageContext.class, MessageDriven.class,
          JMSConnectionFactory.class, MessageDestination.class}) ProcessAnnotatedType<?> pat) {
    if (Services.shouldVeto(pat.getAnnotatedType().getJavaClass())) {
      return;
    }
    final Class<?> beanClass = pat.getAnnotatedType().getJavaClass();
    MessageSend[] mss = beanClass.getAnnotationsByType(MessageSend.class);
    if (isNotEmpty(mss)) {
      for (MessageSend ms : mss) {
        connectionFactories.add(ms.destination().connectionFactoryId());
        marshallers.add(ms.marshaller());
      }
    }
    logger.fine(() -> format("Scanning message driven on bean: %s.", beanClass.getName()));
    ProxyBuilder.buildDeclaredMethods(beanClass, m -> m.isAnnotationPresent(MessageDriven.class))
        .forEach(cm -> {
          Method method = cm.getMethod();
          logger.fine(() -> format("Found message driven method %s.", method.getName()));
          for (MessageDriven md : method.getAnnotationsByType(MessageDriven.class)) {
            for (MessageReply mr : md.reply()) {
              marshallers.add(mr.marshaller());
            }
          }
          Set<MessageDestination> mds =
              setOf(method.getAnnotationsByType(MessageDestination.class));
          Set<MessageDestination> pds =
              setOf(method.getParameterTypes()[0].getAnnotationsByType(MessageDestination.class));
          receiveMethods.put(cm, Pair.of(mds, pds));
        });
  }

  void onProcessInjectionPoint(@Observes ProcessInjectionPoint<?, ?> pip) {
    if (pip.getInjectionPoint().getAnnotated().isAnnotationPresent(JMSConnectionFactory.class)) {
      JMSConnectionFactory cf =
          pip.getInjectionPoint().getAnnotated().getAnnotation(JMSConnectionFactory.class);
      connectionFactories.add(cf.value());// FIXME JNDI
    } else if (pip.getInjectionPoint().getAnnotated().isAnnotationPresent(MessageSend.class)) {
      MessageSend md = pip.getInjectionPoint().getAnnotated().getAnnotation(MessageSend.class);
      connectionFactories.add(md.destination().connectionFactoryId());// FIXME JNDI
    }
  }

  void validate(@Observes AfterDeploymentValidation adv, BeanManager bm) {
    for (String marshaller : marshallers) {
      if (findNamed(MessageMarshaller.class, marshaller).isEmpty()) {
        adv.addDeploymentProblem(new CorantRuntimeException(
            "Can not find any message marshaller named [%s]", marshaller));
      }
    }
    receiveMethods.forEach((m, mds) -> {
      // inject session to handler method 2022-04-04
      if (m.getMethod().getParameterCount() != 1 && m.getMethod().getParameterCount() != 2) {
        adv.addDeploymentProblem(new CorantRuntimeException(
            "The message driven method [%s] must have one or two parameter.", m.getMethod()));
      }
      if (m.getMethod().getParameterCount() == 2
          && !Session.class.isAssignableFrom(m.getMethod().getParameters()[1].getType())) {
        // inject session 2022-04-04
        adv.addDeploymentProblem(new CorantRuntimeException(
            "The message driven method [%s] second parameter type must be session.",
            m.getMethod()));
      }
      if (isNotEmpty(mds.key()) && isNotEmpty(mds.value())) {
        // since 2024-07-04
        logger.info(() -> String.format(
            "Note: the message destination annotation appears on both the method and the first parameter "
                + "of the method, and the system uses the annotation above the method by default, [%s].",
            m.getMethod()));
        // adv.addDeploymentProblem(new CorantRuntimeException(
        // "The message destination either appears on the method or on the first parameter "
        // + "class of the method, and cannot exist at the same time, the method [%s]",
        // m.getMethod()));
      } else if (isEmpty(mds.key()) && isEmpty(mds.value())) {
        adv.addDeploymentProblem(new CorantRuntimeException(
            "Can not find any message destinations on the method [%s]", m.getMethod()));
      }

      for (MessageDestination ds : union(mds.key(), mds.value())) {
        connectionFactories.add(ds.connectionFactoryId());
      }
    });
    for (String cf : connectionFactories) {
      if (configManager.get(cf) == null) {
        adv.addDeploymentProblem(new CorantRuntimeException(
            "Can not find JMS connection factory config by id [%s]", cf));
      }
    }

  }

}
