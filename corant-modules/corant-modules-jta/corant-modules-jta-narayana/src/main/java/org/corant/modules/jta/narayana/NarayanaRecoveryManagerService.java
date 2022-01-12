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
package org.corant.modules.jta.narayana;

import static org.corant.shared.util.Empties.isNotEmpty;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import org.corant.Corant;
import org.corant.context.ContainerEvents.PreContainerStopEvent;
import org.corant.kernel.event.PostCorantReadyAsyncEvent;
import org.corant.modules.jta.shared.TransactionIntegration;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Services;
import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule;
import com.arjuna.ats.jbossatx.jta.RecoveryManagerService;

/**
 * corant-modules-jta-narayana
 *
 * @author bingo 上午12:02:18
 *
 */
@ApplicationScoped
public class NarayanaRecoveryManagerService extends RecoveryManagerService {

  protected final Logger logger = Logger.getLogger(NarayanaRecoveryManagerService.class.getName());
  protected final List<NarayanaXAResourceRecoveryHelper> helpers = new CopyOnWriteArrayList<>();

  @Inject
  protected NarayanaExtension extension;

  protected volatile boolean initialized = false;
  protected volatile boolean ready = false;

  protected void initialize() {
    // initialize the recovery manager
    if (!initialized) {
      synchronized (NarayanaRecoveryManagerService.class) {
        if (!initialized) {
          if (extension.getConfig().isAutoRecovery()) {
            RecoveryManager.manager(RecoveryManager.INDIRECT_MANAGEMENT);
          } else {
            RecoveryManager.manager(RecoveryManager.DIRECT_MANAGEMENT);
          }
          initialized = true;
        }
      }
    }

    if (!ready) {
      synchronized (NarayanaRecoveryManagerService.class) {
        if (!ready) {
          // start the recovery manager
          if (extension.getConfig().isAutoRecovery()) {
            logger.info(() -> "Initialize automatic JTA recovery processes.");
          } else {
            logger.info(() -> "Initialize manual JTA recovery processes.");
          }
          create();
          helpers.clear();
          Services.select(TransactionIntegration.class, Corant.current().getClassLoader())
              .map(NarayanaXAResourceRecoveryHelper::new).forEach(helpers::add);
          XARecoveryModule xaRecoveryModule = XARecoveryModule.getRegisteredXARecoveryModule();
          if (xaRecoveryModule != null && isNotEmpty(helpers)) {
            helpers.forEach(xaRecoveryModule::addXAResourceRecoveryHelper);
          }
          if (extension.getConfig().isAutoRecovery()) {
            start();
            logger.info(() -> "JTA automatic recovery processes has been started.");
          }
          ready = true;
        }
      }
    }
  }

  protected void postCorantReadyEvent(@ObservesAsync final PostCorantReadyAsyncEvent e) {
    initialize();
  }

  protected void preContainerStopEvent(@Observes final PreContainerStopEvent event) {
    try {
      unInitialize();
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  protected void unInitialize() throws Exception {
    if (ready) {
      if (extension.getConfig().isAutoRecovery()) {
        stop();
      }
      helpers.forEach(helper -> {
        XARecoveryModule.getRegisteredXARecoveryModule().removeXAResourceRecoveryHelper(helper);
        helper.destory();
      });
      helpers.clear();
      if (extension.getConfig().isAutoRecovery()) {
        logger.info(() -> "JTA automatic recovery processes has been stopped.");
      }
    }
  }

}
