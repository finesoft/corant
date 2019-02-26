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

import java.util.Locale;

/**
 * corant-suites-query
 *
 * @author bingo 上午11:45:24
 *
 */
public class OracleDialect implements Dialect {


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
    String sqlToUse = sql.trim();
    boolean isForUpdate = false;
    if (sqlToUse.toUpperCase(Locale.getDefault()).endsWith(" FOR UPDATE")) {
      sqlToUse = sqlToUse.substring(0, sqlToUse.length() - 11);
      isForUpdate = true;
    }
    StringBuilder pagingSelect = new StringBuilder(sqlToUse.length() + 100);
    if (offset >= 0) {
      pagingSelect.append("SELECT * FROM ( SELECT row_.*, ROWNUM rownum_ FROM ( ");
    } else {
      pagingSelect.append("SELECT * FROM ( ");
    }
    pagingSelect.append(sqlToUse);

    if (offset >= 0) {
      pagingSelect.append(" ) row_ ) WHERE rownum_ < ").append(offset + limit)
          .append(" AND rownum_ >= ").append(offset);
    } else {
      pagingSelect.append(" ) WHERE ROWNUM < ").append(limit);
    }

    if (isForUpdate) {
      pagingSelect.append(" FOR UPDATE");
    }
    return pagingSelect.toString();
  }

  @Override
  public boolean supportsLimit() {
    return true;
  }

}
