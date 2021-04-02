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
package org.corant.config.declarative;

import java.beans.Transient;
import java.io.Serializable;
import org.eclipse.microprofile.config.Config;

/**
 * corant-config
 *
 * <p>
 * Declarative configuration object, used to construct configuration information into configuration
 * object. Configuration information comes from microprofile config sources. Usually used with
 * {@link ConfigKeyItem} and {@link ConfigKeyItem} and {@link ConfigInstances}.
 *
 * <p>
 * For example:
 *
 * <pre>
 * The declarative configuration object:
 *
 * &#64;ConfigKeyRoot(value = "corant.datasource", keyIndex = 2)
 * public class DatasourceConfig implements DeclarativeConfig {
 *   String url;
 *   Class<?> driver;
 * }
 *
 * The application properties:
 *
 * datasource.url = jdbc:sqlserver://localhost:1433;databaseName=example
 * datasource.driver = com.microsoft.sqlserver.jdbc.SQLServerXADataSource
 *
 * datasource.blog.url = jdbc:mysql://localhost:3306/blog
 * datasource.blog.driver = com.mysql.jdbc.jdbc2.optional.MysqlXADataSource
 *
 * </pre>
 *
 * <p>
 * All configuration item information whose key starts with "datasource" will be mapped to the
 * properties of the declarative configuration object DatasourceConfig, where the name between the
 * property name and "datasource" can be used as the name of the declarative configuration object.
 *
 * The above example DatasourceConfig object has two properties, the names are called "url" and
 * "driver", and the configuration items are named "datasource.blog.url" and
 * "datasource.blog.driver". In this case, the configuration item values of "datasource.blog.url"and
 * "datasource.blog.driver" will be mapped to the DatasourceConfig object named blog.
 *
 * <p>
 * The above example can be mapped to two DataSourceConfig declarative objects, one with a name and
 * one without a name.
 *
 * @author bingo 下午7:39:01
 *
 */
public interface DeclarativeConfig extends Serializable {

  @Transient
  default boolean isValid() {
    return true;
  }

  default void onPostConstruct(Config config, String key) {

  }
}
