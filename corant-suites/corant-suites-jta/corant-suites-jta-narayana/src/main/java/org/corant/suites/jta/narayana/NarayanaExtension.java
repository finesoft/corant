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

import static org.corant.kernel.util.Instances.select;
import static org.corant.shared.util.ClassUtils.defaultClassLoader;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.StreamUtils.streamOf;
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
import javax.enterprise.inject.spi.Extension;
import javax.inject.Singleton;
import javax.naming.NamingException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionScoped;
import javax.transaction.UserTransaction;
import org.corant.config.spi.Sortable;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Defaults;
import org.eclipse.microprofile.config.ConfigProvider;
import com.arjuna.ats.arjuna.common.CoordinatorEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.RecoveryEnvironmentBean;
import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.arjuna.coordinator.CheckedAction;
import com.arjuna.ats.arjuna.logging.tsLogger;
import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.jbossatx.jta.RecoveryManagerService;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.ats.jta.utils.JNDIManager;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

/**
 * corant-suites-jta-narayana
 *
 * FIXME org.jboss.logging.annotations.Message.Format.MESSAGE_FORMAT version issue!!!!
 *
 * @author bingo 下午7:19:18
 *
 */
public class NarayanaExtension implements Extension {

  public static final String JTA_BIND_TO_JNDI_CFG = "jta.bind-to-jndi";
  public static final String JTA_TRANSACTION_TIMEOUT = "jta.transaction.timeout";
  public static final String JTA_AUTO_START_RECOVERY = "jta.auto-start-recovery";
  public static final String JTA_NARAYANA_OBJECTS_STORE = "jta.narayana.objects-store";

  protected final Logger logger = Logger.getLogger(this.getClass().toString());

  void afterBeanDiscovery(@Observes final AfterBeanDiscovery event, final BeanManager beanManager) {
    if (ConfigProvider.getConfig().getOptionalValue(JTA_BIND_TO_JNDI_CFG, Boolean.class)
        .orElse(false)) {
      try {
        JNDIManager.bindJTAImplementation();
        logger.info(() -> "Bind JTA implementations to Jndi.");
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

      if (ConfigProvider.getConfig().getOptionalValue(JTA_AUTO_START_RECOVERY, Boolean.class)
          .orElse(false)) {
        event.<RecoveryManagerService>addBean()
            .addTransitiveTypeClosure(RecoveryManagerService.class)
            .addQualifiers(Any.Literal.INSTANCE, Default.Literal.INSTANCE,
                NamedLiteral.of("narayana-jta"))
            .scope(Singleton.class).createWith(cc -> {
              RecoveryManager.manager(RecoveryManager.DIRECT_MANAGEMENT).initialize();
              RecoveryManagerService rms = new RecoveryManagerService();
              rms.create();
              rms.start();
              return rms;
            }).disposeWith((t, inst) -> t.destroy());
      }
    }
  }

  void beforeBeanDiscovery(@Observes final BeforeBeanDiscovery event,
      final BeanManager beanManager) {
    String dfltObjStoreDir =
        ConfigProvider.getConfig().getOptionalValue(JTA_NARAYANA_OBJECTS_STORE, String.class)
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

    ConfigProvider.getConfig().getOptionalValue(JTA_TRANSACTION_TIMEOUT, Integer.class)
        .ifPresent(t -> {
          coordinatorEnvironmentBean.setDefaultTimeout(t.intValue());
          logger.warning(
              () -> "Use thread interrupt checked action for narayana, It can cause inconsistencies.");
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
    streamOf(ServiceLoader.load(NarayanaConfigurator.class, defaultClassLoader()))
        .sorted(Sortable::compare).forEach(cfgr -> {
          logger.info(() -> String.format("Use customer narayana configurator %s.",
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
