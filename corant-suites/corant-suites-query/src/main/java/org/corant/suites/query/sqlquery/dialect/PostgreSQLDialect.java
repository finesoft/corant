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
package org.corant.suites.query.sqlquery.dialect;

/**
 * asosat-query
 *
 * @author bingo 上午11:46:20
 *
 */
public class PostgreSQLDialect implements Dialect {

  @Override
  public String getLimitSql(String sql, int offset, int limit) {
    return getLimitString(sql, offset, limit);
  }

  /**
   * <pre>
   * dialect.getLimitString("select * from user", 12, ":offset",0,":limit") will return
   * select * from user limit :offset,:limit
   * </pre>
   */
  public String getLimitString(String sql, int offset, int limit) {
    StringBuilder pageSql = new StringBuilder().append(sql);
    pageSql = offset <= 0 ? pageSql.append(" LIMIT ").append(limit)
        : pageSql.append(" LIMIT ").append(limit).append(" OFFSET ").append(offset);
    return pageSql.toString();
  }

  @Override
  public boolean supportsLimit() {
    return true;
  }
}
