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
import static org.corant.shared.util.Assertions.shouldBeFalse;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.StringUtils.defaultString;
import static org.corant.shared.util.StringUtils.isBlank;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.api.jms.JMSFactoryType;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.corant.kernel.event.PostCorantReadyEvent;
import org.corant.kernel.event.PreContainerStopEvent;
import org.corant.kernel.util.Cdis;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.ObjectUtils.Pair;
import org.corant.suites.jms.shared.AbstractJMSExtension;
import org.corant.suites.jms.shared.MessageSender;
import org.corant.suites.jms.shared.annotation.MessageReceive;
import org.corant.suites.jms.shared.annotation.MessageSend;
import org.corant.suites.jms.shared.annotation.MessageSend.MessageSenderLiteral;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * corant-suites-jms-artemis
 *
 * <pre>
 * <h1>Injection of JMSContext objects - Proposals (version 4)</h1>
 *
 * If an injected JMSContext is used in a JTA transaction (both bean-managed and container-managed),
 * its scope will be that of the transaction. This means that: The JMSContext object will be
 * automatically created the first time it is used within the transaction.
 *
 * The JMSContext object will be automatically closed when the transaction is committed.
 *
 * If, within the same JTA transaction, different beans, or different methods within the same bean,
 * use an injected JMSContext which is injected using identical annotations then they will all share
 * the same JMSContext object.
 *
 * If an injected JMSContext is used when there is no JTA transaction then its scope will be the
 * existing CDI scope @RequestScoped. This means that: The JMSContext object will be created the
 * first time it is used within a request.
 *
 * The JMSContext object will be closed when the request ends.
 *
 * If, within the same request, different beans, or different methods within the same bean, use an
 * injected JMSContext which is injected using identical annotations then they will all share the
 * same JMSContext object.
 *
 * If injected JMSContext is used both in a JTA transaction and outside a JTA transaction then
 * separate JMSContext objects will be used, with a separate JMSContext object being used for each
 * JTA transaction as described above.
 * </pre>
 *
 * {@link <a href="https://javaee.github.io/jms-spec/pages/JMSContextScopeProposalsv4p4">Proposed
 * changes to JMSContext to support injection (Option 4)</a>} <br/>
 * {@link <a href="https://javaee.github.io/jms-spec/pages/JMSContextScopeProposalsv4p1">Injection
 * of JMSContext objects - Proposals (version 4)</a>}
 *
 * <p>
 * Attention: For now we do not supported JMSProducer.setDeliveryDelay/setTimeToLive...(six methods)
 * , Artemis implemetions use singleton JMSProducer in one JMSContext, this can be problematic.
 * </p>
 *
 * @author bingo 下午4:12:15
 *
 */
public class ArtemisJMSExtension extends AbstractJMSExtension {

  protected final Map<String, ArtemisConfig> configs = new HashMap<>();
  protected final Map<Object, JMSConsumer> consumers = new ConcurrentHashMap<>();
  protected final Map<String, JMSContext> consumerJmsContexts = new ConcurrentHashMap<>();

  protected void onBeforeBeanDiscovery(@Observes final BeforeBeanDiscovery bbd,
      final BeanManager beanManager) {
    configs.clear();
    ArtemisConfig.from(ConfigProvider.getConfig()).forEach(configs::put);
    if (configs.isEmpty()) {
      logger.info(() -> "Can not find any artemis configurations.");
    } else {
      logger.info(() -> String.format("Find %s artemis names %s", configs.size(),
          String.join(", ", configs.keySet())));
    }
  }

  void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery event) {
    if (event != null) {
      configs.forEach((dsn, dsc) -> {
        event.<ActiveMQConnectionFactory>addBean().addQualifier(Cdis.resolveNamed(dsn))
            .addQualifier(Default.Literal.INSTANCE)
            .addTransitiveTypeClosure(ActiveMQConnectionFactory.class)
            .beanClass(ActiveMQConnectionFactory.class).scope(ApplicationScoped.class)
            .produceWith(beans -> {
              try {
                return buildConnectionFactory(beans, dsc);
              } catch (Exception e) {
                throw new CorantRuntimeException(e);
              }
            }).disposeWith((cf, beans) -> cf.close());
      });
    }
  }

  void onPostCorantReadyEvent(@Observes PostCorantReadyEvent adv) {
    if (instance().select(ConnectionFactory.class).isUnsatisfied()) {
      logger.warning(() -> "Can not found any jms connection factory!");
      return;
    }
    receiverMethods.forEach(rm -> {
      shouldBeTrue(rm.getJavaMember().getParameterCount() == 1);
      shouldBeTrue(rm.getJavaMember().getParameters()[0].getType().equals(Message.class));
      final String clsNme = rm.getJavaMember().getDeclaringClass().getName();
      final String metNme = rm.getJavaMember().getName();
      final MessageReceive msn = rm.getAnnotation(MessageReceive.class);
      final String cfn = defaultString(msn.connectionFactory());
      final JMSContext ctx = consumerJmsContexts.computeIfAbsent(cfn,
          f -> retriveConnectionFactory(f).createContext(JMSContext.AUTO_ACKNOWLEDGE));
      for (String dn : msn.destinations()) {
        if (isBlank(dn)) {
          continue;
        }
        final int sessionModel = msn.sessionModel();
        JMSContext jmsc = ctx.createContext(sessionModel);
        Destination destination = msn.multicast() ? jmsc.createTopic(dn) : jmsc.createQueue(dn);
        final Pair<String, Destination> key = Pair.of(cfn, destination);
        shouldBeFalse(consumers.containsKey(key),
            "The destination named %s with connection factory %s on %s.%s has been used!", dn, cfn,
            clsNme, metNme);
        final JMSConsumer consumer =
            isNotBlank(msn.selector()) ? jmsc.createConsumer(destination, msn.selector())
                : jmsc.createConsumer(destination);
        consumer.setMessageListener(createMessageListener(rm, me().getBeanManager()));
        consumers.put(key, consumer);
      }
    });
  }

  void onPreCorantStop(@Observes PreContainerStopEvent e) {
    consumers.values().forEach(JMSConsumer::close);
    consumerJmsContexts.values().forEach(JMSContext::close);
  }

  private ActiveMQConnectionFactory buildConnectionFactory(Instance<Object> beans,
      ArtemisConfig cfg) throws Exception {
    Map<String, Object> params = new HashMap<>();
    params.put("serverId", "1");
    final ActiveMQConnectionFactory activeMQConnectionFactory;
    if (cfg.getUrl() != null) {
      activeMQConnectionFactory = ActiveMQJMSClient.createConnectionFactory(cfg.getUrl(), null);
    } else {
      if (cfg.getHost() != null) {
        params.put(TransportConstants.HOST_PROP_NAME, cfg.getHost());
        params.put(TransportConstants.PORT_PROP_NAME, cfg.getPort());
      }
      if (cfg.isHa()) {
        activeMQConnectionFactory = ActiveMQJMSClient.createConnectionFactoryWithHA(
            JMSFactoryType.CF, new TransportConfiguration(cfg.getConnectorFactory(), params));
      } else {
        activeMQConnectionFactory = ActiveMQJMSClient.createConnectionFactoryWithoutHA(
            JMSFactoryType.CF, new TransportConfiguration(cfg.getConnectorFactory(), params));
      }
    }
    if (cfg.hasAuthentication()) {
      activeMQConnectionFactory.setUser(cfg.getUsername());
      activeMQConnectionFactory.setPassword(cfg.getPassword());
    }
    // The CF will probably be GCed since it was injected, so we disable the finalize check
    return activeMQConnectionFactory.disableFinalizeChecks();
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
  }

  @ApplicationScoped
  public static class ArtemisMessageSenderProducer {

    @Produces
    public MessageSender messageSender(final InjectionPoint ip) {
      final MessageSend at = Cdis.getAnnotated(ip).getAnnotation(MessageSend.class);
      return new ArtemisMessageSender(MessageSenderLiteral.of(at));
    }
  }
}
