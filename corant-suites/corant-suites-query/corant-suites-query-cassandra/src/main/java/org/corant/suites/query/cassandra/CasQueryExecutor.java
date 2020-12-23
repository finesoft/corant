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
package org.corant.suites.query.cassandra;

import static org.corant.shared.util.Maps.getMapInteger;
import java.util.List;
import java.util.Map;
import org.corant.suites.query.shared.dynamic.SqlHelper;

/**
 * corant-suites-query
 *
 * @author bingo 下午5:20:01
 *
 */
public interface CasQueryExecutor {

  Map<String, Object> get(String keyspace, String cql, Object... args);

  List<Map<String, Object>> paging(String keyspace, String cql, int offset, int limit,
      Object... args);

  List<Map<String, Object>> select(String keyspace, String cql, Object... args);

  default int total(String keyspace, String cql, Object... args) {
    String totalCql = "SELECT COUNT(*) AS total ".concat(SqlHelper.removeSelect(cql));
    return getMapInteger(get(keyspace, totalCql, args), "total", 0);
  }

}
