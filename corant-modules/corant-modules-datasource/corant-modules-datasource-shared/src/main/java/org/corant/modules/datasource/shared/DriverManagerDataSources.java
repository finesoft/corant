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
package org.corant.modules.datasource.shared;

import java.util.Properties;
import javax.sql.DataSource;

/**
 * corant-modules-datasource-shared
 *
 * @author bingo 下午2:07:45
 *
 */
public class DriverManagerDataSources {

  /**
   * Returns a data source for given jdbc-url.
   *
   * @param jdbcUrl the jdbc-url use to build data source
   */
  public static DataSource get(String jdbcUrl) {
    return new DriverManagerDataSource(jdbcUrl);
  }

  /**
   * Returns a data source for given jdbc-url and properties.
   *
   * @param jdbcUrl the jdbc-url use to build data source
   * @param properties a list of arbitrary string tag/value pairs as connection arguments. Normally
   *        at least a "user" and "password" property should be included.
   */
  public static DataSource get(String jdbcUrl, Properties properties) {
    return new DriverManagerDataSource(jdbcUrl, properties);
  }

  /**
   * Returns a data source for given jdbc-url and driver class name and properties.
   *
   * @param jdbcUrl the jdbc-url use to build data source
   * @param driverClassName the driver class name use to java.sql.Driver
   * @param properties a list of arbitrary string tag/value pairs as connection arguments. Normally
   *        at least a "user" and "password" property should be included.
   */
  public static DataSource get(String jdbcUrl, String driverClassName, Properties properties) {
    return new DriverManagerDataSource(jdbcUrl, driverClassName, properties, null, null);
  }

  /**
   * Returns a data source for given jdbc-url and driver class name and properties.
   *
   * @param jdbcUrl the jdbc-url use to build data source
   * @param driverClassName the driver class name use to java.sql.Driver
   * @param properties a list of arbitrary string tag/value pairs as connection arguments. Normally
   *        at least a "user" and "password" property should be included.
   * @param catalog the data base catalog use to get connection
   * @param schema the data base schema use to get connection
   */
  public static DataSource get(String jdbcUrl, String driverClassName, Properties properties,
      String catalog, String schema) {
    return new DriverManagerDataSource(jdbcUrl, driverClassName, properties, catalog, schema);
  }

  /**
   * Returns a data source for given jdbc-url and user name and password.
   *
   * @param jdbcUrl the jdbc-url use to build data source
   * @param username the user name use to access the data base
   * @param password the password use to access the data base
   */
  public static DataSource get(String jdbcUrl, String username, String password) {
    return new DriverManagerDataSource(jdbcUrl, username, password);
  }

  /**
   * Returns a data source for given jdbc-url and driver class name and user name and password.
   *
   * @param jdbcUrl the jdbc-url use to build data source
   * @param driverClassName the driver class name use to java.sql.Driver
   * @param username the user name use to access the data base
   * @param password the password use to access the data base
   */
  public static DataSource get(String jdbcUrl, String driverClassName, String username,
      String password) {
    return new DriverManagerDataSource(jdbcUrl, driverClassName, username, password);
  }
}
