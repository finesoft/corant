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
package org.corant.suites.jta.narayana;

import static org.corant.context.Instances.select;
import static org.corant.shared.normal.Names.applicationName;
import static org.corant.shared.util.Classes.defaultClassLoader;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.MBeans.registerToMBean;
import static org.corant.shared.util.Streams.streamOf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.CreationException;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.inject.Singleton;
import javax.naming.NamingException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionScoped;
import javax.transaction.UserTransaction;
import org.corant.config.declarative.DeclarativeConfigResolver;
import org.corant.kernel.event.PostCorantReadyEvent;
import org.corant.kernel.event.PreContainerStopEvent;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.util.MBeans;
import org.corant.suites.jta.shared.TransactionExtension;
import com.arjuna.ats.arjuna.common.CoordinatorEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.RecoveryEnvironmentBean;
import com.arjuna.ats.jbossatx.jta.RecoveryManagerService;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.ats.jta.utils.JNDIManager;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

/**
 * corant-suites-jta-narayana
 *
 * FIXME org.jboss.logging.annotations.Message.Format.MESSAGE_FORMAT version issue!!!!
 *
 *
 * FIXME relase recovery XAResources.
 *
 * @author bingo 下午7:19:18
 *
 */
public class NarayanaExtension implements TransactionExtension {

  protected final Logger logger = Logger.getLogger(this.getClass().toString());
  protected final NarayanaTransactionConfig config =
      DeclarativeConfigResolver.resolveSingle(NarayanaTransactionConfig.class);
  protected volatile NarayanaRecoveryManagerService recoveryManagerService;
  protected List<String> mbeanNames = new ArrayList<>();

  @Override
  public NarayanaTransactionConfig getConfig() {
    return config;
  }

  void afterBeanDiscovery(@Observes final AfterBeanDiscovery event, final BeanManager beanManager) {
    if (getConfig().isBindToJndi()) {
      try {
        JNDIManager.bindJTAImplementation();
        logger.fine(() -> "Bind JTA implementations to Jndi.");
      } catch (NamingException e) {
        throw new CorantRuntimeException(e,
            "An error occurred while registering Transaction Manager to JNDI");
      }
    }

    if (event != null) {
      // The TransactionSynchronizationRegistry && TransactionManager have been registered by
      // narayana self see com.arjuna.ats.jta.cdi.TransactionExtension
      final Collection<? extends Bean<?>> userTransactionBeans =
          beanManager.getBeans(UserTransaction.class);
      if (isEmpty(userTransactionBeans)) {
        event.addBean().types(UserTransaction.class)
            .addQualifiers(Any.Literal.INSTANCE, Default.Literal.INSTANCE).scope(Dependent.class)
            .createWith(cc -> com.arjuna.ats.jta.UserTransaction.userTransaction());
      }

      event.addBean().id(Transaction.class.getName())
          .addQualifiers(Any.Literal.INSTANCE, Default.Literal.INSTANCE).types(Transaction.class)
          .scope(TransactionScoped.class).createWith(cc -> {
            try {
              return select(TransactionManager.class).get().getTransaction();
            } catch (final SystemException systemException) {
              throw new CreationException(systemException.getMessage(), systemException);
            }
          });

      event.addBean().addTransitiveTypeClosure(JTAEnvironmentBean.class)
          .addQualifiers(Any.Literal.INSTANCE, Default.Literal.INSTANCE).scope(Singleton.class)
          .createWith(cc -> BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class));

      event.<RecoveryManagerService>addBean().addTransitiveTypeClosure(RecoveryManagerService.class)
          .addQualifiers(Any.Literal.INSTANCE, Default.Literal.INSTANCE,
              NamedLiteral.of("narayana-jta"))
          .scope(Singleton.class).createWith(cc -> recoveryManagerService)
          .disposeWith((t, inst) -> t.destroy());
    }
  }

  void beforeBeanDiscovery(@Observes final BeforeBeanDiscovery event) {
    configEnvironmentBean();// TODO FIXME NOTE: initialization sequence
  }

  void configEnvironmentBean() {
    final ObjectStoreEnvironmentBean nullActionStoreBean =
        BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, null);
    final ObjectStoreEnvironmentBean defaultStoreBean =
        BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "default");
    final ObjectStoreEnvironmentBean stateStoreBean =
        BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "stateStore");
    final ObjectStoreEnvironmentBean commuStoreBean =
        BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "communicationStore");
    final CoordinatorEnvironmentBean coordinatorBean =
        BeanPopulator.getDefaultInstance(CoordinatorEnvironmentBean.class);
    final CoreEnvironmentBean coreBean =
        BeanPopulator.getDefaultInstance(CoreEnvironmentBean.class);
    final JTAEnvironmentBean jtaBean = BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class);
    final RecoveryEnvironmentBean recoveryBean =
        BeanPopulator.getDefaultInstance(RecoveryEnvironmentBean.class);
    streamOf(ServiceLoader.load(NarayanaConfigurator.class, defaultClassLoader()))
        .sorted(Sortable::compare).forEach(cfgr -> {
          logger.fine(() -> String.format("Use customer narayana configurator %s.",
              cfgr.getClass().getName()));
          cfgr.configCoreEnvironment(coreBean, config);
          cfgr.configCoordinatorEnvironment(coordinatorBean, config);
          cfgr.configRecoveryEnvironment(recoveryBean, config);
          cfgr.configObjectStoreEnvironment(nullActionStoreBean, null, config);
          cfgr.configObjectStoreEnvironment(defaultStoreBean, "default", config);
          cfgr.configObjectStoreEnvironment(stateStoreBean, "stateStore", config);
          cfgr.configObjectStoreEnvironment(commuStoreBean, "communicationStore", config);
          cfgr.configJTAEnvironmentBean(jtaBean, config);
        });
    if (config.isEnableMbean()) {
      registerToMBean(resolveMBeanName("ObjStoreEnvBean-nullAction"), nullActionStoreBean);
      registerToMBean(resolveMBeanName("ObjStoreEnvBean-default"), defaultStoreBean);
      registerToMBean(resolveMBeanName("ObjStoreEnvBean-stateStore"), stateStoreBean);
      registerToMBean(resolveMBeanName("ObjStoreEnvBean-communicationStore"), commuStoreBean);
      registerToMBean(resolveMBeanName("CoordinatorEnvtBean"), coordinatorBean);
      registerToMBean(resolveMBeanName("JTAEnvBean"), jtaBean);
      registerToMBean(resolveMBeanName("RecoveryEnvBean"), recoveryBean);
      logger.info(() -> "Registered narayana environment beans to MBean server.");
    }
    if (recoveryManagerService == null) {
      recoveryManagerService = new NarayanaRecoveryManagerService(config.isAutoRecovery());
    }
  }

  void postCorantReadyEvent(@Observes final PostCorantReadyEvent e) {
    recoveryManagerService.initialize();
  }

  void preContainerStopEvent(@Observes final PreContainerStopEvent event) {
    try {
      recoveryManagerService.unInitialize();
      recoveryManagerService = null;
      if (isNotEmpty(mbeanNames)) {
        mbeanNames.forEach(MBeans::deregisterFromMBean);
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, e, () -> "Uninitialize recovery manager service occurred error!");
    }
  }

  String resolveMBeanName(String beanName) {
    String name = applicationName() + ":type=narayana,name=" + beanName;
    mbeanNames.add(name);
    return name;
  }
}
