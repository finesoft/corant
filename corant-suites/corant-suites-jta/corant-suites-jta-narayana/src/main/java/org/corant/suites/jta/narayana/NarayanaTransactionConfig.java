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

import static org.corant.shared.util.Maps.getMapBoolean;
import static org.corant.shared.util.Maps.getMapString;
import static org.corant.shared.util.Maps.getOptMapObject;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.corant.config.declarative.ConfigKeyItem;
import org.corant.config.declarative.ConfigKeyRoot;
import org.corant.config.declarative.DeclarativePattern;
import org.corant.shared.normal.Defaults;
import org.corant.shared.util.Conversions;
import org.corant.suites.jta.shared.TransactionConfig;
import com.arjuna.ats.internal.arjuna.objectstore.ShadowNoFileLockStore;

/**
 * corant-suites-jta-narayana
 *
 * @author bingo 下午9:23:19
 *
 */
@ConfigKeyRoot(value = "jta.transaction", keyIndex = 2, ignoreNoAnnotatedItem = false)
public class NarayanaTransactionConfig extends TransactionConfig {

  private static final long serialVersionUID = -1353597648110310051L;

  static final String DFLT_OBJECT_STORE_DIR =
      Defaults.corantUserDir("-narayana-objects").toString();
  static final String DFLT_OBJECT_STORE_TYPE = ShadowNoFileLockStore.class.getName();

  @ConfigKeyItem
  Optional<Duration> autoRecoveryBackoffPeriod;

  @ConfigKeyItem
  Optional<Duration> autoRecoveryPeriod;

  @ConfigKeyItem
  Optional<Duration> autoRecoveryInitOffset;

  @ConfigKeyItem(pattern = DeclarativePattern.PREFIX)
  Map<String, String> objectStoreEnvironment = new HashMap<>();

  @ConfigKeyItem(pattern = DeclarativePattern.PREFIX)
  Map<String, String> coordinatorEnvironment = new HashMap<>();

  @ConfigKeyItem(defaultValue = "false")
  boolean enableMbean;

  public Optional<Duration> getAutoRecoveryBackoffPeriod() {
    return autoRecoveryBackoffPeriod;
  }

  public Optional<Duration> getAutoRecoveryInitOffset() {
    return autoRecoveryInitOffset;
  }

  public Optional<Duration> getAutoRecoveryPeriod() {
    return autoRecoveryPeriod;
  }

  public boolean getCeTransactionStatusManagerEnable() {
    return getOptMapObject(getCoordinatorEnvironment(), "transactionStatusManagerEnable",
        Conversions::toBoolean).orElse(Boolean.TRUE);
  }

  public Map<String, String> getCoordinatorEnvironment() {
    return coordinatorEnvironment;
  }

  public boolean getObeCommunicationStoreDropTable() {
    return getOptMapObject(getObjectStoreEnvironment(), "communicationStore.dropTable",
        Conversions::toBoolean).orElse(getObeDefaultDropTable());
  }

  public String getObeCommunicationStoreJdbcAccess() {
    return getOptMapObject(getObjectStoreEnvironment(), "communicationStore.jdbcAccess",
        Conversions::toString).orElse(getObeDefaultJdbcAccess());
  }

  public String getObeCommunicationStoreObjectStoreDir() {
    return getOptMapObject(getObjectStoreEnvironment(), "communicationStore.objectStoreDir",
        Conversions::toString).orElse(getObeDefaultObjectStoreDir());
  }

  public String getObeCommunicationStoreObjectStoreType() {
    return getOptMapObject(getObjectStoreEnvironment(), "communicationStore.objectStoreType",
        Conversions::toString).orElse(getObeDefaultObjectStoreType());
  }

  public String getObeCommunicationStoreTablePrefix() {
    return getOptMapObject(getObjectStoreEnvironment(), "communicationStore.tablePrefix",
        Conversions::toString).orElse(getObeDefaultTablePrefix());
  }

  public boolean getObeDefaultDropTable() {
    return getMapBoolean(getObjectStoreEnvironment(), "dropTable");
  }

  public String getObeDefaultJdbcAccess() {
    return getObjectStoreEnvironment().get("jdbcAccess");
  }

  public String getObeDefaultObjectStoreDir() {
    return getMapString(getObjectStoreEnvironment(), "objectStoreDir", DFLT_OBJECT_STORE_DIR);
  }

  public String getObeDefaultObjectStoreType() {
    return getMapString(getObjectStoreEnvironment(), "objectStoreType", DFLT_OBJECT_STORE_TYPE);
  }

  public String getObeDefaultTablePrefix() {
    return getObjectStoreEnvironment().get("tablePrefix");
  }

  public boolean getObeStateStoreDropTable() {
    return getOptMapObject(getObjectStoreEnvironment(), "stateStore.dropTable",
        Conversions::toBoolean).orElse(getObeDefaultDropTable());
  }

  public String getObeStateStoreJdbcAccess() {
    return getOptMapObject(getObjectStoreEnvironment(), "stateStore.jdbcAccess",
        Conversions::toString).orElse(getObeDefaultJdbcAccess());
  }

  public String getObeStateStoreObjectStoreDir() {
    return getOptMapObject(getObjectStoreEnvironment(), "stateStore.objectStoreDir",
        Conversions::toString).orElse(getObeDefaultObjectStoreDir());
  }

  public String getObeStateStoreObjectStoreType() {
    return getOptMapObject(getObjectStoreEnvironment(), "stateStore.objectStoreType",
        Conversions::toString).orElse(getObeDefaultObjectStoreType());
  }

  public String getObeStateStoreTablePrefix() {
    return getOptMapObject(getObjectStoreEnvironment(), "stateStore.tablePrefix",
        Conversions::toString).orElse(getObeDefaultTablePrefix());
  }

  public Map<String, String> getObjectStoreEnvironment() {
    return objectStoreEnvironment;
  }

  public boolean isEnableMbean() {
    return enableMbean;
  }
}
