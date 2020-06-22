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

import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Objects.forceCast;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.api.jms.JMSFactoryType;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.corant.config.declarative.DeclarativeConfigResolver;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.Conversions;
import org.corant.suites.cdi.Qualifiers.DefaultNamedQualifierObjectManager;
import org.corant.suites.jms.shared.AbstractJMSExtension;

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

  protected void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery event) {
    if (event != null) {
      getConfigManager().getAllWithQualifiers().forEach((dsc, dsn) -> {
        if (dsc.isEnable()) {
          event.<ActiveMQConnectionFactory>addBean().addQualifiers(dsn)
              .addTransitiveTypeClosure(ActiveMQConnectionFactory.class)
              .beanClass(ActiveMQConnectionFactory.class).scope(ApplicationScoped.class)
              .produceWith(beans -> {
                try {
                  return buildConnectionFactory(beans, forceCast(dsc));
                } catch (Exception e) {
                  throw new CorantRuntimeException(e);
                }
              }).disposeWith((cf, beans) -> cf.close());
        }
      });
    }
  }

  protected void onBeforeBeanDiscovery(@Observes final BeforeBeanDiscovery bbd,
      final BeanManager beanManager) {
    Map<String, ArtemisConfig> configs =
        DeclarativeConfigResolver.resolveMulti(ArtemisConfig.class);
    configManager = new DefaultNamedQualifierObjectManager<>(configs.values());
    if (configManager.isEmpty()) {
      logger.info(() -> "Can not find any artemis configurations.");
    } else {
      logger.fine(() -> String.format("Find %s artemis brokers named [%s].", configManager.size(),
          String.join(", ", configManager.getAllDisplayNames())));
    }
  }

  private ActiveMQConnectionFactory buildConnectionFactory(Instance<Object> beans,
      ArtemisConfig cfg) throws Exception {

    final ActiveMQConnectionFactory activeMQConnectionFactory;
    if (cfg.getUrl() != null) {
      activeMQConnectionFactory =
          ActiveMQJMSClient.createConnectionFactory(cfg.getUrl(), cfg.getConnectionFactoryId());
    } else {
      List<TransportConfiguration> tcs = new ArrayList<>();
      if (isNotEmpty(cfg.getHostPorts())) {
        int seq = 0;
        for (Pair<String, Integer> hp : cfg.getHostPortPairs()) {
          Map<String, Object> params = new HashMap<>();
          params.put("serverId", ++seq);
          params.put(TransportConstants.HOST_PROP_NAME, hp.getLeft());
          params.put(TransportConstants.PORT_PROP_NAME, hp.getRight());
          tcs.add(new TransportConfiguration(cfg.getConnectorFactory(), params));
        }
      }
      JMSFactoryType factoryType = cfg.isXa() ? JMSFactoryType.XA_CF : JMSFactoryType.CF;
      if (cfg.isHa()) {
        activeMQConnectionFactory = ActiveMQJMSClient.createConnectionFactoryWithHA(factoryType,
            tcs.stream().toArray(TransportConfiguration[]::new));
      } else {
        activeMQConnectionFactory = ActiveMQJMSClient.createConnectionFactoryWithoutHA(factoryType,
            tcs.stream().toArray(TransportConfiguration[]::new));
      }
    }
    if (cfg.hasAuthentication()) {
      activeMQConnectionFactory.setUser(cfg.getUsername());
      activeMQConnectionFactory.setPassword(cfg.getPassword());
    }
    cfg.getProperties().forEach((m, v) -> {
      if (v.isPresent()) {
        try {
          Class<?> parameterType = m.getParameterTypes()[0];
          if (String.class.equals(parameterType)) {
            m.invoke(activeMQConnectionFactory, Conversions.toString(v.get()));
          } else if (int.class.equals(parameterType)) {
            m.invoke(activeMQConnectionFactory, Conversions.toInteger(v.get()));
          } else if (long.class.equals(parameterType)) {
            m.invoke(activeMQConnectionFactory, Conversions.toLong(v.get()));
          } else if (boolean.class.equals(parameterType)) {
            m.invoke(activeMQConnectionFactory, Conversions.toBoolean(v.get()));
          } else if (double.class.equals(parameterType)) {
            m.invoke(activeMQConnectionFactory, Conversions.toDouble(v.get()));
          }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
          throw new CorantRuntimeException(e);
        }
      }
    });
    // The CF will probably be GCed since it was injected, so we disable the finalize check
    return activeMQConnectionFactory.disableFinalizeChecks();
  }

}
