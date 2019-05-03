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
import static org.corant.Corant.me;
import static org.corant.shared.util.Assertions.shouldBeFalse;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.StringUtils.defaultString;
import static org.corant.shared.util.StringUtils.isBlank;
import static org.corant.shared.util.StringUtils.isNoneBlank;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import org.corant.kernel.event.PostCorantReadyEvent;
import org.corant.kernel.event.PreContainerStopEvent;
import org.corant.kernel.util.Cdis;
import org.corant.kernel.util.Unnamed;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.ObjectUtils.Pair;
import org.corant.suites.jms.shared.MessageReceiver.MessageReceiverImpl;
import org.corant.suites.jms.shared.MessageSender.MessageSenderImpl;
import org.corant.suites.jms.shared.annotation.MessageReceive;
import org.corant.suites.jms.shared.annotation.MessageSend;
import org.corant.suites.jms.shared.annotation.MessageStream;

/**
 * corant-suites-jms-shared
 *
 * @author bingo 下午3:53:23
 *
 */
public abstract class AbstractJMSExtension implements Extension {

  protected final Logger logger = Logger.getLogger(getClass().getName());
  protected final Map<Object, JMSConsumer> consumers = new ConcurrentHashMap<>();
  protected final Map<String, JMSContext> consumerJmsContexts = new ConcurrentHashMap<>();

  protected final Set<AnnotatedMethod<?>> receiverMethods =
      newSetFromMap(new ConcurrentHashMap<>());
  protected final Set<AnnotatedMethod<?>> streamProcessorMethods =
      newSetFromMap(new ConcurrentHashMap<>());

  static ConnectionFactory retriveConnectionFactory(String connectionFactory) {
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

  protected MessageListener createMessageListener(AnnotatedMethod<?> method,
      BeanManager beanManager, JMSContext jmsc, int sessoinModel) {
    final Set<Bean<?>> beans = beanManager.getBeans(method.getJavaMember().getDeclaringClass());
    final Bean<?> propertyResolverBean = beanManager.resolve(beans);
    final CreationalContext<?> creationalContext =
        beanManager.createCreationalContext(propertyResolverBean);
    Object inst = beanManager.getReference(propertyResolverBean,
        method.getJavaMember().getDeclaringClass(), creationalContext);
    AccessController.doPrivileged((PrivilegedAction<?>) () -> {
      method.getJavaMember().setAccessible(true);
      return null;
    });
    return new MessageReceiverImpl(jmsc, (msg) -> {
      try {
        method.getJavaMember().invoke(inst, msg);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        throw new CorantRuntimeException(e);
      }
    });
  }

  protected boolean enable() {
    return true;
  }

  protected void onPostCorantReadyEvent(@Observes PostCorantReadyEvent adv) {
    if (!enable()) {
      logger.info(() -> "JMS not enable!");
      return;
    }
    if (instance().select(ConnectionFactory.class).isUnsatisfied()) {
      logger.warning(() -> "Can not found any JMS connection factory!");
      return;
    }
    receiverMethods.forEach(rm -> {
      shouldBeTrue(rm.getJavaMember().getParameterCount() == 1);
      shouldBeTrue(rm.getJavaMember().getParameters()[0].getType().equals(Message.class));
      final MessageReceive ann = rm.getAnnotation(MessageReceive.class);
      if (!isNoneBlank(ann.destinations())) {
        return;
      }
      final String cfn = defaultString(ann.connectionFactory());
      final int sessionMode = ann.sessionMode();
      final JMSContext ctx = consumerJmsContexts.computeIfAbsent(cfn,
          f -> retriveConnectionFactory(f).createContext(sessionMode));
      final String clsNme = rm.getJavaMember().getDeclaringClass().getName();
      final String metNme = rm.getJavaMember().getName();
      for (String dn : ann.destinations()) {
        if (isBlank(dn)) {
          continue;
        }
        JMSContext jmsc = ctx.createContext(sessionMode);
        Destination destination = ann.multicast() ? jmsc.createTopic(dn) : jmsc.createQueue(dn);
        final Pair<String, Destination> key = Pair.of(cfn, destination);
        shouldBeFalse(consumers.containsKey(key),
            "The destination named %s with connection factory %s on %s.%s has been used!", dn, cfn,
            clsNme, metNme);
        final JMSConsumer consumer =
            isNotBlank(ann.selector()) ? jmsc.createConsumer(destination, ann.selector())
                : jmsc.createConsumer(destination);
        consumer.setMessageListener(
            createMessageListener(rm, me().getBeanManager(), jmsc, sessionMode));
        consumers.put(key, consumer);
      }
    });
  }

  protected void onPreCorantStop(@Observes PreContainerStopEvent e) {
    if (!enable()) {
      return;
    }
    consumers.values().forEach(JMSConsumer::close);
    consumerJmsContexts.values().forEach(JMSContext::close);
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

  @ApplicationScoped
  public static class MessageSenderProducer {

    @Produces
    public MessageSender messageSender(final InjectionPoint ip) {
      final MessageSend at = shouldNotNull(Cdis.getAnnotated(ip).getAnnotation(MessageSend.class));
      return new MessageSenderImpl(at);
    }
  }

}
