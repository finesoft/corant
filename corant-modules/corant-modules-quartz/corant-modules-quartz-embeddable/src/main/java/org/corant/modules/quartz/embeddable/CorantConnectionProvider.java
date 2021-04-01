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
package org.corant.modules.quartz.embeddable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.quartz.utils.PoolingConnectionProvider;

/**
 * corant-modules-quartz-embeddable
 *
 * @author bingo 上午11:12:03
 *
 */
public class CorantConnectionProvider implements PoolingConnectionProvider {

  final Supplier<DataSource> dataSources;

  /**
   * @param dataSources
   */
  public CorantConnectionProvider(Supplier<DataSource> dataSources) {
    this.dataSources = dataSources;
  }

  @Override
  public Connection getConnection() throws SQLException {
    return getDataSource().getConnection();
  }

  @Override
  public DataSource getDataSource() {
    return dataSources.get();
  }

  @Override
  public void initialize() throws SQLException {}

  @Override
  public void shutdown() throws SQLException {}

}
