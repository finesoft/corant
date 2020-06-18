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

import static org.corant.shared.normal.Names.applicationName;
import static org.corant.shared.util.Classes.defaultClassLoader;
import static org.corant.shared.util.Conversions.toInteger;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.suites.cdi.Instances.select;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Hashtable;
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
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
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
import org.corant.shared.normal.Defaults;
import org.corant.shared.ubiquity.Sortable;
import org.corant.suites.jta.shared.TransactionExtension;
import com.arjuna.ats.arjuna.common.CoordinatorEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.RecoveryEnvironmentBean;
import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.arjuna.coordinator.CheckedAction;
import com.arjuna.ats.arjuna.logging.tsLogger;
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

  synchronized void beforeBeanDiscovery(@Observes final BeforeBeanDiscovery event) {
    String dfltObjStoreDir = getConfig().getObjectsStore()
        .orElse(Defaults.corantUserDir("-narayana-objects").toString());
    final ObjectStoreEnvironmentBean nullActionStoreObjectStoreEnvironmentBean =
        BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, null);
    nullActionStoreObjectStoreEnvironmentBean.setObjectStoreDir(dfltObjStoreDir);
    final ObjectStoreEnvironmentBean defaultActionStoreObjectStoreEnvironmentBean =
        BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "default");
    defaultActionStoreObjectStoreEnvironmentBean.setObjectStoreDir(dfltObjStoreDir);
    final ObjectStoreEnvironmentBean stateStoreObjectStoreEnvironmentBean =
        BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "stateStore");
    stateStoreObjectStoreEnvironmentBean.setObjectStoreDir(dfltObjStoreDir);
    final ObjectStoreEnvironmentBean communicationStoreObjectStoreEnvironmentBean =
        BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "communicationStore");
    communicationStoreObjectStoreEnvironmentBean.setObjectStoreDir(dfltObjStoreDir);

    logger.info(() -> String.format("Specify the narayana object store path %s.", dfltObjStoreDir));

    final CoordinatorEnvironmentBean coordinatorEnvironmentBean =
        BeanPopulator.getDefaultInstance(CoordinatorEnvironmentBean.class);

    getConfig().getTimeout().ifPresent(t -> {
      coordinatorEnvironmentBean.setDefaultTimeout(toInteger(t.getSeconds()));
      logger.fine(
          () -> "Use thread interrupt checked action for narayana, it can cause inconsistencies.");
      coordinatorEnvironmentBean.setAllowCheckedActionFactoryOverride(true);
      coordinatorEnvironmentBean
          .setCheckedActionFactory((txId, actionType) -> new InterruptCheckedAction());
    });

    final CoreEnvironmentBean coreEnvironmentBean =
        BeanPopulator.getDefaultInstance(CoreEnvironmentBean.class);
    final JTAEnvironmentBean jtaEnvironmentBean =
        BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class);
    final RecoveryEnvironmentBean recoveryEnvironmentBean =
        BeanPopulator.getDefaultInstance(RecoveryEnvironmentBean.class);

    if (getConfig().isAutoRecovery()) {
      getConfig().getAutoRecoveryPeriod().ifPresent(
          p -> recoveryEnvironmentBean.setPeriodicRecoveryPeriod(toInteger(p.getSeconds())));
      getConfig().getAutoRecoveryInitOffset().ifPresent(p -> recoveryEnvironmentBean
          .setPeriodicRecoveryInitilizationOffset(toInteger(p.getSeconds())));
      getConfig().getAutoRecoveryBackoffPeriod().ifPresent(
          p -> recoveryEnvironmentBean.setRecoveryBackoffPeriod(toInteger(p.getSeconds())));
    }

    streamOf(ServiceLoader.load(NarayanaConfigurator.class, defaultClassLoader()))
        .sorted(Sortable::compare).forEach(cfgr -> {
          logger.fine(() -> String.format("Use customer narayana configurator %s.",
              cfgr.getClass().getName()));
          cfgr.configCoreEnvironment(coreEnvironmentBean);
          cfgr.configCoordinatorEnvironment(coordinatorEnvironmentBean);
          cfgr.configRecoveryEnvironment(recoveryEnvironmentBean);
          cfgr.configObjectStoreEnvironment(nullActionStoreObjectStoreEnvironmentBean, null);
          cfgr.configObjectStoreEnvironment(defaultActionStoreObjectStoreEnvironmentBean,
              "default");
          cfgr.configObjectStoreEnvironment(stateStoreObjectStoreEnvironmentBean, "stateStore");
          cfgr.configObjectStoreEnvironment(communicationStoreObjectStoreEnvironmentBean,
              "communicationStore");
          cfgr.configJTAEnvironmentBean(jtaEnvironmentBean);
        });

    if (recoveryManagerService == null) {
      recoveryManagerService = new NarayanaRecoveryManagerService(config.isAutoRecovery());
    }
  }

  void postCorantReadyEvent(@Observes final PostCorantReadyEvent e) {
    recoveryManagerService.initialize();
    registerToMBean();
  }

  void preContainerStopEvent(@Observes final PreContainerStopEvent event) {
    try {
      recoveryManagerService.unInitialize();
      recoveryManagerService = null;
    } catch (Exception e) {
      logger.log(Level.WARNING, e, () -> "Uninitialize recovery manager service occurred error!");
    }
  }

  synchronized void registerToMBean() {
    if (config.isEnableMbean()) {
      registerToMBean("ObjectStoreEnvironmentBean-nullAction",
          BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, null));
      registerToMBean("ObjectStoreEnvironmentBean-default",
          BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "default"));
      registerToMBean("ObjectStoreEnvironmentBean-stateStore",
          BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "stateStore"));
      registerToMBean("ObjectStoreEnvironmentBean-communicationStore",
          BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "communicationStore"));
      registerToMBean("CoordinatorEnvironmentBean",
          BeanPopulator.getDefaultInstance(CoordinatorEnvironmentBean.class));
      registerToMBean("JTAEnvironmentBean",
          BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class));
      registerToMBean("RecoveryEnvironmentBean",
          BeanPopulator.getDefaultInstance(RecoveryEnvironmentBean.class));
      logger.info(() -> "Registered narayana environment beans to MBean server.");
    }
  }

  void registerToMBean(String beanName, Object bean) {
    ObjectName objectName = null;
    try {
      objectName = new ObjectName(applicationName() + ":type=narayana,name=" + beanName);
    } catch (MalformedObjectNameException ex) {
      throw new CorantRuntimeException(ex);
    }

    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    try {
      if (server.isRegistered(objectName)) {
        server.unregisterMBean(objectName);
      }
      server.registerMBean(bean, objectName);
    } catch (InstanceAlreadyExistsException | MBeanRegistrationException
        | NotCompliantMBeanException | InstanceNotFoundException ex) {
      throw new CorantRuntimeException(ex);
    }
  }

  public static class InterruptCheckedAction extends CheckedAction {

    protected final Logger logger = Logger.getLogger(this.getClass().toString());

    @Override
    public synchronized void check(boolean isCommit, Uid actUid,
        @SuppressWarnings("rawtypes") Hashtable list) {
      if (isCommit) {
        tsLogger.i18NLogger.warn_coordinator_CheckedAction_1(actUid, Integer.toString(list.size()));
      } else {
        try {
          for (Object item : list.values()) {
            if (item instanceof Thread) {
              Thread thread = (Thread) item;
              try {
                Throwable t =
                    new Throwable("STACK TRACE OF ACTIVE THREAD IN TERMINATING TRANSACTION");
                t.setStackTrace(thread.getStackTrace());
                logger.log(Level.INFO, t,
                    () -> String.format("Transaction %s is %s with active thread %s",
                        actUid.toString(), isCommit ? "committing" : "aborting", thread.getName()));
              } catch (Exception e) {
                logger.log(Level.WARNING, e,
                    () -> String.format(
                        "Narayana extension checked action execute failed on %s , isCommit %s .",
                        actUid, isCommit));
              }
              thread.interrupt();
            }
          }
        } catch (Exception e) {
          logger.log(Level.WARNING, e,
              () -> String.format(
                  "Narayana extension checked action execute failed on %s , isCommit %s .", actUid,
                  isCommit));
        }
        tsLogger.i18NLogger.warn_coordinator_CheckedAction_2(actUid, Integer.toString(list.size()));
      }
    }
  }
}
