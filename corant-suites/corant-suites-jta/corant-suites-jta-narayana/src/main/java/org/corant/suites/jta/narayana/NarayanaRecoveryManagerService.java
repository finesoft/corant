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

import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.StreamUtils.streamOf;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import org.corant.Corant;
import org.corant.suites.jta.shared.TransactionIntegration;
import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule;
import com.arjuna.ats.jbossatx.jta.RecoveryManagerService;

/**
 * corant-suites-jta-narayana
 *
 * @author bingo 上午12:02:18
 *
 */
public class NarayanaRecoveryManagerService extends RecoveryManagerService {

  protected final Logger logger = Logger.getLogger(NarayanaRecoveryManagerService.class.getName());
  protected final boolean autoRecovery;
  protected final List<NarayanaXAResourceRecoveryHelper> helpers = new CopyOnWriteArrayList<>();

  public NarayanaRecoveryManagerService(boolean autoRecovery) {
    this.autoRecovery = autoRecovery;
    if (autoRecovery) {
      RecoveryManager.manager(RecoveryManager.INDIRECT_MANAGEMENT);
    } else {
      RecoveryManager.manager(RecoveryManager.DIRECT_MANAGEMENT);
    }
  }

  public void initialize() {
    if (autoRecovery) {
      logger.info(() -> "Initialize automatic JTA recovery processes.");
    } else {
      logger.info(() -> "Initialize manual JTA recovery processes.");
    }
    create();
    helpers.clear();
    streamOf(ServiceLoader.load(TransactionIntegration.class, Corant.current().getClassLoader()))
        .map(NarayanaXAResourceRecoveryHelper::new).forEach(helpers::add);
    XARecoveryModule xaRecoveryModule = XARecoveryModule.getRegisteredXARecoveryModule();
    if (xaRecoveryModule != null && isNotEmpty(helpers)) {
      helpers.stream().forEach(xaRecoveryModule::addXAResourceRecoveryHelper);
    }
    if (autoRecovery) {
      start();
      logger.info(() -> "JTA automatic recovery processes has been started.");
    }
  }

  public void unInitialize() throws Exception {
    stop();
    helpers.stream().forEach(helper -> {
      XARecoveryModule.getRegisteredXARecoveryModule().removeXAResourceRecoveryHelper(helper);
      helper.destory();
    });
    if (autoRecovery) {
      logger.info(() -> "JTA automatic recovery processes has been stopped.");
    } else {
      logger.info(() -> "JTA manual recovery processes has been stopped.");
    }
  }

}
