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
package org.corant.modules.jta.shared;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;

/**
 * corant-modules-jta-shared
 *
 * @author bingo 下午4:48:08
 *
 */
public interface TransactionAwareness {

  Object getConnection();

  void transactionBeforeCompletion(boolean successful);

  void transactionChecker(Callable<Boolean> transactionChecker);

  void transactionCommit() throws Exception;

  void transactionEnd() throws Exception;

  void transactionRollback() throws Exception;

  void transactionStart() throws Exception;

  /**
   * corant-modules-jta-shared
   *
   * @author bingo 下午4:12:05
   *
   */
  class LocalXATransactionAwareness implements TransactionAwareness {

    protected final Connection connection;

    protected final boolean closeConnectionOnEnd;

    protected volatile boolean enlisted;

    protected volatile boolean needResetAutoCommit;

    protected Callable<Boolean> transactionChecker;

    public LocalXATransactionAwareness(Connection connection, boolean closeConnectionOnEnd) {
      this.connection = connection;
      this.closeConnectionOnEnd = closeConnectionOnEnd;
    }

    @Override
    public Connection getConnection() {
      return connection;
    }

    @Override
    public void transactionBeforeCompletion(boolean successful) {}

    @Override
    public void transactionChecker(Callable<Boolean> transactionChecker) {
      this.transactionChecker = transactionChecker;
    }

    @Override
    public void transactionCommit() throws Exception {
      shouldBeTrue(enlisted);
      try {
        connection.commit();
      } catch (SQLException se) {
        throw se;
      }
    }

    @Override
    public void transactionEnd() throws Exception {
      enlisted = false;
      try {
        if (needResetAutoCommit) {
          connection.setAutoCommit(true);
        }
      } finally {

      }
      try {
        if (closeConnectionOnEnd) {
          connection.close();
        }
      } catch (Exception e) {
        throw e;
      }
    }

    @Override
    public void transactionRollback() throws Exception {
      shouldBeTrue(enlisted);
      try {
        connection.rollback();
      } catch (SQLException se) {
        throw se;
      }
    }

    @Override
    public void transactionStart() throws Exception {
      try {
        if (!enlisted && connection.getAutoCommit()) {
          connection.setAutoCommit(false);
          needResetAutoCommit = true;
        }
        enlisted = true;
      } catch (SQLException se) {
        throw se;
      }
    }

  }
}
