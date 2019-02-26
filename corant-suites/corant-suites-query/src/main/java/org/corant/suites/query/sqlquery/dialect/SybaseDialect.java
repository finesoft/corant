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
 * corant-suites-query
 *
 * @author bingo 上午11:47:15
 *
 */
public class SybaseDialect implements Dialect {

  @Override
  public String getCountSql(String sql) {
    return new StringBuilder(sql.length() + 64).append("select count(1) from ( ")
        .append(Dialect.getNonOrderByPart(sql)).append(" ) as tmp_count").toString();
  }

  @Override
  public String getLimitSql(String sql, int offset, int limit) {
    throw new UnsupportedOperationException("The database Sybase limit script not supported");
  }

  public String getLimitString(String sql, int offset, String offsetPlaceholder, int limit,
      String limitPlaceholder) {
    throw new UnsupportedOperationException("The database Sybase limit script not supported");
  }

  @Override
  public boolean supportsLimit() {
    return false;
  }
}
