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
import static org.corant.shared.util.Assertions.shouldBeTrue;
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
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import org.corant.kernel.event.PostCorantReadyEvent;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.jms.shared.AbstractJmsExtension;
import org.corant.suites.jms.shared.annotation.MessageConsumer;

/**
 * corant-suites-jms-artemis
 *
 * @author bingo 下午4:12:15
 *
 */
public class ArtemisJmsExtension extends AbstractJmsExtension {

  protected final Map<String, Queue> queues = new ConcurrentHashMap<>();
  protected final Map<Queue, JMSConsumer> consumers = new ConcurrentHashMap<>();

  void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery event) {
    if (event != null) {
      event.<JMSProducer>addBean().addQualifier(Default.Literal.INSTANCE)
          .addTransitiveTypeClosure(JMSProducer.class).beanClass(JMSProducer.class)
          .scope(ApplicationScoped.class).produceWith(beans -> {
            return instance().select(JMSContext.class).get().createProducer();
          });
    }
  }

  void onBeforeShutdown(@Observes BeforeShutdown e) {
    consumers.values().forEach(JMSConsumer::close);
  }

  void onPostCorantReadyEvent(@Observes PostCorantReadyEvent adv) {
    if (me() != null && !tryInstance().select(JMSContext.class).isResolvable()) {
      logger.warning(() -> "Can not found jms context!");
      return;
    }
    final JMSContext jmsc = tryInstance().select(JMSContext.class).get();
    consumerMethods.forEach(cm -> {
      shouldBeTrue(cm.getJavaMember().getParameterCount() == 1);
      shouldBeTrue(cm.getJavaMember().getParameters()[0].getType().equals(Message.class));
      final MessageConsumer msn = cm.getAnnotation(MessageConsumer.class);
      for (String qn : msn.queues()) {
        if (isNotBlank(qn)) {
          Queue queue = queues.computeIfAbsent(qn, q -> jmsc.createQueue(q));
          final JMSConsumer consumer = consumers.computeIfAbsent(queue,
              q -> isNotBlank(msn.selector()) ? jmsc.createConsumer(q, msn.selector())
                  : jmsc.createConsumer(q));
          consumer.setMessageListener(createMessageListener(cm, me().getBeanManager()));
        }
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
    return (msg) -> {
      try {
        method.getJavaMember().invoke(inst, msg);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        throw new CorantRuntimeException(e);
      }
    };
  }

}
