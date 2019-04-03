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
package org.corant.suites.jms.artemis;

import static org.corant.Corant.instance;
import static org.corant.Corant.me;
import static org.corant.Corant.tryInstance;
import static org.corant.shared.util.Assertions.shouldBeFalse;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.StreamUtils.asStream;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.MessageListener;
import org.corant.kernel.event.PostCorantReadyEvent;
import org.corant.kernel.event.PreContainerStopEvent;
import org.corant.kernel.util.Cdis.InjectionTargetWrapper;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.jms.shared.AbstractJmsExtension;
import org.corant.suites.jms.shared.annotation.MessageReceiver;
import org.corant.suites.jms.shared.annotation.MessageSender;
import org.corant.suites.jms.shared.annotation.MessageSender.MessageProducerLiteral;

/**
 * corant-suites-jms-artemis
 *
 * @author bingo 下午4:12:15
 *
 */
public class ArtemisJmsExtension extends AbstractJmsExtension {

  protected final Map<String, Destination> destinations = new ConcurrentHashMap<>();
  protected final Map<Destination, JMSConsumer> consumers = new ConcurrentHashMap<>();
  protected final Map<MessageProducerLiteral, ArtemisMessageSender> senders =
      new ConcurrentHashMap<>();

  public Destination getDestination(String name) {
    return destinations.get(name);
  }

  void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery event) {
    if (event != null) {
      event.<JMSProducer>addBean().addQualifier(Default.Literal.INSTANCE)
          .addTransitiveTypeClosure(JMSProducer.class).beanClass(JMSProducer.class)
          .scope(ApplicationScoped.class).produceWith(beans -> {
            return instance().select(JMSContext.class).get().createProducer();
          });
    }
  }

  void onPostCorantReadyEvent(@Observes PostCorantReadyEvent adv) {
    if (!instance().select(JMSContext.class).isResolvable()) {
      logger.warning(() -> "Can not found jms context!");
      return;
    }
    final JMSContext jmsc = instance().select(JMSContext.class).get();
    receiverMethods.forEach(rm -> {
      shouldBeTrue(rm.getJavaMember().getParameterCount() == 1);
      shouldBeTrue(rm.getJavaMember().getParameters()[0].getType().equals(Message.class));
      final MessageReceiver msn = rm.getAnnotation(MessageReceiver.class);
      for (String qn : msn.destinations()) {
        if (isNotBlank(qn)) {
          shouldBeFalse(destinations.containsKey(qn),
              "The destination name %s on %s.%s has been used!", qn,
              rm.getJavaMember().getDeclaringClass().getName(), rm.getJavaMember().getName());
          Destination destination = destinations.computeIfAbsent(qn,
              q -> msn.multicast() ? jmsc.createTopic(q) : jmsc.createQueue(q));
          final JMSConsumer consumer = consumers.computeIfAbsent(destination,
              q -> isNotBlank(msn.selector()) ? jmsc.createConsumer(q, msn.selector())
                  : jmsc.createConsumer(q));
          consumer.setMessageListener(createMessageListener(rm, me().getBeanManager()));
        }
      }
    });
  }

  void onPreCorantStop(@Observes PreContainerStopEvent e) {
    consumers.values().forEach(JMSConsumer::close);
    tryInstance().ifPresent(i -> i.select(JMSContext.class).get().close());
  }

  <X> void onProcessInjectionTarget(@Observes ProcessInjectionTarget<X> pit) {
    final InjectionTarget<X> it = pit.getInjectionTarget();
    final AnnotatedType<X> at = pit.getAnnotatedType();
    pit.setInjectionTarget(new InjectionTargetWrapper<X>(it) {
      @Override
      public void inject(X instance, CreationalContext<X> ctx) {
        it.inject(instance, ctx);
        asStream(at.getJavaClass().getDeclaredFields()).forEach(field -> {
          final MessageSender mp = field.getAnnotation(MessageSender.class);
          if (mp != null && field.getType().isAssignableFrom(ArtemisMessageSender.class)) {
            field.setAccessible(Boolean.TRUE);
            final MessageProducerLiteral mpInst = MessageProducerLiteral.of(mp);
            try {
              field.set(instance,
                  senders.computeIfAbsent(mpInst, (p) -> new ArtemisMessageSender(mpInst)));
            } catch (IllegalArgumentException | IllegalAccessException e) {
              throw new CorantRuntimeException(e, "Can not inject ArtemisMessageSender to %s.%s",
                  at.getJavaClass().getName(), field.getName());
            }
          }
        });
      }
    });
  }

  private MessageListener createMessageListener(AnnotatedMethod<?> method,
      BeanManager beanManager) {
    final Set<Bean<?>> beans = beanManager.getBeans(method.getJavaMember().getDeclaringClass());
    final Bean<?> propertyResolverBean = beanManager.resolve(beans);
    final CreationalContext<?> creationalContext =
        beanManager.createCreationalContext(propertyResolverBean);
    Object inst = beanManager.getReference(propertyResolverBean,
        method.getJavaMember().getDeclaringClass(), creationalContext);
    method.getJavaMember().setAccessible(true);
    return new ArtemisMessageReceiver((msg) -> {
      try {
        method.getJavaMember().invoke(inst, msg);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        throw new CorantRuntimeException(e);
      }
    });
    // return (msg) -> {
    // try {
    // method.getJavaMember().invoke(inst, msg);
    // } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
    // throw new CorantRuntimeException(e);
    // }
    // };
  }

}
