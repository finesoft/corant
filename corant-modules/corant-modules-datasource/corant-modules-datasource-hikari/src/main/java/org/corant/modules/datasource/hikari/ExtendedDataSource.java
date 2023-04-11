/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.datasource.hikari;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.Executor;
import java.util.logging.Logger;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import org.corant.modules.datasource.shared.WrappedConnection;
import org.corant.modules.jta.shared.TransactionAwareness;
import org.corant.modules.jta.shared.TransactionAwareness.LocalXATransactionAwareness;
import org.corant.modules.jta.shared.TransactionIntegrator;
import org.corant.modules.jta.shared.TransactionIntegrator.LocalXATransactionIntegrator;
import org.corant.shared.ubiquity.Experimental;
import com.zaxxer.hikari.HikariDataSource;

/**
 * corant-modules-datasource-hikari
 *
 * @author bingo 下午2:07:19
 *
 */
@Experimental
public class ExtendedDataSource implements DataSource, Closeable {

  protected final HikariDataSource delegate;

  protected final TransactionIntegrator transactionIntegrator;

  public ExtendedDataSource(HikariDataSource delegate, TransactionManager transactionManager,
      TransactionSynchronizationRegistry transactionSynchronizationRegistry) {
    this.delegate = delegate;
    transactionIntegrator =
        new LocalXATransactionIntegrator(transactionManager, transactionSynchronizationRegistry);
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }

  @Override
  public Connection getConnection() throws SQLException {
    TransactionAwareness awareness;
    if ((awareness = transactionIntegrator.getTransactionAwareness()) != null) {
      return (Connection) awareness.getConnection();
    }
    Connection connection = wrapConnection(delegate.getConnection());
    awareness = new LocalXATransactionAwareness(connection, true);
    transactionIntegrator.associate(awareness, null);
    return (Connection) awareness.getConnection();
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    Connection connection = wrapConnection(delegate.getConnection(username, password));
    TransactionAwareness awareness = new LocalXATransactionAwareness(connection, true);
    transactionIntegrator.associate(awareness, null);
    return (Connection) awareness.getConnection();
  }

  public HikariDataSource getDelegate() {
    return delegate;
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return delegate.getLoginTimeout();
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return delegate.getLogWriter();
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    return delegate.getParentLogger();
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return delegate.isWrapperFor(iface);
  }

  @Override
  public void setLoginTimeout(int seconds) throws SQLException {
    delegate.setLoginTimeout(seconds);
  }

  @Override
  public void setLogWriter(PrintWriter out) throws SQLException {
    delegate.setLogWriter(out);
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    return delegate.unwrap(iface);
  }

  protected Connection wrapConnection(Connection connection) {
    return new UsedConnection(connection, transactionIntegrator);
  }

  /**
   * corant-modules-datasource-hikari
   *
   * @author bingo 下午5:17:57
   *
   */
  public static final class UsedConnection extends WrappedConnection {

    protected TransactionIntegrator transactionIntegrator;

    public UsedConnection(Connection connection, TransactionIntegrator transactionIntegrator) {
      super(connection);
      this.transactionIntegrator = transactionIntegrator;
    }

    @Override
    public void abort(Executor executor) throws SQLException {
      transactionIntegrator.disassociate(null);
      super.abort(executor);
    }

    @Override
    public void close() throws SQLException {
      if (transactionIntegrator.getTransactionAwareness() != null) {
        return;
      }
      transactionIntegrator.disassociate(null);
      super.close();
    }
  }
}
