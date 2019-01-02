/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.suites.jta.narayana;

import java.nio.file.Paths;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;
import org.corant.kernel.event.PostCorantReadyEvent;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Names;
import org.jboss.tm.JBossXATerminator;
import org.jboss.tm.XAResourceRecoveryRegistry;
import org.jboss.tm.usertx.UserTransactionRegistry;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.internal.jbossatx.jta.jca.XATerminator;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple;
import com.arjuna.ats.jbossatx.jta.RecoveryManagerService;
import com.arjuna.ats.jta.common.jtaPropertyManager;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

/**
 * corant-suites-jta-narayana
 *
 * @author bingo 下午7:56:21
 *
 */
@ApplicationScoped
public class NarayanaTransactionProducers {

  @PostConstruct
  void onPostConstruct() {
    // FIXME,here to config
    String objectStoreDir = Paths.get(System.getProperty("user.home"))
        .resolve("." + Names.CORANT + "-narayana-objects").toString();
    final ObjectStoreEnvironmentBean nullActionStoreObjectStoreEnvironmentBean =
        BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, null);
    nullActionStoreObjectStoreEnvironmentBean.setObjectStoreDir(objectStoreDir);
    final ObjectStoreEnvironmentBean defaultActionStoreObjectStoreEnvironmentBean =
        BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "default");
    defaultActionStoreObjectStoreEnvironmentBean.setObjectStoreDir(objectStoreDir);
    final ObjectStoreEnvironmentBean stateStoreObjectStoreEnvironmentBean =
        BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "stateStore");
    stateStoreObjectStoreEnvironmentBean.setObjectStoreDir(objectStoreDir);
    final ObjectStoreEnvironmentBean communicationStoreObjectStoreEnvironmentBean =
        BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, "communicationStore");
    communicationStoreObjectStoreEnvironmentBean.setObjectStoreDir(objectStoreDir);
  }

  void register(@Observes PostCorantReadyEvent event, InitialContext ctx,
      TransactionManager transactionManager) {
    try {
      ctx.bind(jtaPropertyManager.getJTAEnvironmentBean().getTransactionManagerJNDIContext(),
          transactionManager);
    } catch (NamingException e) {
      throw new CorantRuntimeException(e,
          "An error occurred while registering Transaction Manager to JNDI");
    }
  }

  @Produces
  @ApplicationScoped
  TransactionManager transactionManager() {
    return com.arjuna.ats.jta.TransactionManager.transactionManager();
  }

  @Produces
  @ApplicationScoped
  TransactionSynchronizationRegistry transactionSynchronizationRegistry() {
    return new TransactionSynchronizationRegistryImple();
  }

  @Produces
  @ApplicationScoped
  UserTransaction userTransaction() {
    return com.arjuna.ats.jta.UserTransaction.userTransaction();
  }

  @Produces
  @ApplicationScoped
  UserTransactionRegistry userTransactionRegistry() {
    return new UserTransactionRegistry();
  }

  @Produces
  @ApplicationScoped
  XAResourceRecoveryRegistry xaResourceRecoveryRegistry() {
    return new RecoveryManagerService();
  }

  @Produces
  @ApplicationScoped
  JBossXATerminator xaTerminator() {
    return new XATerminator();
  }
}
