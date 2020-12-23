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

import static org.corant.context.Instances.resolve;
import static org.corant.shared.util.Assertions.shouldNotBlank;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.StringTokenizer;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.inject.Named;
import javax.sql.DataSource;
import org.corant.shared.exception.NotSupportedException;

/**
 * corant-suites-jta-narayana
 *
 * @author bingo 下午3:30:52
 *
 */
public class DomainDataSourceAccess extends AbstractDomainJDBCAccess {

  protected static final DomainDataSourceAccess instance = new DomainDataSourceAccess();
  protected volatile String domain;
  protected volatile Named dataSourceName;
  protected volatile String database;

  @Override
  public void finalize() {
    // Noop! we are pooled
  }

  @Override
  public Connection getConnection() throws SQLException {
    Connection conn = resolve(DataSource.class, dataSourceName).getConnection();
    conn.setAutoCommit(false);
    return conn;
  }

  @Override
  public String getDomain() {
    return domain;
  }

  @Override
  public AbstractDomainJDBCDriver getDriver() {
    if (database.equalsIgnoreCase("mssql")) {
      return new DomainMSSqlDriver();
    } else if (database.equalsIgnoreCase("mysql")) {
      return new DomainMySqlDriver();
    } else {
      throw new NotSupportedException("Can't support domain jdbc driver for %s", database);
    }
  }

  @Override
  public void initialise(StringTokenizer tokenizer) {
    // resolve control configs
    Map<String, String> controlConfigs = resolveConfig(tokenizer.nextToken());
    domain = shouldNotBlank(controlConfigs.remove("domain-name"));
    dataSourceName = NamedLiteral.of(controlConfigs.remove("datasource-name"));
    database = shouldNotBlank(controlConfigs.remove("database"));
  }

}
