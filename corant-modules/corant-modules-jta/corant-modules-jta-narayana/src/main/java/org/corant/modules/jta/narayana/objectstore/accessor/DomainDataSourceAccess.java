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
package org.corant.modules.jta.narayana.objectstore.accessor;

import static org.corant.context.Instances.resolve;
import static org.corant.shared.util.Assertions.shouldNotBlank;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.StringTokenizer;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.inject.Named;
import javax.sql.DataSource;
import org.corant.modules.jta.narayana.objectstore.driver.AbstractDomainJDBCDriver;
import org.corant.modules.jta.narayana.objectstore.driver.DomainDB2Driver;
import org.corant.modules.jta.narayana.objectstore.driver.DomainMSSqlDriver;
import org.corant.modules.jta.narayana.objectstore.driver.DomainMySqlDriver;
import org.corant.modules.jta.narayana.objectstore.driver.DomainOracleDriver;
import org.corant.modules.jta.narayana.objectstore.driver.DomainPostgreDriver;
import org.corant.shared.exception.NotSupportedException;

/**
 * corant-modules-jta-narayana
 *
 * The configs see below:
 *
 * <pre>
 * In order to enable, put the following two lines of configuration item into the configuration
 * (application.properties/microprofile-config.properties/System.properties):
 *
 * <b>
 * corant.jta.transaction.object-store-environment.jdbcAccess = domain=the-domain-name;database=mysql;non-xa-datasource=the-data-source
 * corant.jta.transaction.object-store-environment.objectStoreType = org.corant.modules.jta.narayana.extend.DomainDataSourceStore
 * </b>

 * param explain:
 *  1. domain=the-domain-name, the name keyword of the subsystem or subdomain,
 *     used to locate the location of the transaction.
 *  2. database=mysql, object storage database service type,
 *     for now supports mysql/mssql/oracle/DB2/postgre.
 *  3. non-xa-datasource=the-data-source, the data source name, may integrate with CDI Bean
 *     (with Named qualifier), <b>be care the data source must non XA</b>.
 * </pre>
 *
 *
 *
 * @author bingo 下午3:30:52
 *
 */
public class DomainDataSourceAccess extends AbstractDomainJDBCAccess {

  public static final DomainDataSourceAccess instance = new DomainDataSourceAccess();
  protected volatile String domain;
  protected volatile Named dataSourceName;
  protected volatile String database;

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
    } else if (database.equalsIgnoreCase("oracle")) {
      return new DomainOracleDriver();
    } else if (database.equalsIgnoreCase("postgre")) {
      return new DomainPostgreDriver();
    } else if (database.equalsIgnoreCase("db2")) {
      return new DomainDB2Driver();
    } else {
      throw new NotSupportedException("Can't support domain jdbc driver for %s.", database);
    }
  }

  @Override
  public void initialise(StringTokenizer tokenizer) {
    // resolve control configs
    Map<String, String> controlConfigs = resolveConfig(tokenizer.nextToken());
    domain = shouldNotBlank(controlConfigs.remove("domain"));
    dataSourceName = NamedLiteral.of(controlConfigs.remove("non-xa-datasource"));
    database = shouldNotBlank(controlConfigs.remove("database"));
  }

}
