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
package org.corant.suites.query.sql;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.corant.suites.query.sql.dialect.Dialect;

/**
 * corant-suites-query
 *
 * @author bingo 下午5:20:01
 *
 */
public interface SqlQueryExecutor {

  Map<String, Object> get(String sql, Object... args) throws SQLException;

  Dialect getDialect();

  List<Map<String, Object>> select(String sql, int expectRows, Object... args) throws SQLException;

  default List<Map<String, Object>> select(String sql, Object... args) throws SQLException {
    return select(sql, 0, args);
  }

  Stream<Map<String, Object>> stream(String sql, Object... args);

}
