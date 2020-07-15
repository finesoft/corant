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

import java.sql.Connection;
import java.sql.SQLException;

/**
 * corant-suites-jta-narayana
 *
 * @author bingo 上午11:00:15
 *
 */
public class DomainMySqlDriver extends AbstractDomainJDBCDriver {

  @Override
  protected void checkCreateTableError(SQLException ex) throws SQLException {
    if (!ex.getSQLState().equals("42S01")) {
      throw ex;
    }
  }

  @Override
  protected void checkDropTableException(Connection connection, SQLException ex)
      throws SQLException {
    if (!ex.getSQLState().equals("42S02")) {
      throw ex;
    }
  }

  @Override
  protected String getObjectStateSQLType() {
    return "BLOB";
  }
}
