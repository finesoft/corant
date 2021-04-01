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
package org.corant.modules.datasource.shared;

import java.util.Properties;
import javax.sql.DataSource;

/**
 * corant-modules-datasource-shared
 *
 * @author bingo 下午2:43:11
 *
 */
public interface DataSourceService {

  default DataSource get(String jdbcUrl) {
    return new DriverManagerDataSource(jdbcUrl);
  }

  default DataSource get(String jdbcUrl, Properties properties) {
    return new DriverManagerDataSource(jdbcUrl, properties);
  }

  default DataSource get(String jdbcUrl, String driverClassName, Properties properties,
      String username, String password) {
    return new DriverManagerDataSource(jdbcUrl, driverClassName, properties, username, password);
  }

  default DataSource get(String jdbcUrl, String driverClassName, Properties properties,
      String username, String password, String catalog, String schema) {
    return new DriverManagerDataSource(jdbcUrl, driverClassName, properties, username, password,
        catalog, schema);
  }

  default DataSource get(String jdbcUrl, String username, String password) {
    return new DriverManagerDataSource(jdbcUrl, username, password);
  }

  default DataSource get(String jdbcUrl, String driverClassName, String username, String password) {
    return new DriverManagerDataSource(jdbcUrl, driverClassName, username, password);
  }

  DataSource getManaged(String name);
}
