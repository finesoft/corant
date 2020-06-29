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
package org.corant.suites.jta.narayana.extend;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.arjuna.exceptions.ObjectStoreException;
import com.arjuna.ats.arjuna.logging.tsLogger;
import com.arjuna.ats.arjuna.objectstore.ObjectStoreAPI;
import com.arjuna.ats.arjuna.objectstore.StateStatus;
import com.arjuna.ats.arjuna.objectstore.jdbc.JDBCAccess;
import com.arjuna.ats.arjuna.state.InputBuffer;
import com.arjuna.ats.arjuna.state.InputObjectState;
import com.arjuna.ats.arjuna.state.OutputBuffer;
import com.arjuna.ats.arjuna.state.OutputObjectState;
import com.arjuna.ats.internal.arjuna.objectstore.jdbc.JDBCImple_driver;

/**
 * corant-suites-jta-narayana
 *
 * @author bingo 下午4:52:52
 *
 */
public class DomainJDBCStore implements ObjectStoreAPI {

  private static final String DEFAULT_TABLE_NAME = "JBossTSTxTable";
  private static Map<String, String> storeNames = new HashMap<>();
  protected JDBCImple_driver _theImple;
  protected String tableName;
  protected final ObjectStoreEnvironmentBean jdbcStoreEnvironmentBean;
  private String _storeName;

  /**
   * Create a new JDBCStore
   *
   * @param jdbcStoreEnvironmentBean The environment bean containing the configuration
   * @throws ObjectStoreException In case the store environment bean was not correctly configured
   */
  public DomainJDBCStore(ObjectStoreEnvironmentBean jdbcStoreEnvironmentBean)
      throws ObjectStoreException {
    this.jdbcStoreEnvironmentBean = jdbcStoreEnvironmentBean;
    String connectionDetails = jdbcStoreEnvironmentBean.getJdbcAccess();
    String key;

    if (connectionDetails == null) {
      throw new ObjectStoreException(tsLogger.i18NLogger.get_objectstore_JDBCStore_5());
    }
    String impleTableName = DEFAULT_TABLE_NAME;
    final String tablePrefix = jdbcStoreEnvironmentBean.getTablePrefix();
    if (tablePrefix != null && tablePrefix.length() > 0) {
      impleTableName = tablePrefix + impleTableName;
    }
    tableName = impleTableName;
    key = connectionDetails + tableName;
    _storeName = storeNames.get(key);
    if (_theImple == null) {
      try {
        StringTokenizer stringTokenizer = new StringTokenizer(connectionDetails, ";");
        JDBCAccess jdbcAccess =
            (JDBCAccess) Class.forName(stringTokenizer.nextToken()).newInstance();
        AbstractDomainJDBCDriver jdbcImple =
            (AbstractDomainJDBCDriver) Class.forName(stringTokenizer.nextToken()).newInstance();
        jdbcAccess.initialise(stringTokenizer);
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

  @Override
  public boolean allObjUids(String s, InputObjectState buff) throws ObjectStoreException {
    return allObjUids(s, buff, StateStatus.OS_UNKNOWN);
  }

  @Override
  public boolean allObjUids(String tName, InputObjectState state, int match)
      throws ObjectStoreException {
    return _theImple.allObjUids(tName, state, match);
  }

  @Override
  public boolean allTypes(InputObjectState foundTypes) throws ObjectStoreException {
    return _theImple.allTypes(foundTypes);
  }

  @Override
  public boolean commit_state(Uid objUid, String tName) throws ObjectStoreException {
    return _theImple.commit_state(objUid, tName);
  }

  @Override
  public int currentState(Uid objUid, String tName) throws ObjectStoreException {
    return _theImple.currentState(objUid, tName);
  }

  /**
   * Does this store need to do the full write_uncommitted/commit protocol?
   *
   * @return <code>true</code> if full commit is needed, <code>false</code> otherwise.
   */

  @Override
  public boolean fullCommitNeeded() {
    return true;
  }

  @Override
  public String getStoreName() {
    return _storeName;
  }

  @Override
  public boolean hide_state(Uid objUid, String tName) throws ObjectStoreException {
    return _theImple.hide_state(objUid, tName);
  }

  /**
   * Is the current state of the object the same as that provided as the last parameter?
   *
   * @param u The object to work on.
   * @param tn The type of the object.
   * @param st The expected type of the object.
   *
   * @return <code>true</code> if the current state is as expected, <code>false</code> otherwise.
   */

  @Override
  public boolean isType(Uid u, String tn, int st) throws ObjectStoreException {
    return currentState(u, tn) == st;
  }

  public synchronized void packInto(OutputBuffer buff) throws IOException {
    buff.packString(tableName);
  }

  @Override
  public InputObjectState read_committed(Uid storeUid, String tName) throws ObjectStoreException {
    return _theImple.read_state(storeUid, tName, StateStatus.OS_COMMITTED);
  }

  @Override
  public InputObjectState read_uncommitted(Uid storeUid, String tName) throws ObjectStoreException {
    return _theImple.read_state(storeUid, tName, StateStatus.OS_UNCOMMITTED);
  }

  @Override
  public boolean remove_committed(Uid storeUid, String tName) throws ObjectStoreException {
    return _theImple.remove_state(storeUid, tName, StateStatus.OS_COMMITTED);
  }

  @Override
  public boolean remove_uncommitted(Uid storeUid, String tName) throws ObjectStoreException {
    return _theImple.remove_state(storeUid, tName, StateStatus.OS_UNCOMMITTED);
  }

  @Override
  public boolean reveal_state(Uid objUid, String tName) throws ObjectStoreException {
    return _theImple.reveal_state(objUid, tName);
  }

  @Override
  public void start() {}

  @Override
  public void stop() {}

  /**
   * Some object store implementations may be running with automatic sync disabled. Calling this
   * method will ensure that any states are flushed to disk.
   */

  @Override
  public void sync() throws java.io.SyncFailedException, ObjectStoreException {}

  public synchronized void unpackFrom(InputBuffer buff) throws IOException {
    tableName = buff.unpackString();
  }

  @Override
  public boolean write_committed(Uid storeUid, String tName, OutputObjectState state)
      throws ObjectStoreException {
    return _theImple.write_state(storeUid, tName, state, StateStatus.OS_COMMITTED);
  }

  @Override
  public boolean write_uncommitted(Uid storeUid, String tName, OutputObjectState state)
      throws ObjectStoreException {
    return _theImple.write_state(storeUid, tName, state, StateStatus.OS_UNCOMMITTED);
  }

}
