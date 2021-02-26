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
package org.corant.suites.jta.narayana.objectstore.driver;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * corant-suites-jta-narayana
 *
 * @author bingo 上午11:00:15
 *
 */
public class DomainPostgreDriver extends AbstractDomainJDBCDriver {

  @Override
  public int getMaxStateSize() {
    // Oracle BLOBs should be OK up to > 4 GB, but cap @ 10 MB for
    // testing/performance:
    return 1024 * 1024 * 10;
  }

  @Override
  protected void checkCreateTableError(SQLException ex) throws SQLException {
    if (!ex.getSQLState().equals("42P07")) {
      throw ex;
    }
  }

  @Override
  protected void checkDropTableException(Connection connection, SQLException ex)
      throws SQLException {
    if (!ex.getSQLState().equals("42P01")) {
      throw ex;
    } else {
      // For some reason PSQL leaves the transaction in a bad state on a
      // failed drop
      connection.commit();
    }
  }

  @Override
  protected String getObjectStateSQLType() {
    return "BLOB";
  }
}
