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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotBlank;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.naming.NamingException;
import org.corant.shared.exception.CorantRuntimeException;
import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.arjuna.exceptions.ObjectStoreException;
import com.arjuna.ats.arjuna.logging.tsLogger;
import com.arjuna.ats.arjuna.objectstore.StateStatus;
import com.arjuna.ats.arjuna.objectstore.jdbc.JDBCAccess;
import com.arjuna.ats.arjuna.state.InputObjectState;
import com.arjuna.ats.arjuna.state.OutputObjectState;
import com.arjuna.ats.internal.arjuna.common.UidHelper;

/**
 * corant-suites-jta-narayana
 *
 * @author bingo 上午11:00:15
 *
 */
public abstract class AbstractDomainJDBCDriver
    extends com.arjuna.ats.internal.arjuna.objectstore.jdbc.JDBCImple_driver {

  protected JDBCAccess jdbcAccess;
  protected String domain;
  protected volatile boolean ready = false;
  protected volatile boolean createTable;
  protected volatile boolean dropTable;

  /**
   * allObjUids - Given a type name, return an ObjectState that contains all of the uids of objects
   * of that type.
   */
  @Override
  public boolean allObjUids(String typeName, InputObjectState state, int match)
      throws ObjectStoreException {
    // Taken this requirement from ObjStoreBrowser
    if (typeName.startsWith("/")) {
      typeName = typeName.substring(1);
    }
    try {
      OutputObjectState store = new OutputObjectState();
      Connection connection = jdbcAccess.getConnection();
      PreparedStatement pstmt = null;
      ResultSet rs = null;
      try {
        pstmt = connection.prepareStatement(
            "SELECT DISTINCT UidString FROM " + tableName + " WHERE TypeName = ? AND Domain = ?");
        pstmt.setString(1, typeName);
        pstmt.setString(2, domain);
        rs = pstmt.executeQuery();
        boolean finished = false;
        while (!finished && rs.next()) {
          Uid theUid = null;
          try {
            theUid = new Uid(rs.getString(1));
            UidHelper.packInto(theUid, store);
          } catch (IOException ex) {
            tsLogger.i18NLogger.warn_objectstore_JDBCImple_5(ex);
            return false;
          }
        }
        connection.commit();
      } finally {
        release(rs, pstmt, connection);
      }
      UidHelper.packInto(Uid.nullUid(), store);
      state.setBuffer(store.buffer());
      store = null;
      return true;
    } catch (Exception e) {
      tsLogger.i18NLogger.warn_objectstore_JDBCImple_4(e);
      return false;
    }
  }

  @Override
  public boolean allTypes(InputObjectState foundTypes) throws ObjectStoreException {
    try {
      OutputObjectState store = new OutputObjectState();
      Connection connection = jdbcAccess.getConnection();
      PreparedStatement pstmt = null;
      ResultSet rs = null;
      try {
        pstmt = connection
            .prepareStatement("SELECT DISTINCT TypeName FROM " + tableName + " WHERE Domain= ?");
        pstmt.setString(1, domain);
        rs = pstmt.executeQuery();
        boolean finished = false;
        while (!finished && rs.next()) {
          try {
            String type = rs.getString(1);
            store.packString(type);
          } catch (IOException ex) {
            tsLogger.i18NLogger.warn_objectstore_JDBCImple_7(ex);
            return false;
          }
        }
        connection.commit();
      } finally {
        release(rs, pstmt, connection);
      }
      store.packString("");
      foundTypes.setBuffer(store.buffer());
      return true;
    } catch (Exception e) {
      tsLogger.i18NLogger.warn_objectstore_JDBCImple_6(e);
      return false;
    }
  }

  @Override
  public boolean commit_state(Uid objUid, String typeName) throws ObjectStoreException {
    // Taken this requirement from ObjStoreBrowser
    if (typeName.startsWith("/")) {
      typeName = typeName.substring(1);
    }
    boolean result = false;
    Connection connection = null;
    PreparedStatement pstmt = null;
    PreparedStatement pstmt2 = null;
    try {
      connection = jdbcAccess.getConnection();
      // Delete any previously committed state
      pstmt = connection.prepareStatement("DELETE FROM " + tableName
          + " WHERE TypeName = ? AND UidString = ? AND Domain = ? AND StateType = ?");
      pstmt.setString(1, typeName);
      pstmt.setString(2, objUid.stringForm());
      pstmt.setString(3, domain);
      pstmt.setInt(4, StateStatus.OS_COMMITTED);
      int rowcount = pstmt.executeUpdate();
      if (rowcount > 0) {
        tsLogger.i18NLogger.trace_JDBCImple_previouslycommitteddeleted(rowcount);
      }
      // now do the commit itself:
      pstmt2 = connection.prepareStatement("UPDATE " + tableName
          + " SET StateType = ? WHERE TypeName = ? AND UidString = ? AND Domain = ? AND StateType = ?");
      pstmt2.setInt(1, StateStatus.OS_COMMITTED);
      pstmt2.setString(2, typeName);
      pstmt2.setString(3, objUid.stringForm());
      pstmt2.setString(4, domain);
      pstmt2.setInt(5, StateStatus.OS_UNCOMMITTED);
      int rowcount2 = pstmt2.executeUpdate();
      if (rowcount2 > 0) {
        connection.commit();
        result = true;
      } else {
        tsLogger.i18NLogger.warn_objectstore_JDBCImple_nothingtocommit(objUid.stringForm());
        connection.rollback();
      }
    } catch (Exception e) {
      tsLogger.i18NLogger.warn_objectstore_JDBCImple_writefailed(e);
    } finally {
      release(pstmt, pstmt2, connection);
    }
    return result;
  }

  /**
   * currentState - determine the current state of an object. State search is ordered
   * OS_UNCOMMITTED, OS_UNCOMMITTED_HIDDEN, OS_COMMITTED, OS_COMMITTED_HIDDEN
   *
   * @throws ObjectStoreException - in case the JDBC store cannot be contacted
   */
  @Override
  public int currentState(Uid objUid, String typeName) throws ObjectStoreException {
    // Taken this requirement from ObjStoreBrowser
    if (typeName.startsWith("/")) {
      typeName = typeName.substring(1);
    }
    int theState = StateStatus.OS_UNKNOWN;
    ResultSet rs = null;
    Connection connection = null;
    PreparedStatement pstmt = null;
    try {
      connection = jdbcAccess.getConnection();
      pstmt = connection.prepareStatement("SELECT StateType, Hidden FROM " + tableName
          + " WHERE TypeName = ? AND UidString = ? AND Domain = ?");
      pstmt.setString(1, typeName);
      pstmt.setString(2, objUid.stringForm());
      pstmt.setString(3, domain);
      rs = pstmt.executeQuery();

      // we may have multiple states. need to sort out the order of
      // precedence
      // without making multiple round trips out to the db. this gets
      // a bit messy:
      boolean have_OS_UNCOMMITTED = false;
      boolean have_OS_COMMITTED = false;
      boolean have_OS_UNCOMMITTED_HIDDEN = false;
      boolean have_OS_COMMITTED_HIDDEN = false;

      while (rs.next()) {
        int stateStatus = rs.getInt(1);
        int hidden = rs.getInt(2);
        switch (stateStatus) {
          case StateStatus.OS_UNCOMMITTED:
            if (hidden == 0) {
              have_OS_UNCOMMITTED = true;
            } else {
              have_OS_UNCOMMITTED_HIDDEN = true;
            }
            break;
          case StateStatus.OS_COMMITTED:
            if (hidden == 0) {
              have_OS_COMMITTED = true;
            } else {
              have_OS_COMMITTED_HIDDEN = true;
            }
            break;
        }
      }
      connection.commit();
      // examine in reverse order:
      if (have_OS_COMMITTED_HIDDEN) {
        theState = StateStatus.OS_COMMITTED_HIDDEN;
      }
      if (have_OS_COMMITTED) {
        theState = StateStatus.OS_COMMITTED;
      }
      if (have_OS_UNCOMMITTED_HIDDEN) {
        theState = StateStatus.OS_UNCOMMITTED_HIDDEN;
      }
      if (have_OS_UNCOMMITTED) {
        theState = StateStatus.OS_UNCOMMITTED;
      }
    } catch (Exception e) {
      tsLogger.i18NLogger.warn_objectstore_JDBCImple_3(e);
      throw new ObjectStoreException(e);
    } finally {
      release(rs, pstmt, connection);
    }
    return theState;
  }

  @Override
  public int getMaxStateSize() {
    return 65535;
  }

  @Override
  public boolean hide_state(Uid objUid, String typeName) throws ObjectStoreException {
    // Taken this requirement from ObjStoreBrowser
    if (typeName.startsWith("/")) {
      typeName = typeName.substring(1);
    }
    boolean result = false;
    Connection connection = null;
    PreparedStatement pstmt = null;
    try {
      connection = jdbcAccess.getConnection();
      pstmt = connection.prepareStatement("UPDATE " + tableName
          + " SET Hidden = 1 WHERE TypeName = ? AND UidString = ? AND Domain = ?");
      pstmt.setString(1, typeName);
      pstmt.setString(2, objUid.stringForm());
      pstmt.setString(3, domain);
      int rowcount = pstmt.executeUpdate();
      connection.commit();
      if (rowcount > 0) {
        result = true;
      }
    } catch (Exception e) {
      tsLogger.i18NLogger.warn_objectstore_JDBCImple_1(e);
    } finally {
      release(pstmt, connection);
    }
    return result;
  }

  /**
   * Set up the store for use.
   *
   * @throws NamingException
   * @throws SQLException In case the configured store cannot be connected to
   */
  @Override
  public void initialise(final JDBCAccess jdbcAccess, String tableName,
      ObjectStoreEnvironmentBean jdbcStoreEnvironmentBean) throws SQLException, NamingException {
    shouldBeTrue(jdbcAccess instanceof DomainDataSourceJDBCAccess);
    this.jdbcAccess = jdbcAccess;
    this.tableName = tableName;
    domain = shouldNotBlank(((DomainDataSourceJDBCAccess) jdbcAccess).getDomain());
    dropTable = jdbcStoreEnvironmentBean.getDropTable();
    createTable = jdbcStoreEnvironmentBean.getCreateTable();
    prepare();
  }

  @Override
  public InputObjectState read_state(Uid objUid, String typeName, int stateType)
      throws ObjectStoreException {
    InputObjectState result = null;
    // Taken this requirement from ObjStoreBrowser
    if (typeName.startsWith("/")) {
      typeName = typeName.substring(1);
    }

    if (stateType == StateStatus.OS_COMMITTED || stateType == StateStatus.OS_UNCOMMITTED) {
      ResultSet rs = null;
      Connection connection = null;
      PreparedStatement pstmt = null;
      try {
        connection = jdbcAccess.getConnection();
        pstmt = connection.prepareStatement("SELECT ObjectState FROM " + tableName
            + " WHERE TypeName = ? AND UidString = ? AND StateType = ?  AND Domain = ?");
        pstmt.setString(1, typeName);
        pstmt.setString(2, objUid.stringForm());
        pstmt.setInt(3, stateType);
        pstmt.setString(4, domain);
        rs = pstmt.executeQuery();
        if (rs.next()) {
          byte[] buffer = rs.getBytes(1);
          if (buffer != null) {
            result = new InputObjectState(objUid, typeName, buffer);
          } else {
            tsLogger.i18NLogger.warn_objectstore_JDBCImple_readfailed();
            throw new ObjectStoreException(
                tsLogger.i18NLogger.warn_objectstore_JDBCImple_readfailed_message());
          }
        }
        connection.commit();
      } catch (Exception e) {
        tsLogger.i18NLogger.warn_objectstore_JDBCImple_14(e);
        throw new ObjectStoreException(e);
      } finally {
        release(rs, pstmt, connection);
      }
    } else {
      throw new ObjectStoreException(tsLogger.i18NLogger.unexpected_state_type(stateType));
    }
    return result;
  }

  @Override
  public boolean remove_state(Uid objUid, String typeName, int stateType)
      throws ObjectStoreException {
    // Taken this requirement from ObjStoreBrowser
    if (typeName.startsWith("/")) {
      typeName = typeName.substring(1);
    }
    boolean result = false;
    if (typeName != null) {
      if (stateType == StateStatus.OS_COMMITTED || stateType == StateStatus.OS_UNCOMMITTED) {
        Connection connection = null;
        PreparedStatement pstmt = null;
        try {
          connection = jdbcAccess.getConnection();
          pstmt = connection.prepareStatement("DELETE FROM " + tableName
              + " WHERE TypeName = ? AND UidString = ? AND StateType = ? AND Domain = ?");
          pstmt.setString(1, typeName);
          pstmt.setString(2, objUid.stringForm());
          pstmt.setInt(3, stateType);
          pstmt.setString(4, domain);
          if (pstmt.executeUpdate() > 0) {
            result = true;
          }
          connection.commit();
        } catch (Exception e) {
          result = false;
          tsLogger.i18NLogger.warn_objectstore_JDBCImple_8(e);
        } finally {
          release(pstmt, connection);
        }
      } else {
        // can only remove (UN)COMMITTED objs
        tsLogger.i18NLogger.warn_objectstore_JDBCImple_9(Integer.toString(stateType), objUid);
      }
    } else {
      tsLogger.i18NLogger.warn_objectstore_JDBCImple_10(objUid);
    }

    return result;
  }

  @Override
  public boolean reveal_state(Uid objUid, String typeName) throws ObjectStoreException {
    // Taken this requirement from ObjStoreBrowser
    if (typeName.startsWith("/")) {
      typeName = typeName.substring(1);
    }
    boolean result = false;
    Connection connection = null;
    PreparedStatement pstmt = null;
    try {
      connection = jdbcAccess.getConnection();
      pstmt = connection.prepareStatement("UPDATE " + tableName
          + " SET Hidden = 0 WHERE TypeName = ? AND UidString = ?  AND Domain = ?");
      pstmt.setString(1, typeName);
      pstmt.setString(2, objUid.stringForm());
      pstmt.setString(3, domain);
      int rowcount = pstmt.executeUpdate();
      connection.commit();
      if (rowcount > 0) {
        result = true;
      }
    } catch (Exception e) {
      tsLogger.i18NLogger.warn_objectstore_JDBCImple_2(e);
    } finally {
      release(pstmt, connection);
    }

    return result;
  }

  @Override
  public boolean write_state(Uid objUid, String typeName, OutputObjectState state, int stateType)
      throws ObjectStoreException {
    // Taken this requirement from ObjStoreBrowser
    if (typeName.startsWith("/")) {
      typeName = typeName.substring(1);
    }
    boolean result = false;
    int imageSize = state.length();
    if (imageSize > getMaxStateSize()) {
      tsLogger.i18NLogger.warn_objectstore_JDBCImple_over_max_image_size(imageSize,
          getMaxStateSize());
    } else if (imageSize > 0) {
      byte[] b = state.buffer();
      ResultSet rs = null;
      Connection connection = null;
      PreparedStatement pstmt = null;
      try {
        connection = jdbcAccess.getConnection();
        pstmt = connection.prepareStatement(
            "SELECT ObjectState, UidString, StateType, TypeName FROM " + tableName
                + " WHERE TypeName = ? AND UidString = ? AND StateType = ? AND Domain = ?",
            ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
        pstmt.setString(1, typeName);
        pstmt.setString(2, objUid.stringForm());
        pstmt.setInt(3, stateType);
        pstmt.setString(4, domain);
        rs = pstmt.executeQuery();
        if (rs.next()) {
          PreparedStatement pstmt2 =
              connection.prepareStatement("UPDATE " + tableName + " SET ObjectState = ?"
                  + " WHERE TypeName=? AND UidString=? AND StateType=? AND Domain = ?");
          try {
            pstmt2.setBytes(1, b);
            pstmt2.setString(2, typeName);
            pstmt2.setString(3, objUid.stringForm());
            pstmt2.setInt(4, stateType);
            pstmt2.setString(5, domain);
            int executeUpdate = pstmt2.executeUpdate();
            if (executeUpdate != 0) {
              result = true;
            } else {
              tsLogger.i18NLogger.warn_objectstore_JDBCImple_nothingtoupdate(objUid.toString());
            }
          } finally {
            pstmt2.close();
          }
        } else {
          // not in database, do insert:
          PreparedStatement pstmt2 = connection.prepareStatement("INSERT INTO " + tableName
              + " (TypeName,UidString,StateType,Hidden,ObjectState,Domain) VALUES (?,?,?,0,?,?)");
          try {
            pstmt2.setString(1, typeName);
            pstmt2.setString(2, objUid.stringForm());
            pstmt2.setInt(3, stateType);
            pstmt2.setBytes(4, b);
            pstmt2.setString(5, domain);
            int executeUpdate = pstmt2.executeUpdate();
            if (executeUpdate != 0) {
              result = true;
            } else {
              tsLogger.i18NLogger.warn_objectstore_JDBCImple_nothingtoinsert(objUid.toString());
            }
          } finally {
            pstmt2.close();
          }
        }
        connection.commit();
      } catch (Exception e) {
        tsLogger.i18NLogger.warn_objectstore_JDBCImple_writefailed(e);
      } finally {
        release(rs, pstmt, connection);
      }
    }
    return result;
  }

  @Override
  protected void createTable(Statement stmt, String tableName) throws SQLException {
    String statement = "CREATE TABLE " + tableName
        + " (Domain VARCHAR(128) NOT NULL, StateType INTEGER NOT NULL, Hidden INTEGER NOT NULL, "
        + "TypeName VARCHAR(255) NOT NULL, UidString VARCHAR(255) NOT NULL, ObjectState "
        + getObjectStateSQLType() + ", PRIMARY KEY(Domain,UidString, TypeName, StateType))";
    stmt.executeUpdate(statement);
  }

  protected void prepare() {
    if (!ready) {
      synchronized (this) {
        if (!ready) {
          try (Connection connection = jdbcAccess.getConnection()) {
            try (Statement stmt = connection.createStatement()) {
              // table [type, object UID, format, blob]
              if (dropTable) {
                try {
                  stmt.executeUpdate("DROP TABLE " + tableName);
                } catch (SQLException ex) {
                  checkDropTableException(connection, ex);
                }
              }
              if (createTable) {
                try {
                  createTable(stmt, tableName);
                } catch (SQLException ex) {
                  checkCreateTableError(ex);
                }
              }
              // This can be the case when triggering via EmptyObjectStore
              if (!connection.getAutoCommit()) {
                connection.commit();
              }
            } catch (SQLException e) {
              throw new CorantRuntimeException(e);
            }
          } catch (SQLException e1) {
            throw new CorantRuntimeException(e1);
          }
          ready = true;
        }
      }
    }
  }

  protected void release(AutoCloseable... closeables) {
    for (AutoCloseable closeable : closeables) {
      if (closeable != null) {
        try {
          closeable.close();
        } catch (Exception e) {
          // Noop!
        }
      }
    }
  }
}
