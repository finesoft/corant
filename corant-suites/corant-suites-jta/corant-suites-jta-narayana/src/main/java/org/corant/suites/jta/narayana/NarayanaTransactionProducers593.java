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

import static org.corant.shared.util.CollectionUtils.listOf;
import static org.corant.shared.util.StringUtils.split;
import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Defaults;
import org.corant.shared.ubiquity.Sortable;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import com.arjuna.ats.arjuna.common.CoordinatorEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBeanException;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.RecoveryEnvironmentBean;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

/**
 * corant-suites-jta-narayana
 *
 * @author bingo 下午7:56:21
 *
 */
// @ApplicationScoped
public class NarayanaTransactionProducers593 {

  @Inject
  @ConfigProperty(name = "jta.transaction.timeout", defaultValue = "60")
  Integer transactionTimeout;

  @Inject
  @ConfigProperty(name = "jta.transaction.narayana.transactionStatusManagerPort")
  Optional<Integer> transactionStatusManagerPort;

  @Inject
  @ConfigProperty(name = "jta.transaction.narayana.transactionStatusManagerAddress")
  Optional<String> transactionStatusManagerAddress;

  @Inject
  @ConfigProperty(name = "jta.transaction.narayana.transactionSync", defaultValue = "true")
  Boolean transactionSync = true;

  @Inject
  @ConfigProperty(name = "jta.transaction.narayana.socketProcessIdPort", defaultValue = "0")
  Integer socketProcessIdPort;

  @Inject
  @ConfigProperty(name = "jta.transaction.narayana.nodeIdentifier")
  Optional<String> nodeIdentifier;

  @Inject
  @ConfigProperty(name = "jta.transaction.narayana.xaResourceOrphanFilterClassNames")
  Optional<String> xaResourceOrphanFilterClassNames;

  @Inject
  @ConfigProperty(name = "jta.transaction.narayana.expiryScannerClassNames")
  Optional<String> expiryScannerClassNames;

  @Inject
  @ConfigProperty(name = "jta.transaction.narayana.commitOnePhase", defaultValue = "true")
  Boolean commitOnePhase = true;

  @Inject
  @ConfigProperty(name = "jta.transaction.narayana.xaRecoveryNodes")
  Optional<String> xaRecoveryNodes;

  @Inject
  @ConfigProperty(name = "jta.transaction.narayana.periodicRecoveryPeriod", defaultValue = "120")
  Integer periodicRecoveryPeriod;

  @Inject
  @ConfigProperty(name = "jta.transaction.narayana.recoveryBackoffPeriod", defaultValue = "10")
  Integer recoveryBackoffPeriod;

  @Inject
  @ConfigProperty(name = "jta.transaction.narayana.recoveryPort")
  Optional<Integer> recoveryPort;

  @Inject
  @ConfigProperty(name = "jta.transaction.narayana.recoveryAddress")
  Optional<String> recoveryAddress;

  @Inject
  @ConfigProperty(name = "jta.transaction.narayana.recoveryModuleClassNames")
  Optional<String> recoveryModuleClassNames;

  @Inject
  @ConfigProperty(name = "jta.transaction.narayana.recoveryListener")
  Optional<Boolean> recoveryListener;

  @Inject
  @Any
  Instance<NarayanaConfigurator> configurators;

  @PostConstruct
  void onPostConstruct() {

    if (Thread.currentThread().getContextClassLoader()
        .getResource("jbossts-properties.xml") != null) {
      // Use default setting
      return;
    }

    String dfltObjStoreDir = Defaults.corantUserDir("-narayana-objects").toString();
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

    final CoordinatorEnvironmentBean coordinatorEnvironmentBean =
        BeanPopulator.getDefaultInstance(CoordinatorEnvironmentBean.class);
    coordinatorEnvironmentBean.setDefaultTimeout(transactionTimeout);
    coordinatorEnvironmentBean.setCommitOnePhase(commitOnePhase);

    final CoreEnvironmentBean coreEnvironmentBean =
        BeanPopulator.getDefaultInstance(CoreEnvironmentBean.class);
    nodeIdentifier.ifPresent(t -> {
      try {
        coreEnvironmentBean.setNodeIdentifier(t);
      } catch (CoreEnvironmentBeanException e) {
        throw new CorantRuntimeException(e);
      }
    });
    coreEnvironmentBean.setSocketProcessIdPort(socketProcessIdPort);

    final JTAEnvironmentBean jtaEnvironmentBean =
        BeanPopulator.getDefaultInstance(JTAEnvironmentBean.class);
    xaRecoveryNodes
        .ifPresent(x -> jtaEnvironmentBean.setXaRecoveryNodes(listOf(split(x, ",", true, true))));
    xaResourceOrphanFilterClassNames.ifPresent(x -> jtaEnvironmentBean
        .setXaResourceOrphanFilterClassNames(listOf(split(x, ",", true, true))));

    final RecoveryEnvironmentBean recoveryEnvironmentBean =
        BeanPopulator.getDefaultInstance(RecoveryEnvironmentBean.class);
    recoveryEnvironmentBean.setPeriodicRecoveryPeriod(periodicRecoveryPeriod);
    recoveryEnvironmentBean.setRecoveryBackoffPeriod(recoveryBackoffPeriod);
    recoveryModuleClassNames.ifPresent(x -> recoveryEnvironmentBean
        .setRecoveryModuleClassNames(listOf(split(x, ",", true, true))));
    expiryScannerClassNames.ifPresent(
        x -> recoveryEnvironmentBean.setExpiryScannerClassNames(listOf(split(x, ",", true, true))));

    recoveryPort.ifPresent(recoveryEnvironmentBean::setRecoveryPort);
    recoveryAddress.ifPresent(recoveryEnvironmentBean::setRecoveryAddress);
    transactionStatusManagerPort
        .ifPresent(recoveryEnvironmentBean::setTransactionStatusManagerPort);
    transactionStatusManagerAddress
        .ifPresent(recoveryEnvironmentBean::setTransactionStatusManagerAddress);
    recoveryListener.ifPresent(recoveryEnvironmentBean::setRecoveryListener);

    if (!configurators.isUnsatisfied()) {
      configurators.stream().sorted(Sortable::compare).forEachOrdered(cfgr -> {
        cfgr.configCoreEnvironment(coreEnvironmentBean);
        cfgr.configCoordinatorEnvironment(coordinatorEnvironmentBean);
        cfgr.configRecoveryEnvironment(recoveryEnvironmentBean);
        cfgr.configObjectStoreEnvironment(nullActionStoreObjectStoreEnvironmentBean, null);
        cfgr.configObjectStoreEnvironment(defaultActionStoreObjectStoreEnvironmentBean, "default");
        cfgr.configObjectStoreEnvironment(stateStoreObjectStoreEnvironmentBean, "stateStore");
        cfgr.configObjectStoreEnvironment(communicationStoreObjectStoreEnvironmentBean,
            "communicationStore");
      });
    }
  }

  //
  // void register(@Observes PostCorantReadyEvent event, InitialContext ctx,
  // TransactionManager transactionManager) {
  // try {
  // ctx.bind(jtaPropertyManager.getJTAEnvironmentBean().getTransactionManagerJNDIContext(),
  // transactionManager);
  // } catch (NamingException e) {
  // throw new CorantRuntimeException(e,
  // "An error occurred while registering Transaction Manager to JNDI");
  // }
  // }
  //
  // @Produces
  // @ApplicationScoped
  // TransactionManager transactionManager() {
  // return com.arjuna.ats.jta.TransactionManager.transactionManager();
  // }
  //
  // @Produces
  // @ApplicationScoped
  // TransactionSynchronizationRegistry transactionSynchronizationRegistry() {
  // return new TransactionSynchronizationRegistryImple();
  // }
  //
  // @Produces
  // @Dependent
  // UserTransaction userTransaction() {
  // return com.arjuna.ats.jta.UserTransaction.userTransaction();
  // }
  //
  // @Produces
  // @ApplicationScoped
  // UserTransactionRegistry userTransactionRegistry() {
  // return new UserTransactionRegistry();
  // }
  //
  // @Produces
  // @ApplicationScoped
  // XAResourceRecoveryRegistry xaResourceRecoveryRegistry() {
  // return new RecoveryManagerService();
  // }
  //
  // @Produces
  // @ApplicationScoped
  // JBossXATerminator xaTerminator() {
  // return new XATerminator();
  // }
}
