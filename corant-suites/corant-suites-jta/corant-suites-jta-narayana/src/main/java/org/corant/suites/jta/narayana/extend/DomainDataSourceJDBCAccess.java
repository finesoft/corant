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

import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Maps.getMapInteger;
import static org.corant.shared.util.Strings.isNoneBlank;
import static org.corant.shared.util.Strings.split;
import java.lang.reflect.Method;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import javax.sql.DataSource;
import com.arjuna.ats.arjuna.exceptions.FatalError;
import com.arjuna.ats.arjuna.objectstore.jdbc.JDBCAccess;

/**
 * corant-suites-jta-narayana
 *
 * @author bingo 下午3:30:52
 *
 */
public class DomainDataSourceJDBCAccess implements JDBCAccess {

  protected static final DomainDataSourceJDBCAccess instance = new DomainDataSourceJDBCAccess();
  protected volatile BlockingQueue<XConnection> cachedConnections;
  protected volatile BlockingQueue<XConnection> holdedConnections;
  private volatile int validateConnectionTimeout = 8;
  private volatile String domain;
  private volatile DataSource dataSource;
  private volatile Class<?> driverClass;
  private volatile int maxHoldedSize = -1;

  @Override
  public void finalize() {
    if (cachedConnections != null) {
      for (XConnection connection : cachedConnections) {
        try {
          connection.closeImpl();
        } catch (SQLException e) {
        }
      }
      cachedConnections.clear();
    }
    if (holdedConnections != null) {
      holdedConnections.clear();
    }
  }

  @Override
  public Connection getConnection() throws SQLException {
    XConnection connection;
    if (maxHoldedSize > 0) {
      while ((connection = cachedConnections.poll()) == null) {
        XConnection newConn = null;
        Throwable error = null;
        try {
          holdedConnections.put(newConn = new XConnection(createConnection()));
          cachedConnections.put(newConn);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          error = ie;
        } catch (Exception se) {
          error = se;
        } finally {
          if (error != null) {
            throw new SQLException(error);
          }
        }
      }
    } else {
      connection = new XConnection(createConnection());
    }
    return connection;
  }

  public DataSource getDataSource() {
    return dataSource;
  }

  public String getDomain() {
    return domain;
  }

  public Class<?> getDriverClass() {
    return driverClass;
  }

  @Override
  public void initialise(StringTokenizer tokenizer) {
    // resolve control configs
    Map<String, String> controlConfigs = resolveConfig(tokenizer.nextToken());
    domain = shouldNotBlank(controlConfigs.remove("domain-name"));
    maxHoldedSize = getMapInteger(controlConfigs, "max-connection-size", -1);
    validateConnectionTimeout = getMapInteger(controlConfigs, "validate-connection-timeout", 8);
    if (maxHoldedSize > 0) {
      cachedConnections = new LinkedBlockingQueue<>(maxHoldedSize);
      holdedConnections = new LinkedBlockingQueue<>(maxHoldedSize);
    }
    // resolve data source configs
    Map<String, String> dataSourceConfigs = resolveConfig(tokenizer.nextToken());
    try {
      driverClass = Class.forName(dataSourceConfigs.remove("ClassName"));
      dataSource = (DataSource) driverClass.newInstance();
      Iterator<String> iterator = dataSourceConfigs.keySet().iterator();
      while (iterator.hasNext()) {
        String key = iterator.next();
        String value = dataSourceConfigs.get(key);
        Method method = null;
        try {
          method = dataSource.getClass().getMethod("set" + key, java.lang.String.class);
          method.invoke(dataSource, value.replace("\\semi", ";"));
        } catch (NoSuchMethodException nsme) {
          method = dataSource.getClass().getMethod("set" + key, int.class);
          method.invoke(dataSource, Integer.valueOf(value));
        }
      }
    } catch (Exception ex) {
      dataSource = null;
      throw new FatalError(toString() + " : " + ex, ex);
    }
  }

  Connection createConnection() throws SQLException {
    Connection connection = dataSource.getConnection();
    connection.setAutoCommit(false);
    return connection;
  }

  Map<String, String> resolveConfig(String str) {
    Map<String, String> configuration = new HashMap<>();
    for (String s : split(str, ";", true, true)) {
      int pos = s.indexOf('=');
      if (pos > 0) {
        String key = s.substring(0, pos);
        String val = s.substring(pos + 1);
        if (isNoneBlank(key, val)) {
          configuration.put(key, val);
        }
      }
    }
    return configuration;
  }

  class XConnection implements Connection {
    private final Connection connectionImpl;

    public XConnection(Connection connectionImpl) {
      this.connectionImpl = connectionImpl;
    }

    @Override
    public void abort(Executor executor) throws SQLException {
      connectionImpl.abort(executor);
    }

    @Override
    public void clearWarnings() throws SQLException {
      connectionImpl.clearWarnings();
    }

    @Override
    public void close() throws SQLException {
      if (maxHoldedSize > 0) {
        holdedConnections.remove(this);
        if (isValid(validateConnectionTimeout) && !isClosed()) {
          if (!cachedConnections.offer(this)) {
            closeImpl();// queue is full.
          }
        }
      } else {
        closeImpl();
      }
    }

    public void closeImpl() throws SQLException {
      connectionImpl.close();
    }

    @Override
    public void commit() throws SQLException {
      connectionImpl.commit();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
      return connectionImpl.createArrayOf(typeName, elements);
    }

    @Override
    public Blob createBlob() throws SQLException {
      return connectionImpl.createBlob();
    }

    @Override
    public Clob createClob() throws SQLException {
      return connectionImpl.createClob();
    }

    @Override
    public NClob createNClob() throws SQLException {
      return connectionImpl.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
      return connectionImpl.createSQLXML();
    }

    @Override
    public Statement createStatement() throws SQLException {
      return connectionImpl.createStatement();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency)
        throws SQLException {
      return connectionImpl.createStatement(resultSetType, resultSetConcurrency);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency,
        int resultSetHoldability) throws SQLException {
      return connectionImpl.createStatement(resultSetType, resultSetConcurrency,
          resultSetHoldability);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
      return connectionImpl.createStruct(typeName, attributes);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
      return connectionImpl.getAutoCommit();
    }

    @Override
    public String getCatalog() throws SQLException {
      return connectionImpl.getCatalog();
    }

    @Override
    public Properties getClientInfo() throws SQLException {
      return connectionImpl.getClientInfo();
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
      return connectionImpl.getClientInfo(name);
    }

    @Override
    public int getHoldability() throws SQLException {
      return connectionImpl.getHoldability();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
      return connectionImpl.getMetaData();
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
      return connectionImpl.getNetworkTimeout();
    }

    @Override
    public String getSchema() throws SQLException {
      return connectionImpl.getSchema();
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
      return connectionImpl.getTransactionIsolation();
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
      return connectionImpl.getTypeMap();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
      return connectionImpl.getWarnings();
    }

    @Override
    public boolean isClosed() throws SQLException {
      return connectionImpl.isClosed();
    }

    @Override
    public boolean isReadOnly() throws SQLException {
      return connectionImpl.isReadOnly();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
      return connectionImpl.isValid(timeout);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
      return connectionImpl.isWrapperFor(iface);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
      return connectionImpl.nativeSQL(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
      return connectionImpl.prepareCall(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
        throws SQLException {
      return connectionImpl.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
        int resultSetHoldability) throws SQLException {
      return connectionImpl.prepareCall(sql, resultSetType, resultSetConcurrency,
          resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
      return connectionImpl.prepareStatement(sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
        throws SQLException {
      return connectionImpl.prepareStatement(sql, autoGeneratedKeys);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType,
        int resultSetConcurrency) throws SQLException {
      return connectionImpl.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType,
        int resultSetConcurrency, int resultSetHoldability) throws SQLException {
      return connectionImpl.prepareStatement(sql, resultSetType, resultSetConcurrency,
          resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
      return connectionImpl.prepareStatement(sql, columnIndexes);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames)
        throws SQLException {
      return connectionImpl.prepareStatement(sql, columnNames);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
      connectionImpl.releaseSavepoint(savepoint);
    }

    @Override
    public void rollback() throws SQLException {
      connectionImpl.rollback();
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
      connectionImpl.rollback(savepoint);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
      connectionImpl.setAutoCommit(autoCommit);
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
      connectionImpl.setCatalog(catalog);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
      connectionImpl.setClientInfo(properties);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
      connectionImpl.setClientInfo(name, value);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
      connectionImpl.setHoldability(holdability);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
      connectionImpl.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
      connectionImpl.setReadOnly(readOnly);
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
      return connectionImpl.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
      return connectionImpl.setSavepoint(name);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
      connectionImpl.setSchema(schema);
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
      connectionImpl.setTransactionIsolation(level);
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
      connectionImpl.setTypeMap(map);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
      return connectionImpl.unwrap(iface);
    }
  }
}
