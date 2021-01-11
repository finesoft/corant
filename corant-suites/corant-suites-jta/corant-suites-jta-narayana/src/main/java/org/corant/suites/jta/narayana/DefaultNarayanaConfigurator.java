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

import static org.corant.shared.util.Conversions.toInteger;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.arjuna.ats.arjuna.common.CoordinatorEnvironmentBean;
import com.arjuna.ats.arjuna.common.CoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.RecoveryEnvironmentBean;
import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.arjuna.coordinator.CheckedAction;
import com.arjuna.ats.arjuna.logging.tsLogger;
import com.arjuna.ats.jta.common.JTAEnvironmentBean;

/**
 * corant-suites-jta-narayana
 *
 * @author bingo 上午9:53:18
 *
 */
public class DefaultNarayanaConfigurator implements NarayanaConfigurator {

  protected final Logger logger = Logger.getLogger(this.getClass().toString());

  @Override
  public void configCoordinatorEnvironment(CoordinatorEnvironmentBean bean,
      NarayanaTransactionConfig config) {
    config.getTimeout().ifPresent(t -> {
      bean.setDefaultTimeout(toInteger(t.getSeconds()));
      logger.fine(
          () -> "Use thread interrupt checked action for narayana, it can cause inconsistencies.");
      bean.setAllowCheckedActionFactoryOverride(true);
      bean.setCheckedActionFactory((txId, actionType) -> new InterruptCheckedAction());
    });
    bean.setTransactionStatusManagerEnable(config.getCeTransactionStatusManagerEnable());
  }

  @Override
  public void configCoreEnvironment(CoreEnvironmentBean bean, NarayanaTransactionConfig config) {
    // TODO Auto-generated method stub

  }

  @Override
  public void configJTAEnvironmentBean(JTAEnvironmentBean bean, NarayanaTransactionConfig config) {
    // TODO Auto-generated method stub

  }

  @Override
  public void configObjectStoreEnvironment(ObjectStoreEnvironmentBean bean, String name,
      NarayanaTransactionConfig config) {
    if (name == null || "default".equalsIgnoreCase(name)) {
      bean.setDropTable(config.getObeDefaultDropTable());
      bean.setObjectStoreDir(config.getObeDefaultObjectStoreDir());
      bean.setJdbcAccess(config.getObeDefaultJdbcAccess());
      bean.setObjectStoreType(config.getObeDefaultObjectStoreType());
      bean.setTablePrefix(config.getObeDefaultTablePrefix());
    } else if ("stateStore".equalsIgnoreCase(name)) {
      bean.setDropTable(config.getObeStateStoreDropTable());
      bean.setObjectStoreDir(config.getObeStateStoreObjectStoreDir());
      bean.setJdbcAccess(config.getObeStateStoreJdbcAccess());
      bean.setObjectStoreType(config.getObeStateStoreObjectStoreType());
      bean.setTablePrefix(config.getObeStateStoreTablePrefix());
    } else {
      bean.setDropTable(config.getObeCommunicationStoreDropTable());
      bean.setObjectStoreDir(config.getObeCommunicationStoreObjectStoreDir());
      bean.setJdbcAccess(config.getObeCommunicationStoreJdbcAccess());
      bean.setObjectStoreType(config.getObeCommunicationStoreObjectStoreType());
      bean.setTablePrefix(config.getObeCommunicationStoreTablePrefix());
    }
  }

  @Override
  public void configRecoveryEnvironment(RecoveryEnvironmentBean bean,
      NarayanaTransactionConfig config) {
    if (config.isAutoRecovery()) {
      config.getAutoRecoveryPeriod()
          .ifPresent(p -> bean.setPeriodicRecoveryPeriod(toInteger(p.getSeconds())));
      config.getAutoRecoveryInitOffset()
          .ifPresent(p -> bean.setPeriodicRecoveryInitilizationOffset(toInteger(p.getSeconds())));
      config.getAutoRecoveryBackoffPeriod()
          .ifPresent(p -> bean.setRecoveryBackoffPeriod(toInteger(p.getSeconds())));
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
                    () -> String.format("Transaction %s is %s with active thread %s.",
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
