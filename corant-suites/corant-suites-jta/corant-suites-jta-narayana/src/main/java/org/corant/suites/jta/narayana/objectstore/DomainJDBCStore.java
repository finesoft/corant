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
package org.corant.suites.jta.narayana.objectstore;

import static org.corant.shared.util.Strings.isNotBlank;
import java.util.StringTokenizer;
import org.corant.suites.jta.narayana.objectstore.accessor.DomainJDBCAccess;
import org.corant.suites.jta.narayana.objectstore.driver.AbstractDomainJDBCDriver;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.exceptions.ObjectStoreException;
import com.arjuna.ats.arjuna.logging.tsLogger;

/**
 * corant-suites-jta-narayana
 *
 * @author bingo 下午4:52:52
 *
 */
public class DomainJDBCStore extends AbstractDomainObjectStore {

  /**
   * Create a new JDBCStore
   *
   * @param jdbcStoreEnvironmentBean The environment bean containing the configuration
   * @throws ObjectStoreException In case the store environment bean was not correctly configured
   */
  public DomainJDBCStore(ObjectStoreEnvironmentBean jdbcStoreEnvironmentBean)
      throws ObjectStoreException {
    super(jdbcStoreEnvironmentBean);
    String connectionDetails = jdbcStoreEnvironmentBean.getJdbcAccess();
    String key;
    if (connectionDetails == null) {
      throw new ObjectStoreException(tsLogger.i18NLogger.get_objectstore_JDBCStore_5());
    }
    String impleTableName = DEFAULT_TABLE_NAME;
    final String tablePrefix = jdbcStoreEnvironmentBean.getTablePrefix();
    if (isNotBlank(tablePrefix)) {
      impleTableName = tablePrefix + impleTableName;
    }
    tableName = impleTableName;
    key = connectionDetails + tableName;
    _storeName = storeNames.get(key);
    if (_theImple == null) {
      try {
        DomainJDBCAccess jdbcAccess = DomainJDBCAccess.instance;
        StringTokenizer stringTokenizer = new StringTokenizer(connectionDetails, "|");
        jdbcAccess.initialise(stringTokenizer);
        AbstractDomainJDBCDriver jdbcImple = jdbcAccess.getDriver();
        _storeName = jdbcAccess.getClass().getName() + ":" + tableName;
        _theImple = jdbcImple;
        _theImple.initialise(jdbcAccess, tableName, jdbcStoreEnvironmentBean);
        storeNames.put(key, _storeName);
      } catch (Exception e) {
        tsLogger.i18NLogger.fatal_objectstore_JDBCStore_2(_storeName, e);
        throw new ObjectStoreException(e);
      }
    }
  }

}
